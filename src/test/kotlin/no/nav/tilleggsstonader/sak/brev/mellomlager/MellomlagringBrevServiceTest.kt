package no.nav.tilleggsstonader.sak.brev.mellomlager

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.AdvisoryLockService
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MellomlagringBrevServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val advisoryLockService = mockk<AdvisoryLockService>()
    private val mellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    private val mellomlagerFrittståendeBrevRepository = mockk<MellomlagerFrittståendeBrevRepository>()
    private val mellomlagringBrevService =
        MellomlagringBrevService(
            behandlingService = behandlingService,
            mellomlagerBrevRepository = mellomlagerBrevRepository,
            mellomlagerFrittståendeBrevRepository = mellomlagerFrittståendeBrevRepository,
            advisoryLockService = advisoryLockService,
        )

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
        every { SikkerhetContext.hentSaksbehandlerEllerSystembruker() } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(behandlingService, advisoryLockService, mellomlagerBrevRepository, mellomlagerFrittståendeBrevRepository)
        every { behandlingService.behandlingErLåstForVidereRedigering(any()) } returns false
        every { advisoryLockService.lockForTransaction(any(), any<() -> MellomlagretBrev>()) } answers {
            secondArg<() -> MellomlagretBrev>().invoke()
        }
    }

    @Test
    fun `mellomlagreBrev skal insert når brev ikke finnes`() {
        val nyttBrev = slot<MellomlagretBrev>()
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns null
        every { mellomlagerBrevRepository.insert(capture(nyttBrev)) } answers { nyttBrev.captured }

        val resultat = mellomlagringBrevService.mellomlagreBrev(behandlingId, brevverdier, brevmal)

        assertThat(resultat).isEqualTo(behandlingId)
        assertThat(nyttBrev.captured.behandlingId).isEqualTo(behandlingId)
        assertThat(nyttBrev.captured.brevverdier).isEqualTo(brevverdier)
        assertThat(nyttBrev.captured.brevmal).isEqualTo(brevmal)
        verify(exactly = 1) {
            advisoryLockService.lockForTransaction(behandlingId, any<() -> MellomlagretBrev>())
        }
    }

    @Test
    fun `mellomlagreBrev skal update når brev finnes`() {
        val oppdatertBrev = slot<MellomlagretBrev>()
        val eksisterendeBrev = MellomlagretBrev(behandlingId, "gammelVerdi", "gammelMal")
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns eksisterendeBrev
        every { mellomlagerBrevRepository.update(capture(oppdatertBrev)) } answers { oppdatertBrev.captured }

        val resultat = mellomlagringBrevService.mellomlagreBrev(behandlingId, brevverdier, brevmal)

        assertThat(resultat).isEqualTo(behandlingId)
        assertThat(oppdatertBrev.captured).isEqualTo(
            eksisterendeBrev.copy(
                brevverdier = brevverdier,
                brevmal = brevmal,
            ),
        )
        verify(exactly = 1) {
            advisoryLockService.lockForTransaction(behandlingId, any<() -> MellomlagretBrev>())
        }
    }

    @Test
    fun `hentOgValiderMellomlagretBrev skal returnere mellomlagret brev`() {
        every { mellomlagerBrevRepository.findByIdOrNull(behandlingId) } returns mellomlagretBrev

        assertThat(
            mellomlagringBrevService.hentMellomlagretBrev(
                behandlingId,
            ),
        ).isEqualTo(
            MellomlagreBrevDto(
                brevmal = mellomlagretBrev.brevmal,
                brevverdier = mellomlagretBrev.brevverdier,
            ),
        )
    }

    @Test
    fun `hentMellomlagretFrittståendeSanityBrev skal returnere mellomlagret frittstående brev`() {
        val fagsakId = FagsakId.random()

        val brev =
            MellomlagretFrittståendeBrev(
                fagsakId = fagsakId,
                brevverdier = mellomlagretBrev.brevverdier,
                brevmal = mellomlagretBrev.brevmal,
            )

        every {
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(fagsakId, any())
        } returns brev

        assertThat(mellomlagringBrevService.hentMellomlagretFrittståendeSanitybrev(fagsakId)).isEqualTo(
            MellomlagreBrevDto(
                brevmal = mellomlagretBrev.brevmal,
                brevverdier = mellomlagretBrev.brevverdier,
            ),
        )
    }

    private val behandlingId = BehandlingId.random()
    private val brevmal = "testMal"
    private val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal)
}
