package no.nav.tilleggsstonader.sak.brev.mellomlager

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MellomlagringBrevServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val mellomlagerBrevRepository = mockk<MellomlagerBrevRepository>()
    private val mellomlagerFrittståendeBrevRepository = mockk<MellomlagerFrittståendeBrevRepository>()
    private val mellomlagringBrevService =
        MellomlagringBrevService(
            behandlingService = behandlingService,
            mellomlagerBrevRepository = mellomlagerBrevRepository,
            mellomlagerFrittståendeBrevRepository = mellomlagerFrittståendeBrevRepository,
        )

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
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
