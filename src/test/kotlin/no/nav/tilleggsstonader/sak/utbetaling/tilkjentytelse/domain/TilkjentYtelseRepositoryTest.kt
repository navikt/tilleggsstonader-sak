package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class TilkjentYtelseRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var repository: TilkjentYtelseRepository

    @Autowired
    private lateinit var transactionHandler: TransactionHandler

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = tilkjentYtelse(behandling.id)
        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Skal kunne oppdatere tilkjent ytelse med nye andeler`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = repository.insert(tilkjentYtelse(behandling.id))

        repository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = setOf()))
        val tilkjentYtelseUtenAndeler = repository.findByBehandlingId(behandling.id)!!
        assertThat(tilkjentYtelseUtenAndeler.andelerTilkjentYtelse).isEmpty()
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andeler = arrayOf(andelTilkjentYtelse(behandling.id), andelTilkjentYtelse(behandling.id))
        val tilkjentYtelse = tilkjentYtelse(behandling.id, *andeler)

        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Finn tilkjent ytelse på behandlingId`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = tilkjentYtelse(behandling.id)
        val lagretTilkjentYtelse = repository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse = repository.findByBehandlingId(behandling.id)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }

    @Test
    fun `skal låse tilkjent ytelse for oppdatering sånn at den ikke kan bli plukket opp av flere tråder samtidig`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        repository.insert(tilkjentYtelse(behandling.id, andeler = emptyArray()))
        val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

        val latch = CountDownLatch(1)
        val beløpJob1 = 1
        val beløpJob2 = 2

        val job1 = executor.submit {
            transactionHandler.runInNewTransaction {
                val tilkjentYtelse = repository.findByBehandlingIdForUpdate(behandling.id)!!
                latch.countDown() // sier ifra at job1 startet
                Thread.sleep(500)
                val andel = andelTilkjentYtelse(kildeBehandlingId = behandling.id, beløp = beløpJob1)
                repository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = setOf(andel)))
            }
        }
        val job2 = executor.submit {
            latch.await() // venter på at job1 har startet
            transactionHandler.runInNewTransaction {
                val tilkjentYtelse = repository.findByBehandlingIdForUpdate(behandling.id)!!
                val andel = andelTilkjentYtelse(kildeBehandlingId = behandling.id, beløp = beløpJob2)
                repository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = setOf(andel)))
            }
        }

        job1.get()
        job2.get()

        val tilkjentYtelse = repository.findByBehandlingId(behandling.id)!!
        val beløp = tilkjentYtelse.andelerTilkjentYtelse.single().beløp

        assertThat(beløp).isEqualTo(beløpJob2)
    }
}
