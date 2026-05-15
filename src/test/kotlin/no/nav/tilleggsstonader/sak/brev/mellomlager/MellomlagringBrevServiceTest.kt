package no.nav.tilleggsstonader.sak.brev.mellomlager

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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

    @Test
    fun `slettMellomlagretFrittståendeBrev skal slette eksisterende brev`() {
        val fagsakId = FagsakId.random()
        val brev = MellomlagretFrittståendeBrev(fagsakId = fagsakId, brevverdier = brevverdier, brevmal = brevmal)
        every {
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(fagsakId, "bob")
        } returns brev
        every { mellomlagerFrittståendeBrevRepository.deleteById(brev.id) } returns Unit

        mellomlagringBrevService.slettMellomlagretFrittståendeBrev(fagsakId, "bob")

        verify(exactly = 1) {
            mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(fagsakId, "bob")
        }
        verify(exactly = 1) {
            mellomlagerFrittståendeBrevRepository.deleteById(brev.id)
        }
    }

    private val behandlingId = BehandlingId.random()
    private val brevmal = "testMal"
    private val brevverdier = "{}"
    private val mellomlagretBrev = MellomlagretBrev(behandlingId, brevverdier, brevmal)
}
