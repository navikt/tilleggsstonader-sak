package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class SøknadRoutingRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var søknadRoutingRepository: SøknadRoutingRepository

    private val ident = "1"

    private val stønadstype = Stønadstype.BARNETILSYN

    private val routing = SøknadRouting(
        ident = ident,
        type = stønadstype,
        detaljer = JsonWrapper(objectMapper.writeValueAsString("""{"noe": true}""")),
    )

    @Test
    fun `skal lagre og hente søknadrouting`() {
        søknadRoutingRepository.insert(routing)
        val routingDb = søknadRoutingRepository.findByIdOrThrow(routing.id)
        assertThat(routing).isEqualTo(routingDb)
    }

    @Nested
    inner class FindByIdentAndType {

        @Test
        fun `skal ikke finne noe hvis det ikke finnes noe i databasen`() {
            assertThat(søknadRoutingRepository.findByIdentAndType("annenIdent", stønadstype)).isNull()
        }

        @Test
        fun `skal finne routing for gitt person og type`() {
            søknadRoutingRepository.insert(routing)
            assertThat(søknadRoutingRepository.findByIdentAndType(ident, stønadstype)).isEqualTo(routing)
            assertThat(søknadRoutingRepository.findByIdentAndType("annenIdent", stønadstype)).isNull()
        }
    }

    @Nested
    inner class CountByType {

        @Test
        fun `skal returnere antall routing av gitt type`() {
            assertThat(søknadRoutingRepository.countByType(stønadstype)).isEqualTo(0)

            søknadRoutingRepository.insert(routing)
            assertThat(søknadRoutingRepository.countByType(stønadstype)).isEqualTo(1)

            søknadRoutingRepository.insert(routing.copy(id = UUID.randomUUID(), ident = "annenIdent"))
            assertThat(søknadRoutingRepository.countByType(stønadstype)).isEqualTo(2)
        }
    }
}
