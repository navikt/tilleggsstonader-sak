package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import java.util.UUID

class InsertUpdateRepositoryImplTest : IntegrationTest() {

    @Autowired
    lateinit var fagsakPersonRepository: FagsakPersonRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `skal kaste exception hvis man bruker save eller saveAll`() {
        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakPersonRepository.save(fagsakPerson())
            },
        ).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakPersonRepository.saveAll(listOf(fagsakPerson(), fagsakPerson()))
            },
        ).isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun `skal lagre entitet`() {
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = emptySet()))
        fagsakRepository.insert(fagsakDomain(fagsakPersonId = person.id))
        assertThat(fagsakRepository.count()).isEqualTo(1)
    }

    @Test
    internal fun `skal lagre entiteter`() {
        val person1 = fagsakPersonRepository.insert(FagsakPerson(identer = emptySet()))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = emptySet()))
        fagsakRepository.insertAll(
            listOf(
                fagsakDomain(fagsakPersonId = person1.id),
                fagsakDomain(fagsakPersonId = person2.id),
            ),
        )
        assertThat(fagsakRepository.count()).isEqualTo(2)
    }

    @Test
    internal fun `skal oppdatere entitet`() {
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = emptySet()))
        val person2 = fagsakPersonRepository.insert(FagsakPerson(identer = emptySet()))
        val fagsak =
            fagsakRepository.insert(fagsakDomain(stønadstype = Stønadstype.BARNETILSYN, fagsakPersonId = person.id))
        fagsakRepository.update(fagsak.copy(fagsakPersonId = person2.id))

        assertThat(fagsakRepository.count()).isEqualTo(1)
        fagsakRepository.findAll().forEach {
            assertThat(it.fagsakPersonId).isEqualTo(person2.id)
        }
    }

    /**
     * Dersom denne testen slutter å fungere og endretTid oppdateres for alle søkerIdenter ved endring av fagsak/søkerIdenter
     * må vi endre bruken av aktivIdent til å sjekke på opprettetTid - samt jukse med opprettetTid
     * dersom en gammel personIdent gjenbrukes
     */
    @Test
    internal fun `skal ikke oppdatere endretTid på barnEntiteter i collection`() {
        val personIdent = "12345"
        val nyPersonIdent = "1234"
        val annenIdent = "9"
        val person = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(personIdent))))
        Thread.sleep(200)
        val oppdatertPerson =
            fagsakPersonRepository.update(
                person.copy(
                    identer = person.identer.map { it.copy(ident = nyPersonIdent) }
                        .toSet() + PersonIdent(annenIdent),
                ),
            )
        val oppdatertSøkerIdent = oppdatertPerson.identer.first { it.ident == nyPersonIdent }
        val originalSøkerIdent = person.identer.first { it.ident == personIdent }

        assertThat(originalSøkerIdent.sporbar.endret.endretTid).isEqualTo(oppdatertSøkerIdent.sporbar.endret.endretTid)
        assertThat(originalSøkerIdent.sporbar.opprettetTid).isEqualTo(oppdatertSøkerIdent.sporbar.opprettetTid)
    }

    @Test
    internal fun `skal kaste exception hvis man oppdaterer entiteter som ikke finnes`() {
        assertThat(
            catchThrowable {
                fagsakPersonRepository.update(fagsakPerson())
            },
        ).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(
            catchThrowable {
                fagsakPersonRepository.updateAll(listOf(fagsakPerson(), fagsakPerson()))
            },
        ).isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun `insert skal være transactional`() {
        fagsakPersonRepository.insert(fagsakPerson())
        try {
            fagsakPersonRepository.insert(fagsakPerson())
        } catch (e: Exception) {
        }
        assertThat(fagsakPersonRepository.findAll()).hasSize(1)
    }

    private fun fagsakPerson(ident: String = "1") = FagsakPerson(identer = setOf(PersonIdent(ident)))

    private fun fagsakDomain(
        fagsakPersonId: UUID,
        stønadstype: no.nav.tilleggsstonader.kontrakter.felles.Stønadstype = Stønadstype.BARNETILSYN,
    ) =
        FagsakDomain(
            fagsakPersonId = fagsakPersonId,
            stønadstype = stønadstype,
        )
}
