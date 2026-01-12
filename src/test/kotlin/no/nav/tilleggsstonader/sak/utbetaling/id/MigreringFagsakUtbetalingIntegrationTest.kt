package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.OpprettOpphør
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørAlleTaskMedSenererTriggertid
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeslutteVedtakSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførInngangsvilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførSendTilBeslutterSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførSimuleringSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class MigreringFagsakUtbetalingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdService: FagsakUtbetalingIdService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var utbetalingStatusHåndterer: UtbetalingStatusHåndterer

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `behandling blir iverksatt mot gammelt endepunkt, skrur på toggle for migrering, utbetaling for revurdering blir migrert`() {
        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId = opprettFørstegangsbehandling(fom, tom)
        kjørAlleTaskMedSenererTriggertid()

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isFalse
        verify(exactly = 1) { iverksettClient.simulerV2(any()) }
        verify(exactly = 1) { iverksettClient.iverksett(any()) }
        verify(exactly = 0) { iverksettClient.simulerV3(any()) }
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        every { unleashService.isEnabled(Toggle.SKAL_MIGRERE_UTBETALING_MOT_KAFKA) } returns true
        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 15 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )
        gjennomførInngangsvilkårSteg(
            behandlingId = revurderingId,
            medMålgruppe = { behandlingId ->
                lagreVilkårperiodeMålgruppe(
                    behandlingId = behandlingId,
                    målgruppeType = MålgruppeType.AAP,
                    fom = 1 oktober 2025,
                    tom = 10 oktober 2025,
                )
            },
        )
        gjennomførBeregningSteg(revurderingId, Stønadstype.LÆREMIDLER)
        gjennomførSimuleringSteg(revurderingId)
        gjennomførSendTilBeslutterSteg(revurderingId)
        gjennomførBeslutteVedtakSteg(revurderingId)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val revurdering = behandlingService.hentBehandling(revurderingId)
        val finnesUtbetalingIdEtterRevurdering =
            fagsakUtbetalingIdService.finnesUtbetalingsId(revurdering.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterRevurdering).isTrue
        verify(exactly = 1) { iverksettClient.simulerV2(any()) }
        verify(exactly = 1) { iverksettClient.iverksett(any()) }
        verify(exactly = 1) { iverksettClient.simulerV3(any()) }
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
            .map { it.verdiEllerFeil<IverksettingDto>() }
            .forEach { iverksettingDto ->
                assertThat(iverksettingDto.utbetalinger.all { it.brukFagområdeTillst }).isTrue
            }
    }

    @Test
    fun `behandle og opphør ny læremidler-sak, feature-toggle for iverksette nytt grensesnitt på, skal sendes gjennom ny iverksetting`() {
        every { unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA) } returns true

        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId = opprettFørstegangsbehandling(fom, tom)
        kjørAlleTaskMedSenererTriggertid()

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isTrue
        verify(exactly = 0) { iverksettClient.simulerV2(any()) }
        verify(exactly = 0) { iverksettClient.iverksett(any()) }
        verify(exactly = 1) { iverksettClient.simulerV3(any()) }
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)

        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId = førstegangsbehandlingId.toString(),
            melding =
                UtbetalingStatusRecord(
                    status = UtbetalingStatus.OK,
                    detaljer = null,
                    error = null,
                ),
            utbetalingGjelderFagsystem = UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )

        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 15 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )
        gjennomførInngangsvilkårSteg(behandlingId = revurderingId)

        // Opphører alt
        gjennomførBeregningSteg(revurderingId, Stønadstype.LÆREMIDLER, OpprettOpphør(opphørsdato = fom))
        gjennomførSimuleringSteg(revurderingId)
        gjennomførSendTilBeslutterSteg(revurderingId)
        gjennomførBeslutteVedtakSteg(revurderingId)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val revurdering = behandlingService.hentBehandling(revurderingId)
        val finnesUtbetalingIdEtterRevurdering =
            fagsakUtbetalingIdService.finnesUtbetalingsId(revurdering.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterRevurdering).isTrue
        verify(exactly = 0) { iverksettClient.simulerV2(any()) }
        verify(exactly = 0) { iverksettClient.iverksett(any()) }
        verify(exactly = 2) { iverksettClient.simulerV3(any()) }
        val opphørUtbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .maxBy { it.vedtakstidspunkt }

        assertThat(opphørUtbetaling.utbetalinger.size).isEqualTo(1)
        assertThat(opphørUtbetaling.utbetalinger.single().perioder).isEmpty()

        val andelerRevurdering = tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse
        assertThat(andelerRevurdering).hasSize(1)
        assertThat(andelerRevurdering.single().erNullandel()).isTrue
    }

    @Test
    fun `behandle og opphør ny læremidler-sak, toggle for iverksette nytt grensesnitt av, skal sendes gjennom gammelt iverksetting`() {
        every { unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA) } returns false

        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId = opprettFørstegangsbehandling(fom, tom)
        kjørAlleTaskMedSenererTriggertid()

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isFalse
        verify(exactly = 1) { iverksettClient.simulerV2(any()) }
        verify(exactly = 1) { iverksettClient.iverksett(any()) }
        verify(exactly = 0) { iverksettClient.simulerV3(any()) }
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        kravMottatt = 15 februar 2025,
                        nyeOpplysningerMetadata = null,
                    ),
            )
        gjennomførInngangsvilkårSteg(behandlingId = revurderingId)

        // Opphører alt
        gjennomførBeregningSteg(revurderingId, Stønadstype.LÆREMIDLER, OpprettOpphør(opphørsdato = fom))
        gjennomførSimuleringSteg(revurderingId)
        gjennomførSendTilBeslutterSteg(revurderingId)
        gjennomførBeslutteVedtakSteg(revurderingId)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val revurdering = behandlingService.hentBehandling(revurderingId)
        val finnesUtbetalingIdEtterRevurdering =
            fagsakUtbetalingIdService.finnesUtbetalingsId(revurdering.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        assertThat(finnesUtbetalingIdEtterRevurdering).isFalse
        verify(exactly = 2) { iverksettClient.simulerV2(any()) }
        verify(exactly = 2) { iverksettClient.iverksett(any()) }
        verify(exactly = 0) { iverksettClient.simulerV3(any()) }
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
            .map { it.verdiEllerFeil<IverksettingDto>() }
            .forEach { iverksettingDto ->
                assertThat(iverksettingDto.utbetalinger.all { it.brukFagområdeTillst }).isTrue
            }
    }

    private fun opprettFørstegangsbehandling(
        fom: LocalDate,
        tom: LocalDate,
    ): BehandlingId =
        opprettBehandlingOgGjennomførBehandlingsløp(
            fraJournalpost =
                journalpost(
                    journalpostId = "1",
                    journalstatus = Journalstatus.MOTTATT,
                    dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.LÆREMIDLER.verdi)),
                    bruker = Bruker("12345678910", BrukerIdType.FNR),
                    tema = Tema.TSO.name,
                ),
            medAktivitet = { behandlingId ->
                lagreVilkårperiodeAktivitet(
                    behandlingId = behandlingId,
                    aktivitetType = AktivitetType.UTDANNING,
                    fom = fom,
                    tom = tom,
                    faktaOgSvar =
                        FaktaOgSvarAktivitetLæremidlerDto(
                            prosent = 100,
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            svarHarUtgifter = SvarJaNei.JA,
                            svarHarRettTilUtstyrsstipend = SvarJaNei.NEI,
                        ),
                )
            },
            medMålgruppe = { behandlingId ->
                lagreVilkårperiodeMålgruppe(
                    behandlingId = behandlingId,
                    målgruppeType = MålgruppeType.AAP,
                    fom = fom,
                    tom = tom,
                )
            },
            medVilkår = emptyList(),
        )
}
