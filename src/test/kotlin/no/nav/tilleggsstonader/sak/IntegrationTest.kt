package no.nav.tilleggsstonader.sak

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandlingsjournalpost
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.util.DbContainerInitializer
import no.nav.tilleggsstonader.sak.util.TestoppsettService
import no.nav.tilleggsstonader.sak.util.TokenUtil
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsvurdering
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate

// Slett denne når RestTemplateConfiguration er tatt i bruk?
@Configuration
class DefaultRestTemplateConfiguration {

    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder) =
        restTemplateBuilder.build()
}

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [App::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    "integrasjonstest",
    "mock-pdl",
    "mock-egen-ansatt",
    "mock-iverksett",
    "mock-oppgave",
)
@EnableMockOAuth2Server
abstract class IntegrationTest {

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected lateinit var restTemplate: RestTemplate
    protected val headers = HttpHeaders()

    @LocalServerPort
    private var port: Int? = 0

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    protected lateinit var rolleConfig: RolleConfig

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected lateinit var testoppsettService: TestoppsettService

    @AfterEach
    fun tearDown() {
        headers.clear()
        clearClientMocks()
        resetDatabase()
    }

    private fun resetDatabase() {
        listOf(
            TaskLogg::class,
            Task::class,

            Simuleringsresultat::class,
            TilkjentYtelse::class,
            Vilkårsvurdering::class,
            BehandlingBarn::class,

            SøknadBehandling::class,
            SøknadBarnetilsyn::class,

            Behandlingshistorikk::class,
            Behandlingsjournalpost::class,
            EksternBehandlingId::class,
            Behandling::class,
            EksternFagsakId::class,
            FagsakDomain::class,
            PersonIdent::class,
            FagsakPerson::class,
        ).forEach { jdbcAggregateOperations.deleteAll(it.java) }
    }

    private fun clearClientMocks() {
    }

    protected fun localhost(path: String): String {
        return "$LOCALHOST$port/$path"
    }

    protected fun onBehalfOfToken(
        role: String = rolleConfig.beslutterRolle,
        saksbehandler: String = "julenissen",
    ): String {
        return TokenUtil.onBehalfOfToken(mockOAuth2Server, role, saksbehandler)
    }

    companion object {
        private const val LOCALHOST = "http://localhost:"
    }
}
