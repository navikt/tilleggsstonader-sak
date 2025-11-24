package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig.Companion.lagPersonKort
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockGetVariant
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.kildeResultatAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SkjemaRoutingIntegrationTest(
    @Autowired private val ytelseClient: YtelseClient,
    @Autowired private val arenaClient: ArenaClient,
    @Autowired private val pdlClient: PdlClient,
) : CleanDatabaseIntegrationTest() {
    val jonasIdent = "12345678910"
    val ernaIdent = "10987654321"

    @ParameterizedTest
    @EnumSource(
        value = Skjematype::class,
        names = ["SØKNAD_BARNETILSYN", "SØKNAD_LÆREMIDLER", "SØKNAD_BOUTGIFTER"],
    )
    fun `visse stønadstyper skal alltid routes til ny løsning`(skjematype: Skjematype) {
        val routingSjekk = kall.søknadRouting.skjemaRouting(IdentSkjematype(jonasIdent, skjematype))

        assertThat(routingSjekk.skalBehandlesINyLøsning).isTrue()
        assertThat(routingHarBlittLagret(skjematype)).isFalse()
    }

    @Nested
    inner class DagligReise {
        val skjemaRoutingDagligReise =
            SkjemaRouting(
                ident = jonasIdent,
                type = Skjematype.SØKNAD_DAGLIG_REISE,
                detaljer = JsonWrapper("{}"),
            )
        val dagligReiseRoutingRequest =
            IdentSkjematype(
                ident = jonasIdent,
                skjematype = Skjematype.SØKNAD_DAGLIG_REISE,
            )

        @Test
        fun `skal alltid svare ja hvis personen har blitt routet til ny løsning tidligere`() {
            testoppsettService.lagreSøknadRouting(skjemaRoutingDagligReise)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isTrue()
        }

        @ParameterizedTest
        @EnumSource(
            value = Stønadstype::class,
            names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"],
        )
        fun `skal lagre routing og svare true hvis det finnes en daglig reise-behandling på personen`(stønadstype: Stønadstype) {
            val dagligReiseFagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = stønadstype)
            val dagligReiseBehandling = behandling(dagligReiseFagsak)

            testoppsettService.lagreFagsak(dagligReiseFagsak)
            testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isTrue()
            assertThat(routingHarBlittLagret()).isTrue()
        }

        @Test
        fun `skal route til gammel løsning hvis person har aktivt vedtak i Arena`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 10)
            mockDagligReiseVedtakIArena(erAktivt = true)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal route til gammel løsning hvis person har AAP men fortrolig adresse`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 10)
            mockAapVedtak(erAktivt = true)
            mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal route til gammel løsning hvis person har AAP men strengt fortrolig adresse`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 10)
            mockAapVedtak(erAktivt = true)
            mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal svare nei hvis feature toggle sier at ingen skal slippe gjennom`() {
            mockMaksAntallSomKanRoutesPåDagligReise(0)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)

            assertThat(routingSjekk.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `brukere med aktiv AAP skal bli routet til ny løsning`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 10)
            mockDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)
            assertThat(routingSjekk.skalBehandlesINyLøsning).isTrue()
            assertThat(routingHarBlittLagret()).isTrue()
        }

        @Test
        fun `brukere uten aktiv AAP skal bli routet til gammel løsning`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 10)
            mockDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = false)

            val routingSjekk = kall.søknadRouting.skjemaRouting(dagligReiseRoutingRequest)
            assertThat(routingSjekk.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal slippe gjennom personer til ny løsning, men bare til maks antall er nådd`() {
            mockMaksAntallSomKanRoutesPåDagligReise(maksAntall = 1)
            mockDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekkFørsteRouting = kall.søknadRouting.skjemaRouting(IdentSkjematype(jonasIdent, Skjematype.SØKNAD_DAGLIG_REISE))
            val routingSjekkAndreRouting = kall.søknadRouting.skjemaRouting(IdentSkjematype(ernaIdent, Skjematype.SØKNAD_DAGLIG_REISE))

            assertThat(routingSjekkFørsteRouting.skalBehandlesINyLøsning).isTrue()
            assertThat(routingHarBlittLagret(ident = jonasIdent)).isTrue()
            assertThat(routingSjekkAndreRouting.skalBehandlesINyLøsning).isFalse()
            assertThat(routingHarBlittLagret(ident = ernaIdent)).isFalse()
        }

        private fun mockMaksAntallSomKanRoutesPåDagligReise(maksAntall: Int) {
            unleashService.mockGetVariant(
                Toggle.SØKNAD_ROUTING_DAGLIG_REISE,
                Variant("antall", maksAntall.toString(), true),
            )
        }

        private fun mockDagligReiseVedtakIArena(erAktivt: Boolean) {
            val arenaVedtak =
                ArenaStatusDtoUtil
                    .vedtakStatus(harVedtak = true, harAktivtVedtak = erAktivt, harVedtakUtenUtfall = false)
            val statusFraArena =
                ArenaStatusDto(
                    sak = SakStatus(harAktivSakUtenVedtak = false),
                    vedtak = arenaVedtak,
                )
            every { arenaClient.hentStatus(any()) } returns statusFraArena
            mockHentIdenterFraPdl() // Trengs fordi ArenaClient:hentStatus først henter identer fra PDL
        }

        private fun mockAapVedtak(erAktivt: Boolean) {
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

        private fun mockPersonMedAdressebeskyttelse(adressebeskyttelseGradering: AdressebeskyttelseGradering) {
            every { pdlClient.hentPersonKortBolk(any()) } answers {
                firstArg<List<String>>().associateWith {
                    lagPersonKort(
                        fornavn = it,
                        adressebeskyttelseGradering = adressebeskyttelseGradering,
                    )
                }
            }
        }

        private fun mockHentIdenterFraPdl() {
            every { pdlClient.hentPersonidenter(any()) } answers
                { PdlIdenter(listOf(PdlIdent(ident = firstArg(), historisk = false, gruppe = "FOLKEREGISTERIDENT"))) }
        }
    }

    private fun routingHarBlittLagret(
        skjematype: Skjematype = Skjematype.SØKNAD_DAGLIG_REISE,
        ident: String = jonasIdent,
    ) = testoppsettService.hentSøknadRouting(ident, skjematype) != null
}
