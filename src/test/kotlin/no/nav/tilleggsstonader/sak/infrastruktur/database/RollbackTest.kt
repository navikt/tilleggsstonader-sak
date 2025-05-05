package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

/**
 * Tester att applikasjonen har satt opp rollback av transaksjoner riktig.
 * @EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
 * Hvis ikke rulles kun unchecked exceptions tilbake.
 */
class RollbackTest : IntegrationTest() {
    @Autowired
    private lateinit var transactionHandler: TransactionHandler

    @Autowired
    private lateinit var taskService: TaskService

    @AfterEach
    override fun tearDown() {
        transactionHandler.runInNewTransaction {
            taskService.deleteAll(taskService.findAll())
        }
        super.tearDown()
    }

    @Test
    fun `happy case`() {
        transactionHandler.runInNewTransaction {
            taskService.save(Task(type = "test", payload = "{}"))
        }
        transactionHandler.runInNewTransaction {
            assertThat(taskService.findAll()).hasSize(1)
        }
    }

    @Test
    fun `skal rulle tilbake når checked exception IOException kastes`() {
        val exception =
            try {
                transactionHandler.runInNewTransaction {
                    taskService.save(Task(type = "test", payload = "{}"))
                    throw IOException("yolo")
                }
            } catch (e: Exception) {
                e
            }

        transactionHandler.runInNewTransaction {
            assertThat(taskService.findAll()).hasSize(0)
        }
        assertThat(exception).isInstanceOf(IOException::class.java)
    }

    /**
     * Kontrollerer at den ikke er konfigurert med checked exceptions.
     * Men at EnableTransactionManagement er satt opp riktig
     */
    @Test
    fun `kontroller at runInNewTransaction kun har rollback for RuntimeException`() {
        val annotation =
            TransactionHandler::class
                .declaredFunctions
                .single { it.name == "runInNewTransaction" }
                .findAnnotation<Transactional>()

        assertThat(annotation?.rollbackFor).isEmpty()
    }
}
