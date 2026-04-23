package no.nav.tilleggsstonader.sak.brev.frittstående

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.brev.DistribuerBrevService
import no.nav.tilleggsstonader.sak.brev.ResultatDistribusjon
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.client.HttpClientErrorException

class DistribuerFrittståendeBrevServiceTest {
    private val journalpostClient = mockk<JournalpostClient>()
    private val brevmottakerFrittståendeBrevRepository = mockk<BrevmottakerFrittståendeBrevRepository>()

    private val service =
        DistribuerFrittståendeBrevService(
            brevmottakerFrittståendeBrevRepository = brevmottakerFrittståendeBrevRepository,
            transactionHandler = TransactionHandler(),
            distribuerBrevService = DistribuerBrevService(journalpostClient),
        )

    private val journalpostId = "journalpostId123"
    private val bestillingId = "bestillingId123"
    private val mottaker =
        BrevmottakerFrittståendeBrev(
            fagsakId = FagsakId.random(),
            mottaker = mottakerPerson(ident = "12345678901"),
            journalpostId = journalpostId,
            bestillingId = null,
        )

    private val utførteDistribusjoner = mutableListOf<DistribuerJournalpostRequest>()
    private val oppdaterteBrevmottakere = mutableListOf<BrevmottakerFrittståendeBrev>()

    @BeforeEach
    fun setUp() {
        utførteDistribusjoner.clear()
        oppdaterteBrevmottakere.clear()

        every { brevmottakerFrittståendeBrevRepository.update(capture(oppdaterteBrevmottakere)) } answers { firstArg() }
    }

    @Test
    fun `skal distribuere frittstående brev og returnere BrevDistribuert`() {
        every {
            journalpostClient.distribuerJournalpost(capture(utførteDistribusjoner))
        } returns bestillingId

        val resultat = service.distribuerBrev(mottaker)

        assertThat(resultat).isEqualTo(ResultatDistribusjon.Distribuert)

        assertThat(utførteDistribusjoner).hasSize(1)
        with(utførteDistribusjoner[0]) {
            assertThat(this.journalpostId).isEqualTo(journalpostId)
            assertThat(bestillendeFagsystem).isEqualTo(Fagsystem.TILLEGGSSTONADER)
            assertThat(dokumentProdApp).isEqualTo("TILLEGGSSTONADER-SAK")
            assertThat(distribusjonstype).isEqualTo(Distribusjonstype.VIKTIG)
        }

        assertThat(oppdaterteBrevmottakere).hasSize(1)
        assertThat(oppdaterteBrevmottakere[0].bestillingId).isEqualTo(bestillingId)
    }

    @Test
    fun `skal returnere FeiletFordiMottakerErDødOgManglerAdresse ved 410 Gone`() {
        every {
            journalpostClient.distribuerJournalpost(any())
        } throws HttpStatus.GONE.problemDetailException()

        val resultat = service.distribuerBrev(mottaker)

        assertThat(resultat).isInstanceOf(
            ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse::class.java,
        )
    }

    @Test
    fun `skal kaste videre andre ProblemDetailException-feil`() {
        every {
            journalpostClient.distribuerJournalpost(any())
        } throws HttpStatus.BAD_REQUEST.problemDetailException()

        assertThatThrownBy { service.distribuerBrev(mottaker) }
            .isInstanceOf(ProblemDetailException::class.java)
    }

    private fun HttpStatus.problemDetailException() =
        ProblemDetailException(
            detail = ProblemDetail.forStatus(this),
            responseException = HttpClientErrorException.create(this, "", HttpHeaders(), byteArrayOf(), null),
            httpStatus = this,
        )
}
