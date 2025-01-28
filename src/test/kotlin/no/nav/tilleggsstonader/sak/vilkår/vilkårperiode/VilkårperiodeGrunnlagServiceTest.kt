package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.RegisterAktivitetClientConfig
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse.YtelseSubtype
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class VilkårperiodeGrunnlagServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService

    @Autowired
    lateinit var vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository

    @Autowired
    lateinit var registerAktivitetClient: RegisterAktivitetClient

    @Autowired
    lateinit var ytelseClient: YtelseClient

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        RegisterAktivitetClientConfig.resetMock(registerAktivitetClient)
    }

    @Test
    internal fun `skal lagre ned grunnlagsadata på aktiviteter når man henter vilkårsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)).isNull()

        val response = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

        assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)!!.grunnlag)
            .isEqualTo(
                response,
            )
        assertThat(response!!.aktivitet.aktiviteter).isNotEmpty
        assertThat(response.ytelse.perioder).isNotEmpty
    }

    @Test
    internal fun `skal ikke lagre ned grunnlagsadata for behandling som ikke er redigerbar`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

        vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

        assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)).isNull()
    }

    @Test
    fun `skal kun ta med aktiviteter som er stønadsberettiget i grunnlaget`() {
        val idStønadsberettiget = "1"
        every {
            registerAktivitetClient.hentAktiviteter(any(), any(), any())
        } returns
            listOf(
                ArenaKontraktUtil.aktivitetArenaDto(id = idStønadsberettiget, erStønadsberettiget = true),
                ArenaKontraktUtil.aktivitetArenaDto(id = "2", erStønadsberettiget = false),
            )

        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
        assertThat(
            grunnlag!!
                .grunnlag.aktivitet.aktiviteter
                .map { it.id },
        ).hasSize(1)
            .contains(idStønadsberettiget)
    }

    @Test
    fun `veileder skal ikke hentes då det opprettes grunnlag når behandlingStatus=OPPRETTET`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val exception =
            catchThrowableOfType<ApiFeil> {
                BrukerContextUtil.testWithBrukerContext(groups = listOf(rolleConfig.veilederRolle)) {
                    vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)
                }
            }
        assertThat(exception.frontendFeilmelding).contains("Behandlingen er ikke påbegynt")
    }

    @Test
    fun `veileder skal ikke kunne hente behandlingen hvis statusen er annet enn UTREDES`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.UTREDES))

        val exception =
            catchThrowableOfType<ApiFeil> {
                BrukerContextUtil.testWithBrukerContext(groups = listOf(rolleConfig.veilederRolle)) {
                    vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)
                }
            }
        assertThat(exception.frontendFeilmelding).contains("Behandlingen er ikke påbegynt")
    }

    @Test
    fun `saksbehandler skal kunne opprette grunnlag hvis behandlingStatus=OPPRETTET`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        BrukerContextUtil.testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
            vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)
        }
    }

    @Test
    fun `skal ta ta med ferdig avklarte perioder fra AAP som grunnlag`() {
        val nesteDag = LocalDate.now().plusDays(1) // for å få 2 ulike AAP-perioder
        every {
            ytelseClient.hentYtelser(any())
        } returns
            YtelsePerioderDto(
                perioder =
                    listOf(
                        YtelsePeriode(TypeYtelsePeriode.AAP, LocalDate.now(), LocalDate.now(), aapErFerdigAvklart = false),
                        YtelsePeriode(TypeYtelsePeriode.AAP, nesteDag, nesteDag, aapErFerdigAvklart = true),
                        YtelsePeriode(
                            TypeYtelsePeriode.ENSLIG_FORSØRGER,
                            LocalDate.now(),
                            LocalDate.now(),
                            aapErFerdigAvklart = null,
                            ensligForsørgerStønadstype = EnsligForsørgerStønadstype.OVERGANGSSTØNAD,
                        ),
                    ),
                hentetInformasjon = emptyList(),
            )

        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
        val perioder = grunnlag!!.grunnlag.ytelse.perioder
        assertThat(perioder).containsExactlyInAnyOrder(
            PeriodeGrunnlagYtelse(TypeYtelsePeriode.AAP, LocalDate.now(), LocalDate.now()),
            PeriodeGrunnlagYtelse(
                TypeYtelsePeriode.AAP,
                nesteDag,
                nesteDag,
                subtype = YtelseSubtype.AAP_FERDIG_AVKLART,
            ),
            PeriodeGrunnlagYtelse(
                TypeYtelsePeriode.ENSLIG_FORSØRGER,
                LocalDate.now(),
                LocalDate.now(),
                subtype = YtelseSubtype.OVERGANGSSTØNAD,
            ),
        )
    }

    @Test
    fun `skal ikke ta med EF perioder med barnetilsyn som grunnlag`() {
        every {
            ytelseClient.hentYtelser(any())
        } returns
            YtelsePerioderDto(
                perioder =
                    listOf(
                        YtelsePeriode(TypeYtelsePeriode.AAP, LocalDate.now(), LocalDate.now(), aapErFerdigAvklart = false),
                        YtelsePeriode(
                            TypeYtelsePeriode.ENSLIG_FORSØRGER,
                            LocalDate.now(),
                            LocalDate.now(),
                            aapErFerdigAvklart = null,
                            EnsligForsørgerStønadstype.BARNETILSYN,
                        ),
                        YtelsePeriode(
                            TypeYtelsePeriode.ENSLIG_FORSØRGER,
                            LocalDate.now(),
                            LocalDate.now(),
                            aapErFerdigAvklart = null,
                            EnsligForsørgerStønadstype.SKOLEPENGER,
                        ),
                        YtelsePeriode(
                            TypeYtelsePeriode.ENSLIG_FORSØRGER,
                            LocalDate.now(),
                            LocalDate.now(),
                            aapErFerdigAvklart = null,
                            EnsligForsørgerStønadstype.OVERGANGSSTØNAD,
                        ),
                    ),
                hentetInformasjon = emptyList(),
            )

        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
        val perioder = grunnlag!!.grunnlag.ytelse.perioder
        assertThat(perioder).containsExactlyInAnyOrder(
            PeriodeGrunnlagYtelse(TypeYtelsePeriode.AAP, LocalDate.now(), LocalDate.now()),
            PeriodeGrunnlagYtelse(
                TypeYtelsePeriode.ENSLIG_FORSØRGER,
                LocalDate.now(),
                LocalDate.now(),
                YtelseSubtype.SKOLEPENGER,
            ),
            PeriodeGrunnlagYtelse(
                TypeYtelsePeriode.ENSLIG_FORSØRGER,
                LocalDate.now(),
                LocalDate.now(),
                YtelseSubtype.OVERGANGSSTØNAD,
            ),
        )
    }

    @Nested
    inner class OppdaterGrunnlag {
        @Test
        fun `skal kunne oppdatere grunnlaget når en behandling redigerbar og i riktig steg`() {
            every {
                registerAktivitetClient.hentAktiviteter(any(), any(), any())
            } answers {
                listOf(ArenaKontraktUtil.aktivitetArenaDto(id = "1", erStønadsberettiget = true))
            } andThenAnswer {
                listOf(ArenaKontraktUtil.aktivitetArenaDto(id = "2", erStønadsberettiget = true))
            }

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.INNGANGSVILKÅR))
            val grunnlag = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)!!
            val grunnlag2 = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)!!

            BrukerContextUtil.testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
                vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandling.id)
            }

            val grunnlag3 = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)!!

            assertThat(grunnlag.aktivitet.aktiviteter.map { it.id }).containsExactly("1")
            assertThat(grunnlag2.aktivitet.aktiviteter.map { it.id }).containsExactly("1")

            // Oppdatert grunnlag inneholder kun id=2
            assertThat(grunnlag3.aktivitet.aktiviteter.map { it.id }).containsExactly("2")
        }

        @Test
        fun `skal bruke tidligere hentet informasjon sin fom og bruke tom fra dagens dato`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.INNGANGSVILKÅR))
            val fomFørsteGangHentet = LocalDate.now().minusYears(3).minusDays(1)
            behandling.let {
                val hentetInformasjon =
                    HentetInformasjon(fom = fomFørsteGangHentet, tom = LocalDate.now(), LocalDateTime.now())
                val aktivitet = GrunnlagAktivitet(emptyList())
                val grunnlag = VilkårperioderGrunnlag(aktivitet, GrunnlagYtelse(emptyList()), hentetInformasjon)
                vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(it.id, grunnlag))
            }
            BrukerContextUtil.testWithBrukerContext("beh1", listOf(rolleConfig.saksbehandlerRolle)) {
                vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandling.id)
            }
            with(vilkårperioderGrunnlagRepository.findByIdOrThrow(behandling.id)) {
                assertThat(sporbar.opprettetAv).isEqualTo("VL")
                assertThat(sporbar.endret.endretAv).isEqualTo("beh1")
                assertThat(grunnlag.hentetInformasjon.fom).isEqualTo(fomFørsteGangHentet)
                assertThat(grunnlag.hentetInformasjon.tom)
                    .isEqualTo(YearMonth.now().plusYears(1).atEndOfMonth())
            }
        }

        @Test
        fun `skal kunne hente grunnlagsdata i førstegangsbehandlinger fra annet dato`() {
            val henteFom = LocalDate.of(2023, 1, 1)

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.INNGANGSVILKÅR))
            vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

            vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandling.id, henteFom)
            val grunnlag = vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(behandling.id)

            assertThat(grunnlag!!.hentetInformasjon.fom).isEqualTo(henteFom)
        }

        @Test
        fun `skal ikke kunne oppdatere dataen når behandlingen har feil steg`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.VILKÅR))
            val feil =
                org.junit.jupiter.api
                    .assertThrows<Feil> { vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandling.id) }
            assertThat(feil.frontendFeilmelding)
                .isEqualTo("Kan ikke oppdatere grunnlag når behandlingen er i annet steg enn vilkår.")
        }

        @Test
        fun `skal ikke kunne oppdatere dataen når behandlingen er låst`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))
            val feil =
                org.junit.jupiter.api
                    .assertThrows<Feil> { vilkårperiodeGrunnlagService.oppdaterGrunnlag(behandling.id) }
            assertThat(feil.frontendFeilmelding)
                .isEqualTo("Kan ikke oppdatere grunnlag når behandlingen er låst")
        }
    }
}
