package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.totrinnskontroll
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.CompletableFuture

class IverksettPåKafkaTest(
    @Autowired private val iverksettService: IverksettService,
    @Autowired private val kafkaTemplate: KafkaTemplate<String, String>,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : IntegrationTest() {
    val sendteUtbetalinger = mutableListOf<ProducerRecord<String, String>>()

    @BeforeEach
    fun setUp() {
        every { kafkaTemplate.send(capture(sendteUtbetalinger)) } returns CompletableFuture.completedFuture(mockk())
    }

    @Test
    fun `iverksetter førstegangsbehandling daglig reise første gang, ingen andeler skal utbetales, sender ingenting på kafka`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO))
        val behandling = testoppsettService.lagre(behandling(fagsak, resultat = BehandlingResultat.INNVILGET))
        testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))

        val andelTilkjentYtelse =
            andelTilkjentYtelse(
                type = TypeAndel.DAGLIG_REISE_AAP,
                utbetalingsdato = LocalDate.now().plusDays(1),
                fom = LocalDate.now().plusDays(1),
                tom = LocalDate.now().plusDays(1),
                satstype = Satstype.ENGANGSBELØP,
                kildeBehandlingId = behandling.id,
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andelTilkjentYtelse))

        iverksettService.iverksettBehandlingFørsteGang(behandling.id)

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
        // Ingen andeler skal være iverksatt
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).noneMatch { it.iverksetting != null }

        assertThat(sendteUtbetalinger).hasSize(0)
    }

    @Test
    fun `iverksetter førstegangsbehandling daglig reise første gang, to andeler forrige måned, sender én utbetaling med én periode`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO))
        val behandling = testoppsettService.lagre(behandling(fagsak, resultat = BehandlingResultat.INNVILGET))
        testoppsettService.lagreTotrinnskontroll(totrinnskontroll(behandling.id))

        val forrigeMåned = YearMonth.now().minusMonths(1)
        val andelTilkjentYtelse1 =
            andelTilkjentYtelse(
                type = TypeAndel.DAGLIG_REISE_AAP,
                utbetalingsdato = forrigeMåned.atDay(1),
                fom = forrigeMåned.atDay(1),
                tom = forrigeMåned.atDay(1),
                satstype = Satstype.ENGANGSBELØP,
                kildeBehandlingId = behandling.id,
            )

        val andelTilkjentYtelse2 =
            andelTilkjentYtelse(
                type = TypeAndel.DAGLIG_REISE_AAP,
                utbetalingsdato = forrigeMåned.atDay(10),
                fom = forrigeMåned.atDay(10),
                tom = forrigeMåned.atDay(10),
                satstype = Satstype.ENGANGSBELØP,
                kildeBehandlingId = behandling.id,
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andelTilkjentYtelse1, andelTilkjentYtelse2))

        iverksettService.iverksettBehandlingFørsteGang(behandling.id)
        val iverksettingId = behandling.id.id

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
        // Alle andeler skal være iverksatt
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).allMatch { it.iverksetting?.iverksettingId == iverksettingId }

        assertThat(sendteUtbetalinger).hasSize(1)
        val sendtUtbetaling = sendteUtbetalinger.single()

        assertThat(sendtUtbetaling.key()).isEqualTo(iverksettingId.toString())

        with(sendtUtbetaling.utbetalingRecord().utbetalingsgrunnlag) {
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().fom).isEqualTo(forrigeMåned.atDay(1))
            assertThat(perioder.single().tom).isEqualTo(forrigeMåned.atDay(1))
            assertThat(perioder.single().beløp.toInt()).isEqualTo(andelTilkjentYtelse1.beløp + andelTilkjentYtelse2.beløp)
        }
    }

    private fun ProducerRecord<String, String>.utbetalingRecord() = objectMapper.readValue<IverksettingDto>(value())
}
