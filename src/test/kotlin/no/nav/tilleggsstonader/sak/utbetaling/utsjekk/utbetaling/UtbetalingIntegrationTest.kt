package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UtbetalingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdService: FagsakUtbetalingIdService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var iverksettService: IverksettService

    @Autowired
    lateinit var simuleringClient: SimuleringClient

    @Autowired
    lateinit var utbetalingStatusHåndterer: UtbetalingStatusHåndterer

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `behandle og opphør hele saken, skal sende utbetaling til helved uten perioder for å opphøre`() {
        val fom = 1 september 2025
        val tom = 30 september 2025

        val førstegangsbehandlingId =
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

        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)
        val finnesUtbetalingIdEtterFørstegangsbehandling =
            fagsakUtbetalingIdService.finnesUtbetalingsId(førstegangsbehandling.fagsakId, TypeAndel.LÆREMIDLER_AAP)

        Assertions.assertThat(finnesUtbetalingIdEtterFørstegangsbehandling).isTrue
        verify(exactly = 1) { simuleringClient.simuler(any()) }
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
                        årsak = `BehandlingÅrsak`.SØKNAD,
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

        Assertions.assertThat(finnesUtbetalingIdEtterRevurdering).isTrue
        verify(exactly = 2) { simuleringClient.simuler(any()) }
        val opphørUtbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .maxBy { it.vedtakstidspunkt }

        Assertions.assertThat(opphørUtbetaling.utbetalinger.size).isEqualTo(1)
        Assertions.assertThat(opphørUtbetaling.utbetalinger.single().perioder).isEmpty()

        val andelerRevurdering = tilkjentYtelseRepository.findByBehandlingId(revurderingId)!!.andelerTilkjentYtelse
        Assertions.assertThat(andelerRevurdering).hasSize(1)
        Assertions.assertThat(andelerRevurdering.single().erNullandel()).isTrue
    }

    @Test
    fun `utbetalingsperioder sendt til helved skal matche periode i andelene`() {
        val fom = 9 august 2025
        val tom = 9 desember 2025

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                defaultTilsynBarnTestdata(fom, tom)
            }

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandlingId)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }

        val sendtUtbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        assertThat(sendtUtbetaling.utbetalinger).hasSize(1)
        assertThat(sendtUtbetaling.utbetalinger.single().perioder).hasSize(andeler.size)

        val sortertePerioderSendtTilØkonomi =
            sendtUtbetaling.utbetalinger
                .single()
                .perioder
                .sortedBy { it.fom }
        andeler.forEachIndexed { index, andelTilkjentYtelse ->
            assertThat(andelTilkjentYtelse.fom).isEqualTo(sortertePerioderSendtTilØkonomi[index].fom)
            assertThat(andelTilkjentYtelse.tom).isEqualTo(sortertePerioderSendtTilØkonomi[index].tom)
        }
    }
}
