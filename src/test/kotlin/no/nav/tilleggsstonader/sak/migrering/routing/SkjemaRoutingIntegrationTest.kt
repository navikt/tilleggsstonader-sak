package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.felles.SkjemaRoutingAksjon
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto.KildeResultatYtelse
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig.Companion.BARN2_FNR
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig.Companion.BARN_FNR
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig.Companion.lagPersonKort
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockGetVariant
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockIsEnabled
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
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

    @ParameterizedTest
    @EnumSource(
        value = Skjematype::class,
        names = ["SØKNAD_BARNETILSYN", "SØKNAD_LÆREMIDLER", "SØKNAD_BOUTGIFTER"],
    )
    fun `V1 - visse stønadstyper skal alltid routes til ny løsning`(skjematype: Skjematype) {
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

        @Nested
        inner class GammelLøsning {
            @Test
            fun `skal route til gammel løsning hvis person har aktivt vedtak i Arena`() {
                mockDagligReiseVedtakIArena(erAktivt = true)
                mockPrivatBilToggle(false)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret()).isFalse()
            }

            @Test
            fun `skal route til gammel løsning hvis person har fortrolig adresse`() {
                mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)
                mockPrivatBilToggle(true)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret()).isFalse()
            }

            @Test
            fun `skal route til gammel løsning hvis person har strengt fortrolig adresse`() {
                mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
                mockPrivatBilToggle(true)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret()).isFalse()
            }
        }

        @Nested
        inner class Avsjekk {
            @Test
            fun `skal svare avsjekk hvis privat bil ikke er skrudd på og ingen tidligere behandling`() {
                mockPrivatBilToggle(false)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
                assertThat(routingHarBlittLagret()).isFalse()
            }

            @ParameterizedTest
            @EnumSource(
                value = Stønadstype::class,
                names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"],
            )
            fun `skal svare AVSJEKK hvis det finnes en daglig reise-behandling på personen og privat bil ikke er skrudd på`(
                stønadstype: Stønadstype,
            ) {
                val dagligReiseFagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = stønadstype)
                val dagligReiseBehandling = behandling(dagligReiseFagsak)

                testoppsettService.lagreFagsak(dagligReiseFagsak)
                testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

                mockPrivatBilToggle(false)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
            }
        }

        @Nested
        inner class NyLøsning {
            @Test
            fun `skal route til ny løsning hvis person har vedtak med privat bil`() {
                mockPrivatBilToggle(false)
                opprettBehandlingMedVedtakForPrivatBil(Stønadstype.DAGLIG_REISE_TSO)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            }

            @Test
            fun `skal route til ny løsning hvis privat bil toggle er skrudd på`() {
                mockPrivatBilToggle(true)

                val routingSjekk = kall.skjemaRouting.sjekk(dagligReiseRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            }
        }

        private fun mockPrivatBilToggle(skruddPå: Boolean = true) {
            unleashService.mockIsEnabled(Toggle.SØKNAD_ROUTING_PRIVAT_BIL, skruddPå)
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
    }

    @Nested
    inner class ReiseTilSamling {
        val reiseTilSamlingRoutingRequest =
            IdentSkjematype(
                ident = jonasIdent,
                skjematype = Skjematype.SØKNAD_REISE_TIL_SAMLING,
            )

        @Nested
        inner class GammelLøsning {
            @Test
            fun `skal route til gammel løsning hvis person har fortrolig adresse`() {
                mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isFalse()
            }

            @Test
            fun `skal route til gammel løsning hvis person har strengt fortrolig adresse`() {
                mockPersonMedAdressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isFalse()
            }

            @Test
            fun `skal route til gammel løsning når maks antall er nådd og ingen eksisterende data`() {
                mockMaksAntall(Toggle.SØKNAD_ROUTING_REISE_TIL_SAMLING, 0)

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isFalse()
            }
        }

        @Nested
        inner class NyLøsning {
            @Test
            fun `skal alltid svare ny løsning hvis personen har blitt routet til ny løsning tidligere`() {
                testoppsettService.lagreSøknadRouting(
                    SkjemaRouting(
                        ident = jonasIdent,
                        type = Skjematype.SØKNAD_REISE_TIL_SAMLING,
                        detaljer = JsonWrapper("{}"),
                    ),
                )

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isTrue()
            }

            @Test
            fun `skal lagre routing og svare ny løsning hvis det finnes en behandling på personen`() {
                val fagsak =
                    fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
                val reiseTilSamlingBehandling = behandling(fagsak)

                testoppsettService.lagreFagsak(fagsak)
                testoppsettService.lagre(behandling = reiseTilSamlingBehandling, opprettGrunnlagsdata = false)

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isTrue()
            }

            @Test
            fun `skal svare ny løsning hvis ingen tidligere data og maks antall ikke er nådd`() {
                mockMaksAntall(Toggle.SØKNAD_ROUTING_REISE_TIL_SAMLING, 10)

                val routingSjekk = kall.skjemaRouting.sjekk(reiseTilSamlingRoutingRequest)

                assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
                assertThat(routingHarBlittLagret(Skjematype.SØKNAD_REISE_TIL_SAMLING)).isTrue()
            }
        }
    }

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

        private fun opprettBehandlingMedVedtakUtenPrivatBil(stønadstype: Stønadstype) {
            val fagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = stønadstype)
            val dagligReiseBehandling = behandling(fagsak)

            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling = dagligReiseBehandling, opprettGrunnlagsdata = false)

            val vedtak =
                DagligReiseTestUtil.innvilgelse(
                    behandlingId = dagligReiseBehandling.id,
                    rammevedtakPrivatBil = null,
                )
            vedtakRepository.insert(vedtak)
        }
    }

    @Nested
    inner class FlyttingOgROAH {
        @Test
        fun `skal route ROAH til gammel løsning hvis bruker har flere barn og ett har fortrolig adresse`() {
            val skjematype = Skjematype.SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = true)
            every { pdlClient.hentBarn(listOf(BARN_FNR, BARN2_FNR)) } returns
                mapOf(
                    BARN_FNR to pdlBarn(),
                    BARN2_FNR to
                        pdlBarn(
                            adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG, metadataGjeldende)),
                        ),
                )

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret(skjematype)).isFalse()
        }

        @Test
        fun `skal route ROAH til ny løsning hvis bruker har flere barn og ingen har fortrolig adresse`() {
            val skjematype = Skjematype.SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = true)
            every { pdlClient.hentBarn(listOf(BARN_FNR, BARN2_FNR)) } returns
                mapOf(
                    BARN_FNR to pdlBarn(),
                    BARN2_FNR to pdlBarn(),
                )

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret(skjematype)).isTrue()
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal route til gammel løsning hvis toggle er av`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = false)
            mockAktivitetspenger(harAktivitetspenger = true)

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.GAMMEL_LØSNING)
            assertThat(routingHarBlittLagret(skjematype)).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal lagre routing og svare ny løsning hvis bruker har aktivitetspenger`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = true)

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret(skjematype)).isTrue()
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal svare avsjekk hvis bruker ikke har aktivitetspenger eller behandling`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = false)

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
            assertThat(routingHarBlittLagret(skjematype)).isFalse()
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal svare avsjekk hvis henting av aktivitetspenger feiler`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            every { ytelseClient.hentYtelser(any()) } returns
                ytelsePerioderDto(
                    perioder = emptyList(),
                    kildeResultat = listOf(KildeResultatYtelse(TypeYtelsePeriode.AKTIVITETSPENGER, ResultatKilde.FEILET)),
                )

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal svare avsjekk hvis kallet for å hente aktivitetspenger kaster feil`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            every { ytelseClient.hentYtelser(any()) } throws RuntimeException("ung-sak er nede")

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal ikke svare ny løsning kun fordi routing er lagret tidligere`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = false)
            testoppsettService.lagreSøknadRouting(
                SkjemaRouting(ident = jonasIdent, type = skjematype, detaljer = JsonWrapper("{}")),
            )

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.AVSJEKK)
        }

        @ParameterizedTest
        @EnumSource(value = Skjematype::class, names = ["SØKNAD_FLYTTING", "SØKNAD_REISE_OPPSTART_AVSLUTNING_HJEMREISE"])
        fun `skal svare ny løsning flere ganger uten å feile på lagring av routing`(skjematype: Skjematype) {
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = true)

            kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))
            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
        }

        @Test
        fun `skal lagre routing og svare ny løsning hvis det finnes en flytting-behandling på personen`() {
            val skjematype = Skjematype.SØKNAD_FLYTTING
            mockToggle(skjematype, skruddPå = true)
            mockAktivitetspenger(harAktivitetspenger = false)
            val fagsak = fagsak(identer = setOf(PersonIdent(jonasIdent)), stønadstype = Stønadstype.FLYTTING_TSO)
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling = behandling(fagsak), opprettGrunnlagsdata = false)

            val routingSjekk = kall.skjemaRouting.sjekk(IdentSkjematype(jonasIdent, skjematype))

            assertThat(routingSjekk.aksjon).isEqualTo(SkjemaRoutingAksjon.NY_LØSNING)
            assertThat(routingHarBlittLagret(skjematype)).isTrue()
        }

        private fun mockToggle(
            skjematype: Skjematype,
            skruddPå: Boolean,
        ) {
            val routingStrategi = bestemRoutingStrategi(skjematype) as RoutingStrategi.FlyttingOgROAH
            unleashService.mockIsEnabled(routingStrategi.featureToggle as Toggle, skruddPå)
        }

        private fun mockAktivitetspenger(harAktivitetspenger: Boolean) {
            val perioder =
                if (harAktivitetspenger) {
                    listOf(YtelsePeriode.Aktivitetspenger(fom = LocalDate.now(), tom = LocalDate.now().plusMonths(3)))
                } else {
                    emptyList()
                }
            every { ytelseClient.hentYtelser(any()) } returns
                ytelsePerioderDto(
                    perioder = perioder,
                    kildeResultat = listOf(KildeResultatYtelse(TypeYtelsePeriode.AKTIVITETSPENGER, ResultatKilde.OK)),
                )
        }
    }

    private fun mockMaksAntall(
        toggle: Toggle,
        maksAntall: Int,
    ) {
        unleashService.mockGetVariant(toggle, Variant("antall", maksAntall.toString(), true))
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
        every { pdlClient.hentPersonidenterMedSluttbrukerSinContext(any()) } answers {
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

    private fun opprettBehandlingMedVedtakForPrivatBil(
        stønadstype: Stønadstype,
        ident: String = jonasIdent,
    ) {
        val fagsak = fagsak(identer = setOf(PersonIdent(ident)), stønadstype = stønadstype)
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

    private fun routingHarBlittLagret(
        skjematype: Skjematype = Skjematype.SØKNAD_DAGLIG_REISE,
        ident: String = jonasIdent,
    ) = testoppsettService.hentSøknadRouting(ident, skjematype) != null
}
