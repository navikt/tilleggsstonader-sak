package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokdist.AdresseType
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.DistribuerVedtaksbrevService.ResultatBrevutsendelse.FeiletFordiMottakerErDødOgManglerAdresse
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Dødsfall
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.kontaktinformasjonDødsbo
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

class DistribuerVedtaksbrevServiceTest {
    private val journalpostClient = mockk<JournalpostClient>()
    private val brevmottakerVedtaksbrevRepository = mockk<BrevmottakerVedtaksbrevRepository>()
    private val personService = mockk<PersonService>()

    private val service =
        DistribuerVedtaksbrevService(
            journalpostClient = journalpostClient,
            brevmottakerVedtaksbrevRepository = brevmottakerVedtaksbrevRepository,
            personService = personService,
            transactionHandler = TransactionHandler(),
        )

    private val saksbehandling = saksbehandling()
    private val journalpostId = "journalpostId123"
    private val bestillingId = "bestillingId123"
    private val mottaker =
        BrevmottakerVedtaksbrev(
            behandlingId = saksbehandling.id,
            mottaker = mottakerPerson(ident = saksbehandling.ident),
            journalpostId = journalpostId,
            bestillingId = null,
        )

    private val utførteDistribusjoner = mutableListOf<DistribuerJournalpostRequest>()
    private val oppdaterteBrevmottakere = mutableListOf<BrevmottakerVedtaksbrev>()

    @BeforeEach
    fun setUp() {
        utførteDistribusjoner.clear()
        oppdaterteBrevmottakere.clear()

        every { brevmottakerVedtaksbrevRepository.update(capture(oppdaterteBrevmottakere)) } answers { firstArg() }
    }

    @Test
    fun `vedtaksbrev distribueres til mottaker (happy path)`() {
        every {
            journalpostClient.distribuerJournalpost(
                request = capture(utførteDistribusjoner),
            )
        } returns bestillingId

        every { personService.hentSøker(saksbehandling.ident) } returns PdlTestdataHelper.pdlSøker()

        val resultat = service.distribuerVedtaksbrev(mottaker)

        assertThat(resultat).isEqualTo(DistribuerVedtaksbrevService.ResultatBrevutsendelse.BrevDistribuert)

        assertThat(utførteDistribusjoner).hasSize(1)
        with(utførteDistribusjoner[0]) {
            assertThat(journalpostId).isEqualTo(journalpostId)
            assertThat(bestillendeFagsystem).isEqualTo(Fagsystem.TILLEGGSSTONADER)
            assertThat(dokumentProdApp).isEqualTo("TILLEGGSSTONADER-SAK")
            assertThat(distribusjonstype).isEqualTo(Distribusjonstype.VEDTAK)
            assertThat(adresse).isNull() // Ingen manuell adresse ved vanlig distribusjon
        }

        assertThat(oppdaterteBrevmottakere).hasSize(1)
        assertThat(oppdaterteBrevmottakere[0].bestillingId).isEqualTo(bestillingId)
    }

    @Test
    fun `dersom personen er død distribueres brevet til kontaktperson for dødsbo`() {
        val avdødPerson =
            PdlTestdataHelper.pdlSøker(
                dødsfall = listOf(Dødsfall(LocalDate.now().minusDays(10))),
                kontaktinformasjonForDoedsbo = listOf(kontaktinformasjonDødsbo()),
            )

        val problemDetailException =
            ProblemDetailException(
                detail = ProblemDetail.forStatus(HttpStatus.GONE),
                responseException = HttpClientErrorException.create(HttpStatus.GONE, "", HttpHeaders(), byteArrayOf(), null),
                httpStatus = HttpStatus.GONE,
            )

        // første distribusjon feiler, neste skal fullføre OK
        every {
            journalpostClient.distribuerJournalpost(
                capture(utførteDistribusjoner),
            )
        } throws problemDetailException andThen bestillingId

        every { personService.hentSøker(saksbehandling.ident) } returns avdødPerson

        val resultat = service.distribuerVedtaksbrev(mottaker)

        assertThat(resultat).isEqualTo(DistribuerVedtaksbrevService.ResultatBrevutsendelse.BrevDistribuert)

        assertThat(utførteDistribusjoner).hasSize(2)

        // Første forsøk på distribusjon skal være direkte til søkeren
        val firstRequest = utførteDistribusjoner[0]
        assertThat(firstRequest.journalpostId).isEqualTo(journalpostId)
        assertThat(firstRequest.adresse).isNull()

        // Andre forsøk på distribusjon skal være til kontakt for dødsbo
        with(utførteDistribusjoner[1]) {
            assertThat(journalpostId).isEqualTo(journalpostId)
            assertThat(adresse).isNotNull
            assertThat(adresse!!.adresseType).isEqualTo(AdresseType.norskPostadresse)
            assertThat(adresse!!.adresselinje1).isEqualTo("Dødsbogate 1")
            assertThat(adresse!!.postnummer).isEqualTo("0123")
            assertThat(adresse!!.poststed).isEqualTo("OSLO")
            assertThat(adresse!!.land).isEqualTo("NO")
        }

        assertThat(oppdaterteBrevmottakere).hasSize(1)
        assertThat(oppdaterteBrevmottakere[0].bestillingId).isEqualTo(bestillingId)
    }

    @Test
    fun `dersom personen er død og mangler kontaktinfo for dødsbo returneres feilmelding og det gjøres ikke nytt distribusjonskall`() {
        val avdødPersonUtenKontaktinfoForDødsbo =
            PdlTestdataHelper.pdlSøker(
                dødsfall = listOf(Dødsfall(LocalDate.now().minusDays(10))),
                kontaktinformasjonForDoedsbo = emptyList(),
            )

        val problemDetailException =
            ProblemDetailException(
                detail = ProblemDetail.forStatus(HttpStatus.GONE),
                responseException = HttpClientErrorException.create(HttpStatus.GONE, "", HttpHeaders(), byteArrayOf(), null),
                httpStatus = HttpStatus.GONE,
            )

        every {
            journalpostClient.distribuerJournalpost(
                capture(utførteDistribusjoner),
            )
        } throws problemDetailException

        every { personService.hentSøker(saksbehandling.ident) } returns avdødPersonUtenKontaktinfoForDødsbo

        val resultat = service.distribuerVedtaksbrev(mottaker)

        assertThat(resultat).isInstanceOf(FeiletFordiMottakerErDødOgManglerAdresse::class.java)
        val feilResultat = resultat as FeiletFordiMottakerErDødOgManglerAdresse
        assertThat(feilResultat.feilmelding).contains("fant heller ikke kontaktinformasjon for dødsboet")

        assertThat(utførteDistribusjoner).hasSize(1)
        val request = utførteDistribusjoner[0]
        assertThat(request.journalpostId).isEqualTo(journalpostId)
        assertThat(request.adresse).isNull()

        assertThat(oppdaterteBrevmottakere).isEmpty()
    }
}
