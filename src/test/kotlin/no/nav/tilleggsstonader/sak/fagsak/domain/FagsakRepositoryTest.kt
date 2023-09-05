package no.nav.tilleggsstonader.sak.fagsak.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FagsakRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `skal kunne lagre og hente fagsak`() {
        val fagsak = fagsakRepository.insert(FagsakDomain(stønadstype = Stønadstype.BARNETILSYN))

        val hentetFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        assertThat(hentetFagsak).isEqualTo(fagsak)
    }
}
