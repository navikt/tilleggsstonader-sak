package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GjenbrukUkerIKjørelistebehandlingIntegrationTest : IntegrationTest() {
    private val førsteUkeFom = 5 januar 2026
    private val førsteUkeTom = 11 januar 2026
    private val andreUkeFom = 12 januar 2026
    private val tredjeUkeTom = 25 januar 2026

    lateinit var førstegangsbehandling: Behandling
    lateinit var førsteKjørelistebehandling: Behandling
    lateinit var andreKjørelistebehandling: Behandling
    lateinit var reisevurderingForFørsteKjørelistebehandling: ReisevurderingPrivatBilDto
    lateinit var reisevurderingForAndreKjørelistebehandling: ReisevurderingPrivatBilDto

    @BeforeEach
    fun `opprett daglig-reise sak med to kjørelistebehandlinger`() {
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
                defaultDagligReisePrivatBilTsoTestdata(førsteUkeFom, tredjeUkeTom)

                sendInnKjøreliste {
                    periode = Datoperiode(førsteUkeFom, førsteUkeTom)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        førstegangsbehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)

        førsteKjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        reisevurderingForFørsteKjørelistebehandling =
            kall.privatBil
                .hentReisevurderingForBehandling(førsteKjørelistebehandling.id)
                .single()

        gjennomførKjørelisteBehandling(førsteKjørelistebehandling)

        val reiseId =
            kall.privatBil
                .hentRammevedtak(førstegangsBehandlingContext.ident)
                .single()
                .reiseId

        // Sender inn kjøreliste som dekker to uker, totalt sendt inn 3 uker
        sendInnKjøreliste(
            kjøreliste =
                kjørelisteSkjema(
                    reiseId = reiseId,
                    periode = Datoperiode(andreUkeFom, tredjeUkeTom),
                    dagerKjørt =
                        listOf(
                            KjørtDag(dato = 12 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 13 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 20 januar 2026, parkeringsutgift = 50),
                        ),
                ),
            ident = førstegangsBehandlingContext.ident,
        )

        val kjørelistebehandlinger =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .filter { it.type == BehandlingType.KJØRELISTE }

        andreKjørelistebehandling = kjørelistebehandlinger.single { it.id != førsteKjørelistebehandling.id }
        reisevurderingForAndreKjørelistebehandling =
            kall.privatBil.hentReisevurderingForBehandling(andreKjørelistebehandling.id).single()
        tilordneÅpenBehandlingOppgaveForBehandling(andreKjørelistebehandling.id)
    }

    @Test
    fun `avklarte uker fra første kjørelistebehandling blir kopiert over til påfølgende kjørelistebehandling`() {
        val førsteUkeIFørsteBehandling =
            reisevurderingForFørsteKjørelistebehandling
                .uker
                .single { it.fraDato == førsteUkeFom }

        assertThat(andreKjørelistebehandling.forrigeIverksatteBehandlingId).isEqualTo(førsteKjørelistebehandling.id)

        val reisevurderingForAndreKjørelistebehandling =
            kall.privatBil.hentReisevurderingForBehandling(andreKjørelistebehandling.id).single()

        assertThat(reisevurderingForAndreKjørelistebehandling.uker).hasSize(3)

        val førsteUkeIAndreBehandling =
            reisevurderingForAndreKjørelistebehandling.uker.single { it.fraDato == førsteUkeFom }
        val andreUkeIAndreBehandling =
            reisevurderingForAndreKjørelistebehandling.uker.single { it.fraDato == andreUkeFom }

        assertUkerErLike(førsteUkeIFørsteBehandling, førsteUkeIAndreBehandling)

        assertThat(andreUkeIAndreBehandling.status).isEqualTo(UkeStatus.OK_AUTOMATISK)
        assertThat(andreUkeIAndreBehandling.kjørelisteId).isNotNull()
        assertThat(andreUkeIAndreBehandling.avklartUkeId).isNotNull()
        assertThat(andreUkeIAndreBehandling.ukenummer).isNotEqualTo(førsteUkeIAndreBehandling.ukenummer)
        assertThat(andreUkeIAndreBehandling.dager.map { it.avklartDag }).doesNotContainNull()
    }

    @Test
    fun `nullstilling av en kjørelistebehandling skal sette behandlingen tilbake til samme state som den opprinnelig var`() {
        val førsteUkeIAndreBehandling =
            reisevurderingForAndreKjørelistebehandling.uker.single { it.fraDato == førsteUkeFom }

        // Endrer en uke i behandlingen før nullstilling
        kall.privatBil.oppdaterUke(
            behandlingId = andreKjørelistebehandling.id,
            avklartUkeId = førsteUkeIAndreBehandling.avklartUkeId!!,
            avklarteDager =
                førsteUkeIAndreBehandling.dager.map {
                    EndreAvklartDagRequest(
                        dato = it.dato,
                        godkjentGjennomførtKjøring = it.avklartDag!!.godkjentGjennomførtKjøring,
                        parkeringsutgift = it.kjørelisteDag!!.parkeringsutgift,
                        begrunnelse = it.avklartDag.begrunnelse,
                    )
                },
        )
        kall.behandling.nullstill(andreKjørelistebehandling.id)

        val reisevurderingEtterNullstilling = kall.privatBil.hentReisevurderingForBehandling(andreKjørelistebehandling.id).single()
        assertThat(reisevurderingEtterNullstilling.uker).hasSameSizeAs(reisevurderingForAndreKjørelistebehandling.uker)

        reisevurderingForAndreKjørelistebehandling.uker
            .sortedBy { it.ukenummer }
            .zip(reisevurderingEtterNullstilling.uker.sortedBy { it.ukenummer })
            .forEach { (ukeFørNullstilling, ukeEtterNullstilling) ->
                assertUkerErLike(ukeFørNullstilling, ukeEtterNullstilling)
            }
    }

    private fun assertUkerErLike(
        ukeBehandling1: UkeVurderingDto,
        ukeBehandling2: UkeVurderingDto,
    ) {
        assertThat(ukeBehandling1.ukenummer).isEqualTo(ukeBehandling2.ukenummer)
        assertThat(ukeBehandling1.fraDato).isEqualTo(ukeBehandling2.fraDato)
        assertThat(ukeBehandling1.tilDato).isEqualTo(ukeBehandling2.tilDato)
        assertThat(ukeBehandling1.kjørelisteId).isEqualTo(ukeBehandling2.kjørelisteId)
        assertThat(ukeBehandling1.avklartUkeId).isNotEqualTo(ukeBehandling2.avklartUkeId)
        assertThat(ukeBehandling1.status).isEqualTo(ukeBehandling2.status)
        assertThat(ukeBehandling1.avvik).isEqualTo(ukeBehandling2.avvik)
        assertThat(ukeBehandling1.behandletDato).isEqualTo(ukeBehandling2.behandletDato)
        assertThat(ukeBehandling1.kjørelisteInnsendtDato).isEqualTo(ukeBehandling2.kjørelisteInnsendtDato)
        assertThat(ukeBehandling1.dager).hasSameSizeAs(ukeBehandling2.dager)

        ukeBehandling1.dager
            .sortedBy { it.dato }
            .zip(ukeBehandling2.dager.sortedBy { it.dato })
            .forEach { (dagBehandling1, dagBehandling2) ->
                assertThat(dagBehandling1).isEqualTo(dagBehandling2)
            }
    }
}
