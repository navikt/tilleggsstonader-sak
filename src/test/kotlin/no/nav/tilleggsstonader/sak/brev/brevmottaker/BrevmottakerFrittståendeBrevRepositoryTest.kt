package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import java.time.temporal.ChronoUnit

class BrevmottakerFrittståendeBrevRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var repository: BrevmottakerFrittståendeBrevRepository

    @Autowired
    private lateinit var frittståendeBrevRepository: FrittståendeBrevRepository

    val fagsak = fagsak()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    fun `lagre og hent brevmottaker`() {
        val frittståendeBrev = lagFrittståendeBrev()
        val brevmottaker = BrevmottakerFrittståendeBrev(
            fagsakId = fagsak.id,
            mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
            journalpostId = "123",
            brevId = frittståendeBrev.id,
            bestillingId = "bestillingId",
        )

        repository.insert(brevmottaker)

        val journalpostResultatFraDb = repository.findByIdOrThrow(brevmottaker.id)

        assertThat(journalpostResultatFraDb)
            .usingRecursiveComparison()
            .ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottaker)
        assertThat(journalpostResultatFraDb.sporbar.opprettetTid)
            .isCloseTo(brevmottaker.sporbar.opprettetTid, Assertions.within(2, ChronoUnit.SECONDS))
        assertThat(journalpostResultatFraDb.sporbar.endret.endretTid)
            .isCloseTo(brevmottaker.sporbar.endret.endretTid, Assertions.within(2, ChronoUnit.SECONDS))
    }

    @Test
    fun `skal ikke kunne lagre to journalpostResultat på samme fagsak med samme mottaker`() {
        val brevmottaker1 = BrevmottakerFrittståendeBrev(
            fagsakId = fagsak.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
        )

        val brevmottaker2 = BrevmottakerFrittståendeBrev(
            fagsakId = fagsak.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
        )

        repository.insert(brevmottaker1)
        assertThatThrownBy {
            repository.insert(brevmottaker2)
        }.hasCauseInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    fun `skal kunne lagre to journalpostResultat på samme fagsak med forskjellige mottakere`() {
        val brevmottaker = BrevmottakerFrittståendeBrev(
            fagsakId = fagsak.id,
            mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
        )

        val brevmottakerAnnenMottaker = BrevmottakerFrittståendeBrev(
            fagsakId = fagsak.id,
            mottaker = mottakerPerson(
                ident = "ident",
                mottakerRolle = MottakerRolle.VERGE,
            ),
        )

        repository.insert(brevmottaker)
        repository.insert(brevmottakerAnnenMottaker)

        val hentetResultat = repository.findByIdOrNull(brevmottaker.id)
        val hentetResultatAnnenMottaker = repository.findByIdOrNull(brevmottakerAnnenMottaker.id)

        assertThat(hentetResultat).isNotNull
        assertThat(hentetResultat).usingRecursiveComparison()
            .ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottaker)

        assertThat(hentetResultat).isNotNull
        assertThat(hentetResultatAnnenMottaker).usingRecursiveComparison()
            .ignoringFields("opprettetTid", "sporbar.endret.endretTid")
            .isEqualTo(brevmottakerAnnenMottaker)
    }

    @Nested
    inner class ExistsByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull {

        @Test
        fun `skal ikke finne brev når brevId er satt`() {
            val frittståendeBrev = lagFrittståendeBrev()
            val brev = repository.insert(
                BrevmottakerFrittståendeBrev(
                    fagsakId = fagsak.id,
                    mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
                    brevId = frittståendeBrev.id,
                ),
            )
            val exists =
                repository.existsByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(fagsak.id, brev.sporbar.opprettetAv)
            assertThat(exists).isFalse
        }
    }

    @Nested
    inner class FindByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull {

        @Test
        fun `skal ikke finne brev når brevId er satt`() {
            val frittståendeBrev = lagFrittståendeBrev()
            val brev = repository.insert(
                BrevmottakerFrittståendeBrev(
                    fagsakId = fagsak.id,
                    mottaker = mottakerPerson(ident = fagsak.hentAktivIdent()),
                    brevId = frittståendeBrev.id,
                ),
            )
            val brevmottakere =
                repository.findByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(fagsak.id, brev.sporbar.opprettetAv)
            assertThat(brevmottakere).isEmpty()
        }
    }

    private fun lagFrittståendeBrev() = frittståendeBrevRepository.insert(
        FrittståendeBrev(
            fagsakId = fagsak.id,
            pdf = Fil("123".toByteArray()),
            tittel = "",
            saksbehandlerIdent = "id",
        ),
    )
}
