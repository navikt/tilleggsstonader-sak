package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException

class InsertUpdateRepositoryImplTest : IntegrationTest() {

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Test
    internal fun `skal kaste exception hvis man bruker save eller saveAll`() {
        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakRepository.save(fagsakDomain())
            },
        ).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(
            catchThrowable {
                @Suppress("DEPRECATION")
                fagsakRepository.saveAll(listOf(fagsakDomain(), fagsakDomain()))
            },
        ).isInstanceOf(DbActionExecutionException::class.java)
    }

    @Test
    internal fun `skal kaste exception hvis man oppdaterer entiteter som ikke finnes`() {
        assertThat(
            catchThrowable {
                fagsakRepository.update(fagsakDomain())
            },
        ).isInstanceOf(DbActionExecutionException::class.java)

        assertThat(
            catchThrowable {
                fagsakRepository.updateAll(listOf(fagsakDomain(), fagsakDomain()))
            },
        ).isInstanceOf(DbActionExecutionException::class.java)
    }

    // TODO kopiere andre tester

    private fun fagsakDomain() = FagsakDomain(stønadstype = Stønadstype.BARNETILSYN)
}
