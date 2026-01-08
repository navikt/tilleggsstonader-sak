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
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørAlleTaskMedSenererTriggertid
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeslutteVedtakSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførInngangsvilkårSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførSendTilBeslutterSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførSimuleringSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
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

class FagsakUtbetalingIdMigreringServiceTest : IntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdService: FagsakUtbetalingIdService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Test
    fun `skal migrere til kafka`() {
        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId =
            gjennomførBehandlingsløp(
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

        every { unleashService.isEnabled(Toggle.SKAL_MIGRERE_UTBETALING_TIL_KAFKA) } returns true
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
}
