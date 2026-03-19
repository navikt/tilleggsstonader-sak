package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBilProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class UtbetalingDagligReisePrivatBilIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var avklartKjørtUkeRepository: AvklartKjørtUkeRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var satsDagligReisePrivatBilProvider: SatsDagligReisePrivatBilProvider

    @Test
    fun `innvilger rammevedtak og sender inn kjøreliste som blir godkjent, uke blir sendt til utbetaling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 22 februar 2026
        val reiseavstandEnVei = BigDecimal(10)
        val kjørteDager =
            listOf(
                2 februar 2026 to 50,
                4 februar 2026 to 50,
                5 februar 2026 to 50,
            )

        val førstegangsBehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }

                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom, tom)
                    }
                }

                vilkår {
                    opprett {
                        privatBil(fom, tom, reisedagerPerUke = 3, reiseavstandEnVei = reiseavstandEnVei)
                    }
                }

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    this.kjørteDager = kjørteDager
                }
            }

        val førstegangsBehandling = testoppsettService.hentBehandling(førstegangsBehandlingId)
        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandling(kjørelisteBehandling)

        val forventetBeløp = kjørteDager.kalkulerForventetBeløp(reiseavstandEnVei)
        assertAndelOpprettet(
            andelerForKjørelistebehandling = tilkjentYtelseRepository.findByBehandlingId(kjørelisteBehandling.id)?.andelerTilkjentYtelse,
            forventetBeløp = forventetBeløp,
            forventetTypeAndel = TypeAndel.DAGLIG_REISE_AAP,
        )

        val iverksettingDto =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        assertThat(iverksettingDto.saksbehandler).isEqualTo(testBrukerkontekst.bruker)
        assertThat(iverksettingDto.beslutter).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(iverksettingDto.utbetalinger).hasSize(1)
        val utbetaling = iverksettingDto.utbetalinger.single()
        assertThat(utbetaling.stønad).isEqualTo(StønadUtbetaling.DAGLIG_REISE_AAP)
        assertThat(utbetaling.perioder).hasSize(1)
        val periode = utbetaling.perioder.single()
        // Fom og tom samme verdi, gjelder for en uke
        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(fom)
        assertThat(periode.beløp).isEqualTo(forventetBeløp.toUInt())

        val ferdigstiltKjørelistebehandling = testoppsettService.hentBehandling(kjørelisteBehandling.id)
        assertThat(ferdigstiltKjørelistebehandling.resultat).isEqualTo(BehandlingResultat.INNVILGET)
        assertThat(ferdigstiltKjørelistebehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(ferdigstiltKjørelistebehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val oppgaverPåKjørelisteBehandling = oppgaveRepository.findByBehandlingId(kjørelisteBehandling.id)
        assertThat(oppgaverPåKjørelisteBehandling).hasSize(1)
        assertThat(oppgaverPåKjørelisteBehandling.single().status).isEqualTo(Oppgavestatus.FERDIGSTILT)

        val gjeldendeIverksatteBehandlinger =
            testoppsettService.hentGjeldendeIverksatteBehandlinger(Stønadstype.DAGLIG_REISE_TSO)
        assertThat(gjeldendeIverksatteBehandlinger.map { it.id })
            .contains(kjørelisteBehandling.id)
            .doesNotContain(førstegangsBehandling.id)
    }

    private fun assertAndelOpprettet(
        andelerForKjørelistebehandling: Set<AndelTilkjentYtelse>?,
        forventetBeløp: Int,
        forventetTypeAndel: TypeAndel,
    ) {
        assertThat(andelerForKjørelistebehandling).isNotNull
        assertThat(andelerForKjørelistebehandling).hasSize(1)
        val andel = andelerForKjørelistebehandling!!.single()
        assertThat(andel.type).isEqualTo(forventetTypeAndel)
        assertThat(andel.beløp).isEqualTo(forventetBeløp)
    }

    private fun List<Pair<LocalDate, Int>>.kalkulerForventetBeløp(reiseavstandEnVei: BigDecimal): Int =
        sumOf { (dato, parkeringskostnader) ->
            satsDagligReisePrivatBilProvider
                .finnSatsForÅr(dato.year)
                .beløp
                .multiply(reiseavstandEnVei)
                .multiply(2.toBigDecimal())
                .plus(parkeringskostnader.toBigDecimal())
        }.toInt()
}
