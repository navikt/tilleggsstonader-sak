package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakServiceTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

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
            val res = fagsakService.finnFagsakerForFagsakPersonId(FagsakPersonId.random())
            assertThat(res.barnetilsyn).isNull()
        }

        @Nested
        inner class SettUtbetalPåNyttFagområde {
            private val fagsak = fagsak(setOf(PersonIdent("456")))

            @BeforeEach
            fun setUp() {
                testoppsettService.lagreFagsak(fagsak)
            }

            @Test
            fun `skal kunne sette utbetalPåNyttFagområde når feltet ikke er satt`() {
                val oppdatertVerdi = fagsakService.settUtbetalPåNyttFagområde(fagsak.id, true)

                assertThat(oppdatertVerdi).isTrue
                assertThat(fagsakRepository.findByIdOrThrow(fagsak.id).utbetalPåNyttFagområde).isTrue
            }

            @Test
            fun `skal feile om utbetalPåNyttFagområde allerede er satt`() {
                fagsakService.settUtbetalPåNyttFagområde(fagsak.id, true)

                assertThatThrownBy { fagsakService.settUtbetalPåNyttFagområde(fagsak.id, false) }
                    .isInstanceOf(Feil::class.java)
                    .hasMessageContaining("utbetalPåNyttFagområde er allerede satt")
            }
        }
    }
}
