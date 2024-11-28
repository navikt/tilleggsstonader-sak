package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Executors

@Service
class MyTestService(
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
) {

    @Transactional
    fun slow(behandling: Behandling) {
        val mellomlagret = mellomlagerBrevRepository.findByBehandlingId(behandling.id)
        Thread.sleep(1000)
        mellomlagerBrevRepository.update(mellomlagret.copy(brevmal = "slow"))
        println("slow")
    }

    @Transactional
    fun fast(behandling: Behandling) {
        val mellomlagret = mellomlagerBrevRepository.findByBehandlingId(behandling.id)
        mellomlagerBrevRepository.update(mellomlagret.copy(brevmal = "fast"))
        println("fast")
    }
}

class MellomlagerBrevRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var mellomlagerBrevRepository: MellomlagerBrevRepository

    @Autowired
    lateinit var myTestService: MyTestService

    val taskExecutor = Executors.newVirtualThreadPerTaskExecutor()
    val behandling = behandling()

    @Test
    fun `med transactional og lock`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        mellomlagerBrevRepository.insert(MellomlagretBrev(behandlingId = behandling.id, brevmal = "", brevverdier = ""))
        val job1 = taskExecutor.submit {
            myTestService.slow(behandling)
        }

        Thread.sleep(100)

        val job2 = taskExecutor.submit {
            myTestService.fast(behandling)
        }

        job1.get()
        job2.get()

        println(mellomlagerBrevRepository.findByBehandlingId(behandling.id).brevmal)
    }

    @Test
    fun `uten lock`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        mellomlagerBrevRepository.insert(MellomlagretBrev(behandlingId = behandling.id, brevmal = "", brevverdier = ""))

        val job1 = taskExecutor.submit {
            val mellomlagret = mellomlagerBrevRepository.findByIdOrThrow(behandling.id)
            Thread.sleep(1000)
            mellomlagerBrevRepository.update(mellomlagret.copy(brevmal = "slow"))
            println("slow")
        }

        Thread.sleep(100)

        val job2 = taskExecutor.submit {
            val mellomlagret = mellomlagerBrevRepository.findByIdOrThrow(behandling.id)
            mellomlagerBrevRepository.update(mellomlagret.copy(brevmal = "fast"))
            println("fast")
        }

        job1.get()
        job2.get()

        println(mellomlagerBrevRepository.findByIdOrThrow(behandling.id).brevmal)
    }
}
