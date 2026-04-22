package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
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
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.kildeResultatAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.concurrent.CompletableFuture

class SkjemaRoutingIntegrationTest(
    @Autowired private val arenaClient: ArenaClient,
    @Autowired private val pdlClient: PdlClient,
    @Autowired private val vedtakRepository: VedtakRepository,
) : CleanDatabaseIntegrationTest() {
    val jonasIdent = "12345678910"
    val ernaIdent = "10987654321"

    @ParameterizedTest
    @EnumSource(
        value = Skjematype::class,
        names = ["SØKNAD_BARNETILSYN", "SØKNAD_LÆREMIDLER", "SØKNAD_BOUTGIFTER"],
    )
    fun `visse stønadstyper skal alltid routes til ny løsning`(skjematype: Skjematype) {
        val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

        assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
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

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
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

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret()).isTrue()
        }

        @Test
        fun `skal route til gammel løsning hvis person har aktivt vedtak i Arena`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 10)
            mockDagligReiseVedtakIArena(erAktivt = true)

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal route til gammel løsning hvis person har AAP men fortrolig adresse`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 10)
            mockAapVedtak(erAktivt = true)
            mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal route til gammel løsning hvis person har AAP men strengt fortrolig adresse`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 10)
            mockAapVedtak(erAktivt = true)
            mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `skal svare avsjekk hvis feature toggle sier at ingen skal slippe gjennom`() {
            mockAapVedtak(erAktivt = true)
            mockMaksAntallSomKanRoutesPrivatBil(0)

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
            assertThat(routingHarBlittLagret()).isFalse()
        }

        @Test
        fun `brukere med aktiv AAP skal bli routet til ny løsning`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 10)
            mockDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)
            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret()).isTrue()
        }

        @Test
        fun `skal slippe gjennom personer til ny løsning, men bare til maks antall er nådd`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 1)
            mockDagligReiseVedtakIArena(erAktivt = false)
            mockAapVedtak(erAktivt = true)

            val routingSjekkFørsteRouting =
                kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, Skjematype.SØKNAD_DAGLIG_REISE))
            val routingSjekkAndreRouting =
                kall.skjemaRouting.sjekk(IdentSkjematype(ernaIdent, Skjematype.SØKNAD_DAGLIG_REISE))

            assertThat(routingSjekkFørsteRouting.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret(ident = jonasIdent)).isTrue()
            assertThat(routingSjekkAndreRouting.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
            assertThat(routingHarBlittLagret(ident = ernaIdent)).isFalse()
        }

        @Test
        fun `skal håndtere samtidige kall og ikke kaste feil`() {
            mockMaksAntallSomKanRoutesPrivatBil(maksAntall = 1)
            mockDagligReiseVedtakIArena(erAktivt = false)

            // Legger til en sleep før aap-perioder returneres
            every {
                ytelseClient.hentYtelser(match { it.typer == listOf(TypeYtelsePeriode.AAP) })
            } answers {
                Thread.sleep(50)
                ytelsePerioderDto(
                    perioder = listOf(periodeAAP(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))),
                    kildeResultat = listOf(kildeResultatAAP()),
                )
            }

            // Trigger 5 samtidige kall
            val routingKall =
                (1..5).map {
                    CompletableFuture.supplyAsync {
                        kall.skjemaRouting.apiRespons.sjekk((IdentSkjematype(jonasIdent, Skjematype.SØKNAD_DAGLIG_REISE)))
                    }
                }

            routingKall.forEach {
                it.get().expectStatus().isOk
            }
        }

        private fun mockMaksAntallSomKanRoutesPrivatBil(maksAntall: Int) {
            unleashService.mockGetVariant(
                Toggle.SØKNAD_ROUTING_PRIVAT_BIL,
                Variant("antall", maksAntall.toString(), true),
            )
        }

        private fun mockMaksAntallSomKanRouteSamling(maksAntall: Int) {
            unleashService.mockGetVariant(
                Toggle.SØKNAD_ROUTING_REISE_TIL_SAMLING,
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
            every {
                ytelseClient.hentYtelser(
                    match {
                        it.typer ==
                            listOf(
                                TypeYtelsePeriode.AAP,
                            )
                    },
                )
            } returns
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
            every { pdlClient.hentPersonidenterMedSluttbrukerSinContext(any()) } answers
                {
                    PdlIdenter(
                        listOf(
                            PdlIdent(
                                ident = firstArg(),
                                historisk = false,
                                gruppe = "FOLKEREGISTERIDENT",
                            ),
                        ),
                    )
                }
        }
    }

    private fun routingHarBlittLagret(
        skjematype: Skjematype = Skjematype.SØKNAD_DAGLIG_REISE,
        ident: String = jonasIdent,
    ) = testoppsettService.hentSøknadRouting(ident, skjematype) != null

    @Nested
    inner class DagligReiseKjøreliste {
        val kjørelisteRoutingRequest =
            IdentSkjematype(
                ident = jonasIdent,
                skjematype = Skjematype.DAGLIG_REISE_KJØRELISTE,
            )

        @Test
        fun `skal route til ny løsning hvis person har vedtak med privat bil`() {
            opprettBehandlingMedVedtakForPrivatBil(Stønadstype.DAGLIG_REISE_TSO)

            val routingSjekk = kall.skjemaRouting.sjekk(kjørelisteRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret(Skjematype.DAGLIG_REISE_KJØRELISTE)).isTrue()
        }

        @Test
        fun `skal route til gammel løsning hvis person ikke har vedtak`() {
            val routingSjekk = kall.skjemaRouting.sjekk(kjørelisteRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret(Skjematype.DAGLIG_REISE_KJØRELISTE)).isFalse()
        }

        @Test
        fun `skal route til gammel løsning hvis person har vedtak uten privat bil`() {
            opprettBehandlingMedVedtakUtenPrivatBil(Stønadstype.DAGLIG_REISE_TSO)

            val routingSjekk = kall.skjemaRouting.sjekk(kjørelisteRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret(Skjematype.DAGLIG_REISE_KJØRELISTE)).isFalse()
        }

        @Test
        fun `skal alltid svare ja hvis personen har blitt routet til ny løsning tidligere`() {
            testoppsettService.lagreSøknadRouting(
                SkjemaRouting(
                    ident = jonasIdent,
                    type = Skjematype.DAGLIG_REISE_KJØRELISTE,
                    detaljer = JsonWrapper("{}"),
                ),
            )

            val routingSjekk = kall.skjemaRouting.sjekk(kjørelisteRoutingRequest)

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
        }

        private fun opprettBehandlingMedVedtakForPrivatBil(stønadstype: Stønadstype) {
            val fagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = stønadstype)
            val dagligReiseBehandling = behandling(fagsak)

            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

            val vedtak =
                GeneriskVedtak(
                    behandlingId = dagligReiseBehandling.id,
                    type = TypeVedtak.INNVILGELSE,
                    data =
                        InnvilgelseDagligReise(
                            vedtaksperioder = DagligReiseTestUtil.defaultVedtaksperioder,
                            beregningsresultat = DagligReiseTestUtil.defaultBeregningsresultat,
                            rammevedtakPrivatBil = RammevedtakPrivatBilUtil.rammevedtakPrivatBil(),
                            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                        ),
                    gitVersjon = Applikasjonsversjon.versjon,
                    tidligsteEndring = null,
                    opphørsdato = null,
                )
            vedtakRepository.insert(vedtak)
        }

        private fun opprettBehandlingMedVedtakUtenPrivatBil(stønadstype: Stønadstype) {
            val fagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = stønadstype)
            val dagligReiseBehandling = behandling(fagsak)

            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

            val vedtak =
                DagligReiseTestUtil.innvilgelse(
                    behandlingId = dagligReiseBehandling.id,
                )
            vedtakRepository.insert(vedtak)
        }
    }
}
