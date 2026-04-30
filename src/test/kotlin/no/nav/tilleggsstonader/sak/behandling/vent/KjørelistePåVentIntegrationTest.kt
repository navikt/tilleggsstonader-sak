package no.nav.tilleggsstonader.sak.behandling.vent

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandlingManuelt
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KjørelistePåVentIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var taAvVentService: TaAvVentService

    private val fomUke1 = 5 januar 2026
    private val tomUke1 = 11 januar 2026
    private val fomUke2 = 12 januar 2026
    private val tomUke2 = 18 januar 2026

    lateinit var brukerident: String
    lateinit var reiseId: String
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
                defaultDagligReisePrivatBilTsoTestdata(fomUke1, tomUke2)

                sendInnKjøreliste {
                    periode = Datoperiode(fomUke1, tomUke1)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026, parkeringsutgift = 120),
                        )
                }
            }

        val rammevedtak =
            kall.privatBil
                .hentRammevedtak(førstegangsBehandlingContext.ident)

        reiseId = rammevedtak.first().reiseId
        brukerident = førstegangsBehandlingContext.ident

        // TODO: Bare lagre denne som fagsakId?
        førstegangsbehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)
    }

    @Test
    fun `skal sette nye kjørelistebehandlinger på vent om det finnes åpen kjørelistebehandling`() {
        val kjørelisteBehandling1 =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        // Påbegynt behandling skal ikke gjenbrukes når ny kjøreliste kommer inn.
        tilordneÅpenBehandlingOppgaveForBehandling(kjørelisteBehandling1.id)

        // Sender inn en ny kjøreliste hvor behandlingen blir satt på vent
        sendInnKjøreliste(
            kjørelisteSkjema(
                reiseId = reiseId,
                periode = Datoperiode(fomUke2, tomUke2),
                dagerKjørt =
                    listOf(
                        KjørtDag(dato = 12 januar 2026, parkeringsutgift = 120),
                    ),
            ),
            ident = brukerident,
        )

        val kjørelisteBehandling2 =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE && it.id != kjørelisteBehandling1.id }

        assertThat(kjørelisteBehandling2.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

        val avklartUkeForFørsteKjørelistebehandling =
            kall.privatBil
                .hentReisevurderingForBehandling(kjørelisteBehandling1.id)
                .single()
                .uker
                .first { it.fraDato == fomUke1 }

        tilordneÅpenBehandlingOppgaveForBehandling(kjørelisteBehandling1.id)
        kall.privatBil.oppdaterUke(
            behandlingId = kjørelisteBehandling1.id,
            avklartUkeId = avklartUkeForFørsteKjørelistebehandling.avklartUkeId!!,
            avklarteDager =
                avklartUkeForFørsteKjørelistebehandling.dager.map { dag ->
                    EndreAvklartDagRequest(
                        dato = dag.dato,
                        godkjentGjennomførtKjøring =
                            when {
                                dag.kjørelisteDag?.harKjørt == true -> GodkjentGjennomførtKjøring.JA
                                else -> GodkjentGjennomførtKjøring.NEI
                            },
                        parkeringsutgift = dag.kjørelisteDag?.parkeringsutgift,
                        begrunnelse = "Avklart i test",
                    )
                },
        )

        // Fullfører første kjørelistebehandling
        gjennomførKjørelisteBehandlingManuelt(kjørelisteBehandling1)

        // Ny kjørelistebehandling tas av vent og skal nullstilles
        tilordneÅpenBehandlingOppgaveForBehandling(kjørelisteBehandling2.id)
        kall.settPaVent.taAvVent(kjørelisteBehandling2.id, TaAvVentDto())

        val nullstiltBehandling = testoppsettService.hentBehandling(kjørelisteBehandling2.id)

        assertThat(nullstiltBehandling.forrigeIverksatteBehandlingId).isEqualTo(kjørelisteBehandling1.id)
        assertThat(nullstiltBehandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(nullstiltBehandling.steg).isEqualTo(StegType.KJØRELISTE)
        assertThat(nullstiltBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
    }
}
