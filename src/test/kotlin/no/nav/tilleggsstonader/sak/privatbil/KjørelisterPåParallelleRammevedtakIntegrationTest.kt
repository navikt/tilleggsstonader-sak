package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandlingAutomatisk
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KjørelisterPåParallelleRammevedtakIntegrationTest : CleanDatabaseIntegrationTest() {
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

        reiseIdRamme1 = rammevedtak.single { it.fom == fomRamme1 }.reiseId
        reiseIdRamme2 = rammevedtak.single { it.fom == fomRamme2 }.reiseId
        brukerident = førstegangsBehandlingContext.ident

        førstegangsbehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)
    }

    @Test
    fun `innsending av kjøreliste på ulike rammevedtak skal ikke påvirke hverandre`() {
        val kjørelistebehandling1 =
            sendInnKjøreliste(
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme1,
                        periode = Datoperiode(fomRamme1, tomRamme1),
                        dagerKjørt =
                            listOf(
                                KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                                KjørtDag(dato = 12 januar 2026, parkeringsutgift = 50),
                                KjørtDag(dato = 19 januar 2026, parkeringsutgift = 50),
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

        gjennomførKjørelisteBehandlingAutomatisk(kjørelistebehandling1)

        // Sender inn kjøreliste for nytt rammevedtak
        val kjørelistebehandling2 =
            sendInnKjøreliste(
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme2,
                        periode = Datoperiode(fomRamme2, tomRamme2),
                        dagerKjørt =
                            listOf(
                                KjørtDag(dato = 12 januar 2026, parkeringsutgift = 50),
                                KjørtDag(dato = 19 januar 2026, parkeringsutgift = 50),
                                KjørtDag(dato = 26 januar 2026, parkeringsutgift = 120),
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

    @Test
    fun `skal håndtere innsending av flere kjørelister på samme rammevedtak`() {
        // Sender inn kjøreliste for første uke
        val kjørelistebehandling =
            sendInnKjøreliste(
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme1,
                        periode = Datoperiode(fomRamme1, 11 januar 2026),
                        dagerKjørt =
                            listOf(
                                KjørtDag(dato = 5 januar 2026, parkeringsutgift = 120),
                            ),
                    ),
            )

        // sender inn kjøreliste for andre og tredje uke før første kjørelistebehandling er ferdigbehandlet
        val kjørelistebehandlingEtterAndreInnsending =
            sendInnKjøreliste(
                kjøreliste =
                    kjørelisteSkjema(
                        reiseId = reiseIdRamme1,
                        periode = Datoperiode(12 januar 2026, tomRamme1),
                        dagerKjørt =
                            listOf(
                                KjørtDag(dato = 12 januar 2026, parkeringsutgift = 50),
                                KjørtDag(dato = 19 januar 2026, parkeringsutgift = 50),
                            ),
                    ),
            )

        // Skal kun opprettes en behandling, som tar for seg begge kjørelister
        assertThat(kjørelistebehandling).isEqualTo(kjørelistebehandlingEtterAndreInnsending)

        // Henter reisevurderinger for begge behandlingene
        val (ramme1, ramme2) = finnReisevurderinger(kjørelistebehandling.id)
        assertThat(ramme1).isNotNull
        assertThat(ramme2).isNotNull

        // Verifiser at begge uker finnes på ramme1 for behandlingen
        val ukerHvorDetFinnesAvklarteUker = ramme1!!.uker.filter { it.kjørelisteInnsendtDato != null && it.avklartUkeId != null }
        assertThat(ukerHvorDetFinnesAvklarteUker).hasSameSizeAs(ramme1.uker)

        // Verifiser ingen uker på ramme2
        assertThat(ramme2!!.uker.filter { it.kjørelisteInnsendtDato != null && it.avklartUkeId != null }).isEmpty()
    }

    private fun sendInnKjøreliste(kjøreliste: KjørelisteSkjema): Behandling {
        // Hent eksisterende kjørelistebehandlinger før innsendingen
        val eksisterendeBehandlinger =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .filter { it.type == BehandlingType.KJØRELISTE }
                .map { it.id }
                .toSet()

        sendInnKjøreliste(
            kjøreliste = kjøreliste,
            ident = brukerident,
        )

        val kjørelistebehandlingerEtterInnsending =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .filter { it.type == BehandlingType.KJØRELISTE }

        // Dersom innsendingen oppretter ny behandling returnerer vi den.
        val nyBehandling = kjørelistebehandlingerEtterInnsending.firstOrNull { it.id !in eksisterendeBehandlinger }
        if (nyBehandling != null) return nyBehandling

        // Ved ny innsending på en ikke-ferdigstilt kjørelistebehandling gjenbrukes eksisterende behandling.
        val aktivEksisterendeBehandling =
            kjørelistebehandlingerEtterInnsending
                .filter { it.id in eksisterendeBehandlinger }
                .singleOrNull { it.erAktiv() }

        return aktivEksisterendeBehandling
            ?: throw AssertionError("Ingen ny kjørelistebehandling ble opprettet, og fant heller ingen aktiv eksisterende behandling")
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
