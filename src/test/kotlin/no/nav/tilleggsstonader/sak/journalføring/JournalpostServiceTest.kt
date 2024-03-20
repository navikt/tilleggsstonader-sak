package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class JournalpostServiceTest() {
    private val journalpostClient = mockk<JournalpostClient>()
    private val personService = mockk<PersonService>()
    private val journalpostService = JournalpostService(journalpostClient, personService)

    @Nested
    inner class FinnJournalpostOgPersonIdent {
        val journalpostId = "123"
        val aktørId = "11111111111"
        val personIdentFraPdl = "12345678901"

        @Test
        internal fun `skal hente journalpost med personident utledet fra pdl`() {
            every {
                personService.hentPersonIdenter(aktørId)
            } returns PdlIdenter(listOf(PdlIdent(personIdentFraPdl, false)))

            every { journalpostClient.hentJournalpost(any()) } returns journalpost(journalpostId = journalpostId, bruker = Bruker(type = BrukerIdType.AKTOERID, id = aktørId))

            val (journalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)

            assertThat(personIdent).isEqualTo(personIdentFraPdl)
            assertThat(journalpost.journalpostId).isEqualTo(journalpostId)
        }

        @Test
        fun `skal kaste feil om journalpost ikke har bruker`() {
            val journalpost = journalpost(journalpostId = journalpostId, bruker = null)
            every { journalpostClient.hentJournalpost(any()) } returns journalpost

            assertThatThrownBy {
                journalpostService.finnJournalpostOgPersonIdent(journalpostId)
            }.hasMessageContaining("Kan ikke hente journalpost=$journalpostId uten bruker")
        }

        @Test
        fun `skal kaste feil om journalpost har org som bruker`() {
            val bruker = Bruker(id = UUID.randomUUID().toString(), type = BrukerIdType.ORGNR)

            val journalpost = journalpost(bruker = bruker)
            every { journalpostClient.hentJournalpost(any()) } returns journalpost

            assertThatThrownBy {
                journalpostService.finnJournalpostOgPersonIdent(journalpostId)
            }.hasMessageContaining("Kan ikke hente journalpost=$journalpostId for orgnr")
        }

        @Test
        fun `skal hente journalpost og personIdent ved BrukerIdType = FNR`() {
            val fnr = FnrGenerator.generer()
            val bruker = Bruker(id = fnr, type = BrukerIdType.FNR)

            val journalpost = journalpost(bruker = bruker)
            every { journalpostClient.hentJournalpost(any()) } returns journalpost

            val (mottatJournalpost, personIdent) = journalpostService.finnJournalpostOgPersonIdent(journalpostId)

            assertThat(mottatJournalpost).isEqualTo(journalpost)
            assertThat(personIdent).isEqualTo(fnr)
        }
    }

    @Nested
    inner class HentDokument {
        private val bruker = Bruker(id = FnrGenerator.generer(), type = BrukerIdType.FNR)
        private val dokumentInfoId = "dokumentInfoId"
        private val journalpost = journalpost(
            bruker = bruker,
            dokumenter = listOf(
                dokumentInfo(
                    dokumentInfoId = dokumentInfoId,
                    dokumentvarianter = listOf(dokumentvariant(Dokumentvariantformat.FULLVERSJON)),
                ),
            ),
        )

        @Test
        fun `skal ikke hente dokument om ikke dokument med samme id finnes i journalpost`() {
            assertThatThrownBy {
                journalpostService.hentDokument(
                    journalpost,
                    "feilId",
                )
            }.hasMessageContaining("Finner ikke dokument med id")
        }

        @Test
        fun `skal ikke hente dokument om det ikke har en dokumentvariant på format ARKIV`() {
            assertThatThrownBy {
                journalpostService.hentDokument(
                    journalpost,
                    dokumentInfoId,
                )
            }.hasMessageContaining("Vedlegget er sannsynligvis under arbeid, må åpnes i gosys")
        }
    }
}
