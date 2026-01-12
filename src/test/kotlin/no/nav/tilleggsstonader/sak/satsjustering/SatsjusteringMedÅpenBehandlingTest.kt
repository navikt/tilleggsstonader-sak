package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerProvider
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.bekreftedeSatser
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SatsjusteringMedÅpenBehandlingTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    private lateinit var satsLæremidlerProvider: SatsLæremidlerProvider

    @Autowired
    private lateinit var læremidlerBeregnYtelseSteg: LæremidlerBeregnYtelseSteg

    @Autowired
    private lateinit var settPåVentRepository: SettPåVentRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val sisteBekreftedeSatsÅr = bekreftedeSatser.maxOf { it.fom.year }
    private val fom = LocalDate.of(sisteBekreftedeSatsÅr, 8, 1)
    private val tom = LocalDate.of(sisteBekreftedeSatsÅr + 1, 6, 30)

    @AfterEach
    fun resetMock() {
        clearMocks(satsLæremidlerProvider)
    }

    @Test
    fun `satsjustering setter åpen behandling i status OPPRETTET med ufordelt oppgave på vent og tar den av vent etterpå`() {
        val førstegangsbehandling = opprettFørstegangsbehandlingMedAndelerTilSatsjustering()
        val fagsakId = førstegangsbehandling.fagsakId

        // Opprett en ny behandling i status OPPRETTET (uten å tilordne oppgaven)
        val nyÅpenBehandling = opprettNyBehandlingIStatusOpprettet(fagsakId)
        val oppgave = oppgaveRepository.findByBehandlingId(nyÅpenBehandling.id).single()
        assertThat(oppgave.tilordnetSaksbehandler).isNull()

        mockSatser()

        medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
            kall.satsjustering.satsjustering(Stønadstype.LÆREMIDLER)
        }

        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        // Verifiser at satsjusteringen er gjennomført
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!
        assertThat(sisteIverksatteBehandling.årsak).isEqualTo(BehandlingÅrsak.SATSENDRING)

        // Verifiser at den åpne behandlingen ikke lenger er på vent og er nullstilt
        val oppdatertÅpenBehandling = behandlingRepository.findById(nyÅpenBehandling.id).get()
        assertThat(oppdatertÅpenBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(oppdatertÅpenBehandling.forrigeIverksatteBehandlingId).isEqualTo(sisteIverksatteBehandling.id)

        // Verifiser at settPåVent er inaktiv
        val settPåVent = settPåVentRepository.findByBehandlingIdAndAktivIsTrue(nyÅpenBehandling.id)
        assertThat(settPåVent).isNull()

        // Verifiser at oppgaven fortsatt er ufordelt
        val oppdatertOppgave =
            oppgaveRepository
                .findByBehandlingId(nyÅpenBehandling.id)
                .single { it.status == Oppgavestatus.ÅPEN }
        assertThat(oppdatertOppgave.tilordnetSaksbehandler).isNull()
    }

    @Test
    fun `satsjustering setter ikke behandling på vent hvis oppgaven er fordelt`() {
        val førstegangsbehandling = opprettFørstegangsbehandlingMedAndelerTilSatsjustering()
        val fagsakId = førstegangsbehandling.fagsakId

        // Opprett en ny behandling i status OPPRETTET med fordelt oppgave
        val nyÅpenBehandling = opprettNyBehandlingIStatusOpprettet(fagsakId)
        val oppgave = oppgaveRepository.findByBehandlingId(nyÅpenBehandling.id).single()
        oppgaveRepository.update(oppgave.copy(tilordnetSaksbehandler = "saksbehandler123"))

        mockSatser()

        // Satsjustering skal kaste RekjørSenereException fordi oppgaven er fordelt
        medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.satsjustering.satsjustering(Stønadstype.LÆREMIDLER)
        }

        kjørTasksKlareForProsessering()

        val satsjusteringTask = taskService.findAll()
            .filter { it.type == SatsjusteringTask.TYPE }
            .singleOrNull { it.payload == førstegangsbehandling.id.toString() }

        // Satsjusteringstasken skal ha feilet og skal bli kjørt i morgen kl 0700
        assertThat(satsjusteringTask).isNotNull
        assertThat(satsjusteringTask!!.status).isEqualTo(Status.KLAR_TIL_PLUKK)
        assertThat(satsjusteringTask.triggerTid).isEqualTo(LocalDate.now().plusDays(1).atTime(7, 0))

        // Verifiser at behandlingen fortsatt er i status OPPRETTET (ikke satt på vent og tatt av igjen)
        val oppdatertÅpenBehandling = behandlingRepository.findById(nyÅpenBehandling.id).get()
        assertThat(oppdatertÅpenBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        // Ingen satsjustering er opprettet
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!
        assertThat(sisteIverksatteBehandling.id).isEqualTo(førstegangsbehandling.id)
    }

    @Test
    fun `satsjustering setter ikke behandling på vent hvis behandlingen ikke er i status OPPRETTET`() {
        val førstegangsbehandling = opprettFørstegangsbehandlingMedAndelerTilSatsjustering()
        val fagsakId = førstegangsbehandling.fagsakId

        // Opprett en ny behandling og oppdater status til UTREDES
        val nyÅpenBehandling = opprettNyBehandlingIStatusOpprettet(fagsakId)
        val oppdatertBehandling =
            behandlingRepository.update(
                behandlingRepository.findById(nyÅpenBehandling.id).get().copy(status = BehandlingStatus.UTREDES),
            )

        mockSatser()

        medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
            kall.satsjustering.satsjustering(Stønadstype.LÆREMIDLER)
        }

        // Verifiser at behandlingen fortsatt er i UTREDES status
        val etterSatsjustering = behandlingRepository.findById(oppdatertBehandling.id).get()
        assertThat(etterSatsjustering.status).isEqualTo(BehandlingStatus.UTREDES)

        // Ingen satsjustering er opprettet
        val sisteIverksatteBehandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)!!
        assertThat(sisteIverksatteBehandling.id).isEqualTo(førstegangsbehandling.id)
    }

    private fun opprettFørstegangsbehandlingMedAndelerTilSatsjustering(): Behandling {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), stønadstype = Stønadstype.LÆREMIDLER)

        lagreVilkårOgVedtak(behandling, fom, tom)

        return testoppsettService.ferdigstillBehandling(behandling)
    }

    private fun opprettNyBehandlingIStatusOpprettet(fagsakId: FagsakId): Behandling {
        val ident = fagsakRepository.finnAktivIdent(fagsakId)

        val journalpost =
            journalpost(
                journalpostId = "2",
                journalstatus = Journalstatus.MOTTATT,
                dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.LÆREMIDLER.verdi)),
                bruker = Bruker(ident, BrukerIdType.FNR),
                tema = Tema.TSO.name,
            )
        opprettJournalpost(journalpost)

        val behandlingId = håndterSøknadService.håndterSøknad(journalpost)!!.id
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        return behandlingRepository.findById(behandlingId).get()
    }

    private fun lagreVilkårOgVedtak(
        behandling: Behandling,
        fom: LocalDate,
        tom: LocalDate,
    ) {
        vilkårperiodeRepository.insertAll(
            listOf(
                målgruppe(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingMålgruppeLæremidler()),
                aktivitet(behandlingId = behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingAktivitetLæremidler()),
            ),
        )

        læremidlerBeregnYtelseSteg.utførSteg(
            saksbehandling = behandlingRepository.finnSaksbehandling(behandling.id),
            InnvilgelseLæremidlerRequest(
                vedtaksperioder = listOf(vedtaksperiodeDto(fom = fom, tom = tom)),
            ),
        )
    }

    private fun mockSatser() {
        val nyMakssats = 10_000
        val ubekreftetSats = satsLæremidlerProvider.satser.first { !it.bekreftet }
        val nyBekreftetSats =
            ubekreftetSats.copy(
                tom =
                    ubekreftetSats.fom
                        .toYearMonth()
                        .withMonth(12)
                        .atEndOfMonth(),
                bekreftet = true,
                beløp =
                    ubekreftetSats.beløp
                        .map { it.key to nyMakssats }
                        .toMap(),
            )
        val nyUbekreftetSats =
            ubekreftetSats.copy(
                fom = ubekreftetSats.fom.plusYears(1),
                beløp =
                    ubekreftetSats.beløp
                        .map { it.key to nyMakssats }
                        .toMap(),
            )

        every {
            satsLæremidlerProvider.satser
        } returns bekreftedeSatser + nyBekreftetSats + nyUbekreftetSats
    }
}
