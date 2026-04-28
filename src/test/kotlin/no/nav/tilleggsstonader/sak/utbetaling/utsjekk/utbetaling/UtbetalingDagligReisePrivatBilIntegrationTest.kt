package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandlingManuelt
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.avrundetStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBilProvider
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class UtbetalingDagligReisePrivatBilIntegrationTest : IntegrationTest() {
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
        val reiseavstandEnVei = BigDecimal(7.9)
        val kjørteDager =
            listOf(
                2 februar 2026 to 50,
                4 februar 2026 to 50,
                5 februar 2026 to 50,
            )
        val delperioder =
            listOf(
                FaktaDelperiodePrivatBilDto(
                    fom = fom,
                    tom = tom,
                    reisedagerPerUke = 5,
                    bompengerPerDag = null,
                    fergekostnadPerDag = null,
                ),
            )
        val førstegangsBehandlingContext =
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
                        privatBil(
                            fom,
                            tom,
                            reiseavstandEnVei = reiseavstandEnVei,
                            delperioder = delperioder,
                        )
                    }
                }

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    this.kjørteDager = kjørteDager
                }
            }

        val førstegangsBehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)
        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandlingManuelt(kjørelisteBehandling)

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

        val oppgaverPåKjørelisteBehandling = oppgaveRepository.findByBehandlingId(kjørelisteBehandling.id)
        if (oppgaverPåKjørelisteBehandling.isEmpty()) {
            // Automatisk kjørelistebehandling kjøres uten oppgave og blir dermed systembehandlet.
            assertThat(iverksettingDto.saksbehandler).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        } else {
            assertThat(iverksettingDto.saksbehandler).isEqualTo(testBrukerkontekst.bruker)
            assertThat(oppgaverPåKjørelisteBehandling).hasSize(1)
            assertThat(oppgaverPåKjørelisteBehandling.single().status).isEqualTo(Oppgavestatus.FERDIGSTILT)
        }
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

        val gjeldendeIverksatteBehandlinger =
            testoppsettService.hentGjeldendeIverksatteBehandlinger(Stønadstype.DAGLIG_REISE_TSO)
        assertThat(gjeldendeIverksatteBehandlinger.map { it.id })
            .contains(kjørelisteBehandling.id)
            .doesNotContain(førstegangsBehandling.id)

        val andelsBeløp = tilkjentYtelseRepository.findByBehandlingId(kjørelisteBehandling.id)!!.andelerTilkjentYtelse.sumOf { it.beløp }
        assertThat(andelsBeløp).isEqualTo(forventetBeløp)
    }

    @Test
    fun `sak med to overlappende reiser, begge utbetales med samme typeAndel, grupperes på reise til økonomi`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 mars 2026
        val tom = 15 mars 2026

        val førstegangsBehandlingContext =
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
                        privatBil(fom, tom, reiseavstandEnVei = 10.toBigDecimal())
                        privatBil(fom, tom, reiseavstandEnVei = 100.toBigDecimal())
                    }
                }
            }

        val alleRammevedtak = kall.privatBil.hentRammevedtak(førstegangsBehandlingContext.ident)
        assertThat(alleRammevedtak).hasSize(2)
        val rammevedtak1 = alleRammevedtak[0]
        val rammevedtak2 = alleRammevedtak[1]

        // Sender inn kjøreliste for første rammevedtak
        sendInnKjøreliste(
            kjøreliste =
                KjørelisteSkjemaUtil.kjørelisteSkjema(
                    rammevedtak1.reiseId,
                    periode = Datoperiode(fom, tom),
                    dagerKjørt = listOf(KjørelisteSkjemaUtil.KjørtDag(fom)),
                ),
            ident = førstegangsBehandlingContext.ident,
        )

        val førsteKjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandlingManuelt(førsteKjørelistebehandling)
        testoppsettService.settAndelerTilOkForBehandling(førsteKjørelistebehandling)

        // Sender inn kjøreliste for andre rammevedtak
        sendInnKjøreliste(
            kjøreliste =
                KjørelisteSkjemaUtil.kjørelisteSkjema(
                    rammevedtak2.reiseId,
                    periode = Datoperiode(fom, tom),
                    dagerKjørt = listOf(KjørelisteSkjemaUtil.KjørtDag(fom)),
                ),
            ident = førstegangsBehandlingContext.ident,
        )

        val andreKjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE && it.id != førsteKjørelistebehandling.id }

        gjennomførKjørelisteBehandlingManuelt(andreKjørelistebehandling)

        val sendteIverksettinger =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                .map { it.verdiEllerFeil<IverksettingDto>() }

        assertThat(sendteIverksettinger).hasSize(2)
        val iverksettingFørsteKjørelistebehandling = sendteIverksettinger.minBy { it.vedtakstidspunkt }
        val iverksettingAndreKjørelistebehandling = sendteIverksettinger.maxBy { it.vedtakstidspunkt }

        assertThat(iverksettingFørsteKjørelistebehandling.utbetalinger).hasSize(1)
        assertThat(iverksettingAndreKjørelistebehandling.utbetalinger).hasSize(2)

        // Verifiserer at utbetalingen fra første behandling er med i iverksetting for den andre behandlingen
        assertThat(
            iverksettingAndreKjørelistebehandling.utbetalinger,
        ).contains(iverksettingFørsteKjørelistebehandling.utbetalinger.single())
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
        assertThat(andel.reiseId).isNotNull
    }

    private fun List<Pair<LocalDate, Int>>.kalkulerForventetBeløp(reiseavstandEnVei: BigDecimal): Int =
        sumOf { (dato, parkeringskostnader) ->
            satsDagligReisePrivatBilProvider
                .finnSatsForÅr(dato.year)
                .beløp
                .multiply(reiseavstandEnVei)
                .multiply(2.toBigDecimal())
                .plus(parkeringskostnader.toBigDecimal())
        }.avrundetStønadsbeløp()
            .toInt()
}
