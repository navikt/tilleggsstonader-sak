package no.nav.tilleggsstonader.sak.utbetaling.id

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
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
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørAlleTaskMedSenererTriggertid
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.DagligIverksettTask
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.StønadUtbetaling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class MigreringFagsakUtbetalingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdService: FagsakUtbetalingIdService

    @Autowired
    lateinit var fagsakUtbetalingIdMigreringController: FagsakUtbetalingIdMigreringController

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
        gjennomførBehandlingsløp(revurderingId) {
            målgruppe {
                opprett {
                    målgruppeAAP(fom = 1 oktober 2025, tom = 10 oktober 2025)
                }
            }
        }

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

        // Opphører alt
        gjennomførBehandlingsløp(revurderingId) {
            vedtak {
                opphør(opphørsdato = fom)
            }
        }
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
    fun `nye boutgifter-saker skal iverksettes gjennom kafka`() {
        every { unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA) } returns true

        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakBoutgifter(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        løpendeutgifterEnBolig(fom, tom)
                    }
                }
            }
        kjørAlleTaskMedSenererTriggertid()

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.BOUTGIFTER_AAP)

        assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isTrue
        verify(exactly = 0) { iverksettClient.simulerV2(any()) }
        verify(exactly = 0) { iverksettClient.iverksett(any()) }
        verify(exactly = 1) { iverksettClient.simulerV3(any()) }

        val sendtUtbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .single()

        assertThat(sendtUtbetaling.utbetalinger).hasSize(1)
        assertThat(sendtUtbetaling.utbetalinger.single().stønad).isEqualTo(StønadUtbetaling.BOUTGIFTER_AAP)

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
    }

    @Test
    fun `nye tilsyn-barn-saker skal iverksettes gjennom kafka`() {
        every { unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA) } returns true

        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTilsynBarn(fom, tom, aktivitetsdager = 5)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        passBarn(fom.toYearMonth(), tom.toYearMonth(), utgift = 1000)
                    }
                }
            }
        kjørAlleTaskMedSenererTriggertid()

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.TILSYN_BARN_AAP)

        assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isTrue
        verify(exactly = 0) { iverksettClient.simulerV2(any()) }
        verify(exactly = 0) { iverksettClient.iverksett(any()) }
        verify(exactly = 1) { iverksettClient.simulerV3(any()) }

        val sendtUtbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .single()

        assertThat(sendtUtbetaling.utbetalinger).hasSize(1)
        assertThat(sendtUtbetaling.utbetalinger.single().stønad).isEqualTo(StønadUtbetaling.TILSYN_BARN_AAP)

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

        // Revurdering opphører alt
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingId,
            ) {
                vedtak {
                    opphør(opphørsdato = fom)
                }
            }

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

    @Test
    fun `førstegangsbehandling med flere andeler hvor første andel har gått over rest skal ikke på kafka om migrering-toggle av`() {
        val fom = 1 september 2025
        val tom = 30 september 2025

        val behandlingId = opprettFørstegangsbehandling(fom, tom)
        // For at sendte andeler skal få OK-status

        kjørAlleTaskMedSenererTriggertid()
        // Verifiser utbetaling gått over rest
        verify(exactly = 1) { iverksettClient.simulerV2(any()) }
        verify(exactly = 1) { iverksettClient.iverksett(any()) }

        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        // Skrur nå på toggle for at nye behandlinger skal iverksettes over kafka
        every { unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA) } returns true

        // Legg inn andel manuelt, denne skal også sendes over rest, da første andel har blitt sendt over rest
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!
        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse +
                tilkjentYtelse.andelerTilkjentYtelse.first().copy(
                    id = UUID.randomUUID(),
                    statusIverksetting = StatusIverksetting.UBEHANDLET,
                    iverksetting = null,
                    fom = tom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    tom = tom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    utbetalingsdato = tom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                )

        tilkjentYtelseRepository.update(
            tilkjentYtelse.copy(
                andelerTilkjentYtelse = nyeAndeler,
            ),
        )

        // Kjør DagligIverksettTask som oppretter task for iverksetting av andel manuelt lagt inn over
        // Tasken feiler på onCompletion, men det går bra
        taskService.save(DagligIverksettTask.opprettTask(LocalDate.now()))
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
        verify(exactly = 2) { iverksettClient.iverksett(any()) }

        kjørAlleTaskMedSenererTriggertid()
        assertThat(tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse)
            .allMatch { it.statusIverksetting == StatusIverksetting.OK }
    }

    @Disabled
    @Test
    fun `fagsaker iverksatt gjennom rest, kjører migrering, opprettes utbetalingId på alle saker`() {
        val tilsynBarn =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                defaultTilsynBarnTestdata()
            }

        val læremidler =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.LÆREMIDLER,
            ) {
                defaultLæremidlerTestdata()
            }
        val boutgifter =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                defaultBoutgifterTestdata()
            }

        val behandlingIder = listOf(tilsynBarn, læremidler, boutgifter)
        val fagsakIder = behandlingIder.map { testoppsettService.hentSaksbehandling(it).fagsakId }

        fagsakIder.forEach {
            assertThat(fagsakUtbetalingIdService.hentUtbetalingIderForFagsakId(it)).isEmpty()
        }

        medBrukercontext(
            roller = listOf(rolleConfig.utvikler),
        ) {
            webTestClient
                .post()
                .uri("/api/forvaltning/migrer-utbetalinger")
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
        }

        fagsakIder.forEach {
            assertThat(fagsakUtbetalingIdService.hentUtbetalingIderForFagsakId(it)).isNotEmpty()
        }
    }

    private fun opprettFørstegangsbehandling(
        fom: LocalDate,
        tom: LocalDate,
    ): BehandlingId =
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.LÆREMIDLER,
        ) {
            aktivitet {
                opprett {
                    aktivitetUtdanningLæremidler(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
        }
}
