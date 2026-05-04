package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EndreAvklarteUkerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var oppgaveService: OppgaveService

    val fom = 5 januar 2026
    val tom = 11 januar 2026

    @Test
    fun `fjern dager som overskrider antall dager i rammevedtaket`() {
        val kjørelistebehandling =
            opprettBehandlingOgSendInnKjøreliste(
                dagerKjørt =
                    listOf(
                        KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 8 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 9 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 10 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 11 januar 2026, parkeringsutgift = 50),
                    ),
            )

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id)

        val avklartUkeId =
            reisevurdering
                .single()
                .uker
                .first()
                .avklartUkeId!!

        val request =
            listOf(
                EndreAvklartDagRequest(
                    dato = 5 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 6 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 7 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 8 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 9 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 10 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    parkeringsutgift = null,
                    begrunnelse = "helg",
                ),
                EndreAvklartDagRequest(
                    dato = 11 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    parkeringsutgift = null,
                    begrunnelse = "helg",
                ),
            )

        val oppdatertUke =
            kall.privatBil.oppdaterUke(
                behandlingId = kjørelistebehandling.id,
                avklartUkeId = avklartUkeId,
                avklarteDager = request,
            )

        assertThat(oppdatertUke.status).isEqualTo(UkeStatus.OK_MANUELT)
        assertThat(oppdatertUke.behandletDato).isEqualTo(LocalDate.now())
        // Originalt avvik skal ikke fjernes ved manuell oppdatering, da det kan være relevant for saksbehandler å
        // se at det har vært et avvik som førte til manuell behandling
        assertThat(oppdatertUke.avvik!!.typeAvvik).isEqualTo(TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK)

        oppdatertUke.validerOppdaterteDager(request)
    }

    @Test
    fun `skal feile dersom det ikke sendes inn en hel uke dersom hele uka er innsendt`() {
        val kjørelistebehandling =
            opprettBehandlingOgSendInnKjøreliste(
                dagerKjørt =
                    listOf(
                        KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                    ),
            )

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id)

        val avklartUkeId =
            reisevurdering
                .single()
                .uker
                .first()
                .avklartUkeId!!

        val request =
            listOf(
                EndreAvklartDagRequest(
                    dato = 5 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 6 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
                EndreAvklartDagRequest(
                    dato = 7 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
            )

        kall.privatBil.apiRespons
            .oppdaterUke(
                behandlingId = kjørelistebehandling.id,
                avklartUkeId = avklartUkeId,
                avklarteDager = request,
            ).expectStatus()
            .is5xxServerError()
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("Alle dager i uke må sendes inn")
    }

    @Test
    fun `skal feile dersom det sendes inn endringer på dager som ikke er innenfor uke`() {
        val kjørelistebehandling =
            opprettBehandlingOgSendInnKjøreliste(
                dagerKjørt =
                    listOf(
                        KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                        KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                    ),
            )

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id)

        val avklartUkeId =
            reisevurdering
                .single()
                .uker
                .first()
                .avklartUkeId!!

        val request =
            listOf(
                EndreAvklartDagRequest(
                    dato = 14 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    parkeringsutgift = 50,
                ),
            )

        kall.privatBil.apiRespons
            .oppdaterUke(
                behandlingId = kjørelistebehandling.id,
                avklartUkeId = avklartUkeId,
                avklarteDager = request,
            ).expectStatus()
            .is5xxServerError()
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("Alle dager i uke må sendes inn")
    }

    private fun opprettBehandlingOgSendInnKjøreliste(dagerKjørt: List<KjørtDag>): Saksbehandling {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val rammebehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager = dagerKjørt
                }
            }

        val rammebehandling = behandlingService.hentSaksbehandling(rammebehandlingContext.behandlingId)
        val kjørelistebehandling =
            behandlingService.hentBehandlinger(rammebehandling.fagsakId).first { it.type == BehandlingType.KJØRELISTE }

        plukkOppgaven(kjørelistebehandling.id)

        return behandlingService.hentSaksbehandling(kjørelistebehandling.id)
    }

    private fun UkeVurderingDto.validerOppdaterteDager(request: List<EndreAvklartDagRequest>) {
        this.dager.forEach { oppdatertDag ->
            val requestDag = request.single { it.dato == oppdatertDag.dato }

            assertThat(oppdatertDag.avklartDag?.godkjentGjennomførtKjøring).isEqualTo(requestDag.godkjentGjennomførtKjøring)
            assertThat(oppdatertDag.avklartDag?.parkeringsutgift).isEqualTo(requestDag.parkeringsutgift)
            assertThat(oppdatertDag.avklartDag?.begrunnelse).isEqualTo(requestDag.begrunnelse)
        }
    }

    private fun plukkOppgaven(behandlingId: BehandlingId) {
        val opppgave = oppgaveService.hentAktivBehandleSakOppgave(behandlingId)
        oppgaveService.fordelOppgave(opppgave.id, testBrukerkontekst.bruker, opppgave.versjon)
    }

    private fun LocalDate.datoErHelg() = dayOfWeek.value >= 6
}
