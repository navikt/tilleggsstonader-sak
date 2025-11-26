package no.nav.tilleggsstonader.sak

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskLogg
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.internal.TaskWorker
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.Behandlingsjournalpost
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagretBrev
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagretFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.Vedtaksbrev
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.MockClientService
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.RolleConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.resetMock
import no.nav.tilleggsstonader.sak.integrasjonstest.Kall
import no.nav.tilleggsstonader.sak.migrering.routing.SkjemaRouting
import no.nav.tilleggsstonader.sak.oppfølging.Oppfølging
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBehandling
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.TilbakekrevingHendelse
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingLogg
import no.nav.tilleggsstonader.sak.util.DbContainerInitializer
import no.nav.tilleggsstonader.sak.util.TestoppsettService
import no.nav.tilleggsstonader.sak.util.TokenUtil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cache.CacheManager
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [App::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    "integrasjonstest",
    "mock-arbeidsfordeling",
    "mock-arena",
    "mock-egen-ansatt",
    "mock-featuretoggle",
    "mock-familie-dokument",
    "mock-fullmakt",
    "mock-htmlify",
    "mock-iverksett",
    "mock-journalpost",
    "mock-kafka",
    "mock-klage",
    "mock-kodeverk",
    "mock-oppgave",
    "mock-pdl",
    "mock-register-aktivitet",
    "mock-ytelse-client",
    "mock-google-routes",
)
@EnableMockOAuth2Server
@AutoConfigureWebTestClient
abstract class IntegrationTest {
    @LocalServerPort
    private var port: Int? = 0

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    protected lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var rolleConfig: RolleConfig

    @Autowired
    protected lateinit var testoppsettService: TestoppsettService

    @Autowired
    protected lateinit var unleashService: UnleashService

    @Autowired
    private lateinit var cacheManagers: List<CacheManager>

    @Autowired
    lateinit var mockClientService: MockClientService

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskWorker: TaskWorker

    @Autowired
    lateinit var håndterSøknadService: HåndterSøknadService

    @Autowired
    lateinit var webTestClient: WebTestClient

    @Autowired
    lateinit var oppgaveRepository: OppgaveRepository

    lateinit var testBrukerkontekst: TestBrukerKontekst

    val kall = Kall(this)

    @BeforeEach
    fun setup() {
        testBrukerkontekst =
            TestBrukerKontekst(
                defaultBruker = "julenissen",
                defaultRoller = listOf(rolleConfig.beslutterRolle),
            )
    }

    @AfterEach
    fun tearDown() {
        clearCaches()
        mockClientService.resetAlleTilDefaults()
        resetMock(unleashService)
    }

    private fun clearCaches() {
        cacheManagers.forEach {
            it.cacheNames
                .mapNotNull { cacheName -> it.getCache(cacheName) }
                .forEach { cache -> cache.clear() }
        }
    }

    protected fun onBehalfOfToken(
        roller: List<String> = listOf(rolleConfig.beslutterRolle),
        saksbehandler: String = "julenissen",
    ): String = TokenUtil.onBehalfOfToken(mockOAuth2Server, roller, saksbehandler)

    protected fun clientCredential(
        clientId: String,
        accessAsApplication: Boolean,
    ): String = TokenUtil.clientToken(mockOAuth2Server, clientId, accessAsApplication)

    fun <T : Any> medBrukercontext(
        bruker: String = testBrukerkontekst.defaultBruker,
        roller: List<String> = testBrukerkontekst.roller,
        fn: () -> T,
    ): T {
        testBrukerkontekst.bruker = bruker
        testBrukerkontekst.roller = roller

        return fn().also { testBrukerkontekst.reset() }
    }

    fun WebTestClient.RequestHeadersSpec<*>.medOnBehalfOfToken() =
        this.headers {
            it.setBearerAuth(onBehalfOfToken(testBrukerkontekst.roller, testBrukerkontekst.bruker))
        }

    fun WebTestClient.RequestHeadersSpec<*>.medClientCredentials(
        clientId: String,
        accessAsApplication: Boolean,
    ) = this.headers {
        it.setBearerAuth(clientCredential(clientId, accessAsApplication))
    }

    data class TestBrukerKontekst(
        val defaultBruker: String,
        val defaultRoller: List<String>,
    ) {
        var bruker: String = defaultBruker
        var roller: List<String> = defaultRoller

        fun reset() {
            bruker = defaultBruker
            roller = defaultRoller
        }
    }
}
