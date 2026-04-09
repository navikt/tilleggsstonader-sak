package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KjørelisterPåParallelleRammevedtakIntegrationTest : IntegrationTest() {
    private val fomRamme1 = 5 januar 2026
    private val tomRamme1 = 25 januar 2026

    private val fomRamme2 = 12 januar 2026
    private val tomRamme2 = 1 februar 2026

    lateinit var brukerident: String
    lateinit var reiseIdRamme1: String
    lateinit var reiseIdRamme2: String
    lateinit var førstegangsbehandling: Behandling

    @BeforeEach
    fun `opprett daglig-reise sak med to rammevedtak`() {
        testBrukerkontekst =
            TestBrukerKontekst(
                defaultBruker = "julenissen",
                defaultRoller = listOf(rolleConfig.beslutterRolle),
            )

        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val førstegangsBehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fomRamme1, tomRamme2)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fomRamme1, tomRamme2)
                    }
                }
                vilkår {
                    opprett {
                        privatBil(fomRamme1, tomRamme1, reisedagerPerUke = 3)
                        privatBil(fomRamme2, tomRamme2, reisedagerPerUke = 2)
                    }
                }
            }

        val rammevedtak =
            kall.privatBil
                .hentRammevedtak(førstegangsBehandlingContext.ident)

        reiseIdRamme1 = rammevedtak.first().reiseId
        reiseIdRamme2 = rammevedtak.last().reiseId
        brukerident = førstegangsBehandlingContext.ident

        førstegangsbehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)
    }

    @Test
    fun `innsending av kjøreliste på ulike rammevedtak skal ikke påvirke hverandre`() {
        val kjørelistebehandling1 =
            sendInnKjøreliste(
                indeksKjørelistebehandling = 0,
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme1,
                        periode = Datoperiode(fomRamme1, tomRamme1),
                        dagerKjørt =
                            listOf(
                                KjørtDag(5 januar 2026, 50),
                                KjørtDag(12 januar 2026, 50),
                                KjørtDag(19 januar 2026, 50),
                            ),
                    ),
            )

        val reisevurderingerBehandling1 = finnReisevurderinger(kjørelistebehandling1.id)

        // Data for rammevedtak 1 skal være innsendt og vurdert
        reisevurderingerBehandling1.ramme1!!.uker.forEach {
            assertThat(it.kjørelisteInnsendtDato).isNotNull
            assertThat(it.avklartUkeId).isNotNull
        }

        // Ingen data skal være innsendt eller vurdert for rammevedtak 2 siden kjøreliste ikke er sendt inn for denne
        reisevurderingerBehandling1.ramme2!!.uker.forEach {
            assertThat(it.kjørelisteInnsendtDato).isNull()
            assertThat(it.avklartUkeId).isNull()
        }

        gjennomførKjørelisteBehandling(kjørelistebehandling1)

        // Sender inn kjøreliste for nytt rammevedtak
        val kjørelistebehandling2 =
            sendInnKjøreliste(
                indeksKjørelistebehandling = 1,
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme2,
                        periode = Datoperiode(fomRamme2, tomRamme2),
                        dagerKjørt =
                            listOf(
                                KjørtDag(12 januar 2026, 50),
                                KjørtDag(19 januar 2026, 50),
                                KjørtDag(26 januar 2026, 50),
                            ),
                    ),
            )
        val reisevurderingerBehandling2 = finnReisevurderinger(kjørelistebehandling2.id)

        // Data for rammevedtak 1 skal være lik som ved forrige kjørelistebehandling

        reisevurderingerBehandling2.ramme1!!.uker.forEachIndexed { index, uke ->
            assertThat(uke)
                .usingRecursiveComparison()
                .ignoringFields("avklartUkeId") // AvklartUkeId vil være forskjellig siden de kopieres over
                .isEqualTo(reisevurderingerBehandling1.ramme1.uker[index])
        }

        // Data skal være innsendt og vurdert for rammevedtak 2
        reisevurderingerBehandling2.ramme2!!.uker.forEach {
            assertThat(it.kjørelisteInnsendtDato).isNotNull()
            assertThat(it.avklartUkeId).isNotNull()
        }
    }

    private fun sendInnKjøreliste(
        kjøreliste: KjørelisteSkjema,
        indeksKjørelistebehandling: Int = 0,
    ): Behandling {
        sendInnKjøreliste(
            kjøreliste = kjøreliste,
            ident = brukerident,
        )

        return testoppsettService
            .hentBehandlinger(førstegangsbehandling.fagsakId)
            .filter { it.type == BehandlingType.KJØRELISTE }[indeksKjørelistebehandling]
    }

    data class Reisevurderinger(
        val ramme1: ReisevurderingPrivatBilDto?,
        val ramme2: ReisevurderingPrivatBilDto?,
    )

    private fun finnReisevurderinger(behandlingId: BehandlingId): Reisevurderinger {
        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(behandlingId)

        val vurderingRamme1 = reisevurdering.singleOrNull { it.reiseId == ReiseId.fromString(reiseIdRamme1) }
        val vurderingRamme2 = reisevurdering.singleOrNull { it.reiseId == ReiseId.fromString(reiseIdRamme2) }

        return Reisevurderinger(ramme1 = vurderingRamme1, ramme2 = vurderingRamme2)
    }
}
