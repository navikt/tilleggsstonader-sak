package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.clearAllMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockGetVariant
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.sjekkRoutingForPerson
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.kildeResultatAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SøknadRoutingIntegrationTest(
    @Autowired private val ytelseClient: YtelseClient,
    @Autowired private val arenaClient: ArenaClient,
    @Autowired private val pdlClient: PdlClient,
) : IntegrationTest() {
    val jonas = "12345678910"
    val erna = "10987654321"

    @ParameterizedTest
    @EnumSource(
        value = Søknadstype::class,
        names = ["BARNETILSYN", "LÆREMIDLER", "BOUTGIFTER"],
    )
    fun `visse stønadstyper skal alltid routes til ny løsning`(søknadstype: Søknadstype) {
        val routingSjekk = sjekkRoutingForPerson(SøknadRoutingDto(jonas, søknadstype))

        assertTrue(routingSjekk.skalBehandlesINyLøsning)
    }

    @Nested
    inner class DagligReise {
        @BeforeEach
        fun beforeEach() {
            // Trengs fordi arenaService.hentStatus først henter identer fra PDL
            mockHentIdenterFraPdl()
        }

        @AfterEach
        fun afterEach() {
            clearAllMocks()
        }

        val dagligReiseFagsak = fagsak(identer = setOf(PersonIdent(jonas)), stønadstype = Stønadstype.DAGLIG_REISE_TSO)
        val dagligReiseBehandling = behandling(dagligReiseFagsak)

        @Test
        fun `skal alltid svare ja hvis personen har blitt routet til ny løsning tidligere`() {
            testoppsettService.lagreSøknadRouting(søknadRoutingDagligReise())

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())

            assertTrue(routingSjekk.skalBehandlesINyLøsning)
        }

        @Test
        fun `skal lagre routing og svare true hvis det finnes en behandling på personen for gitt stønad`() {
            testoppsettService.lagreFagsak(dagligReiseFagsak)
            testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())

            assertTrue(routingSjekk.skalBehandlesINyLøsning)
            assertTrue(routingHarBlittLagret())
        }

        @Test
        fun `skal route til gammel løsning hvis person har aktivt vedtak i Arena på daglig reise`() {
            mockMaksAntallSomSkalRoutesPåDagligReise(maksAntall = 10)
            mockBrukerHarDagligReiseVedtakIArena(erAktivt = true)

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())

            assertFalse(routingSjekk.skalBehandlesINyLøsning)
        }

        @Test
        fun `skal svare nei hvis feature toggle sier at ingen skal slippe gjennom`() {
            mockMaksAntallSomSkalRoutesPåDagligReise(0)

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())

            assertFalse(routingSjekk.skalBehandlesINyLøsning)
        }

        @Test
        fun `brukere med aktiv AAP skal bli routet til ny løsning`() {
            mockMaksAntallSomSkalRoutesPåDagligReise(maksAntall = 10)
            mockBrukerHarDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())
            assertTrue(routingSjekk.skalBehandlesINyLøsning)
        }

        @Test
        fun `brukere uten aktiv AAP skal routet til gammel løsning`() {
            mockMaksAntallSomSkalRoutesPåDagligReise(maksAntall = 10)
            mockBrukerHarDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = false)

            val routingSjekk = sjekkRoutingForPerson(dagligReiseRoutingRequest())
            assertFalse(routingSjekk.skalBehandlesINyLøsning)
        }

        @Test
        fun `skal slippe gjennom til ny løsning bare til maks antall er nådd`() {
            mockMaksAntallSomSkalRoutesPåDagligReise(maksAntall = 1)
            mockBrukerHarDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekkFørsteRouting = sjekkRoutingForPerson(SøknadRoutingDto(jonas, Søknadstype.DAGLIG_REISE))
            val routingSjekkAndreRouting = sjekkRoutingForPerson(SøknadRoutingDto(erna, Søknadstype.DAGLIG_REISE))

            assertTrue(routingSjekkFørsteRouting.skalBehandlesINyLøsning)
            assertFalse(routingSjekkAndreRouting.skalBehandlesINyLøsning)
        }

        private fun søknadRoutingDagligReise() =
            SøknadRouting(
                ident = jonas,
                type = Søknadstype.DAGLIG_REISE,
                detaljer = JsonWrapper("{}"),
            )

        fun dagligReiseRoutingRequest() =
            SøknadRoutingDto(
                ident = jonas,
                søknadstype = Søknadstype.DAGLIG_REISE,
            )

        private fun routingHarBlittLagret() = testoppsettService.hentSøknadRouting(jonas, Søknadstype.DAGLIG_REISE) != null

        fun mockMaksAntallSomSkalRoutesPåDagligReise(maksAntall: Int) {
            unleashService.mockGetVariant(
                Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                Variant("antall", maksAntall.toString(), true),
            )
        }

        fun mockBrukerHarDagligReiseVedtakIArena(erAktivt: Boolean) {
            val arenaVedtak =
                ArenaStatusDtoUtil
                    .vedtakStatus(harVedtak = true, harAktivtVedtak = erAktivt, harVedtakUtenUtfall = false)
            val statusFraArena =
                ArenaStatusDto(
                    sak = SakStatus(harAktivSakUtenVedtak = false),
                    vedtak = arenaVedtak,
                )
            every { arenaClient.hentStatus(any()) } returns statusFraArena
        }

        fun mockAapVedtak(erAktivt: Boolean) {
            val pågåendePeriode =
                periodeAAP(
                    fom = LocalDate.now().minusDays(1),
                    tom = LocalDate.now().plusDays(1),
                )
            every { ytelseClient.hentYtelser(any()) } returns
                ytelsePerioderDto(
                    perioder = if (erAktivt) listOf(pågåendePeriode) else emptyList(),
                    kildeResultat = listOf(kildeResultatAAP()),
                )
        }
    }

    fun mockHentIdenterFraPdl() {
        every { pdlClient.hentPersonidenter(any()) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false, "FOLKEREGISTERIDENT"))) }
    }
}
