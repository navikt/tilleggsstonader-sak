package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AdvisoryLockServiceTest : IntegrationTest() {
    @Autowired
    lateinit var advisoryLockService: AdvisoryLockService

    @Autowired
    lateinit var transactionHandler: TransactionHandler

    @Test
    fun `verifiserer at future2 må vente til future1 frigitt låsen`() {
        val behandlingId = BehandlingId.random()
        val resultList = mutableListOf<String>()

        val future1 =
            CompletableFuture.supplyAsync {
                resultList.add("Thread 1 before lock")
                transactionHandler.runInNewTransaction {
                    advisoryLockService.lockForTransaction(behandlingId) {
                        resultList.add("Thread 1 acquired lock")
                        Thread.sleep(1000)
                        // Det forventes at future2 ikke legger til noe til resultList mens første tråd har Sleep,
                        // men at future2 venter til låsen er frigitt.
                        resultList.add("Thread 1 done with lock")
                    }
                }
            }
        Thread.sleep(100)

        val future2 =
            CompletableFuture.supplyAsync {
                resultList.add("Thread 2 before lock")
                transactionHandler.runInNewTransaction {
                    advisoryLockService.lockForTransaction(behandlingId) {
                        resultList.add("Thread 2 acquired lock")
                    }
                }
            }

        CompletableFuture.allOf(future1, future2).get(5, TimeUnit.SECONDS)

        assertThat(resultList).containsExactly(
            "Thread 1 before lock",
            "Thread 1 acquired lock",
            "Thread 2 before lock",
            "Thread 1 done with lock",
            "Thread 2 acquired lock",
        )
    }
}
