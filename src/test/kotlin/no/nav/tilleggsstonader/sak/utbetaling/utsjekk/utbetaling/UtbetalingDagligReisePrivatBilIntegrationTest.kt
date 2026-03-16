package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBilProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.RoundingMode

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
        val reiseavstandEnVei = 10
        val behandlingId =
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
                    kjørteDager =
                        listOf(
                            2 februar 2026 to 50,
                            4 februar 2026 to 50,
                            5 februar 2026 to 50,
                        )
                }
            }

        val førstegangsBehandling = testoppsettService.hentBehandling(behandlingId)
        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandling(kjørelisteBehandling)

        val andelerForKjørelistebehandling = tilkjentYtelseRepository.findByBehandlingId(kjørelisteBehandling.id)?.andelerTilkjentYtelse
        assertThat(andelerForKjørelistebehandling).isNotNull

        // Andeler blir opprettet per uke. Alle tre uker er sendt inn, men kun første uke har beløp > 0. De to siste filtreres ut
        assertThat(andelerForKjørelistebehandling).hasSize(1)

        val andel = andelerForKjørelistebehandling!!.single()
        assertThat(andel.type).isEqualTo(TypeAndel.DAGLIG_REISE_AAP)
        assertThat(andel.beløp).isEqualTo(
            satsDagligReisePrivatBilProvider
                .finnSatsForÅr(fom.year)
                .beløp
                .multiply(reiseavstandEnVei.toBigDecimal())
                .multiply(2.toBigDecimal()) // tur/retur, reiseavstand
                .multiply(3.toBigDecimal()) // 3 reisedager
                .plus(150.toBigDecimal()) // totale parkeringskostnader
                .setScale(0, RoundingMode.HALF_UP)
                .toInt(),
        )

        // TODO - innvilge og sende andeler til helved
    }
}
