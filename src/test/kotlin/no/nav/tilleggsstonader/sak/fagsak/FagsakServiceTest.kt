package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class FagsakServiceTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Nested
    inner class FinnFagsakerForPersonIdent {
        private val fagsakTilknyttetPesonIdent123 = fagsak(setOf(PersonIdent("123")))

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent123)
        }

        @Test
        fun `skal hente fagsaker for personid`() {
            val res = fagsakService.finnFagsakerForFagsakPersonId(fagsakTilknyttetPesonIdent123.fagsakPersonId)
            assertThat(res.barnetilsyn).isNotNull
        }

        @Test
        fun `skal ikke returnere noe om ingen fagsaker er knyttet til personid`() {
            val res = fagsakService.finnFagsakerForFagsakPersonId(UUID.randomUUID())
            assertThat(res.barnetilsyn).isNull()
        }
    }
}
