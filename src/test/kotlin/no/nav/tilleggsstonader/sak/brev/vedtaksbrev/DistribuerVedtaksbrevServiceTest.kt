package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DistribuerVedtaksbrevServiceTest {
    private val journalpostClient = mockk<JournalpostClient>()
    private val brevmottakerVedtaksbrevRepository = mockk<BrevmottakerVedtaksbrevRepository>()
    private val personService = mockk<PersonService>()

    private val service =
        DistribuerVedtaksbrevService(
            journalpostClient = journalpostClient,
            brevmottakerVedtaksbrevRepository = brevmottakerVedtaksbrevRepository,
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
    fun `vedtaksbrev distribueres til mottaker`() {
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
}
