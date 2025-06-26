package no.nav.tilleggsstonader.sak.oppfølging

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.OppfølgingRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeOmstillingsstønad
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class OppfølgingOpprettKontrollerServiceTest {
    val behandling =
        behandling(
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = LocalDateTime.now(),
        )

    val behandlingRepository = mockk<BehandlingRepository>()
    val fagsakService = mockk<FagsakService>()
    val registerAktivitetService = mockk<RegisterAktivitetService>()
    val ytelseService = mockk<YtelseService>()
    val taskService = mockk<TaskService>()
    val oppfølgingRepository = OppfølgingRepositoryFake()
    val vilkårperiodeRepository = mockk<VilkårperiodeRepository>()
    val vedtakRepository = mockk<VedtakRepository>()

    val oppfølgingOpprettKontrollerService =
        OppfølgingOpprettKontrollerService(
            behandlingRepository = behandlingRepository,
            vedtakRepository = vedtakRepository,
            vilkårperiodeRepository = vilkårperiodeRepository,
            registerAktivitetService = registerAktivitetService,
            ytelseService = ytelseService,
            fagsakService = fagsakService,
            taskService = taskService,
            oppfølgingRepository = oppfølgingRepository,
        )

    val vedtaksperiode =
        vedtaksperiode(
            fom = YearMonth.now().minusMonths(1).atDay(1),
            tom = YearMonth.now().plusMonths(2).atDay(1),
        )

    val målgruppe = målgruppe(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
    val målgruppeNedsattArbeidsevne =
        målgruppe(
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.NEDSATT_ARBEIDSEVNE),
            begrunnelse = "Begrunnelse",
        )
    val aktivitet = aktivitet(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)

    @BeforeEach
    fun setUp() {
        oppfølgingRepository.deleteAll()
        every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
            listOf(målgruppe, aktivitet)
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { fagsakService.hentMetadata(any()) } answers {
            val fagsakIds = firstArg<List<FagsakId>>()
            fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
        }

        mockVedtakTilsynBarn(vedtaksperiode)
        mockHentYtelser()
        mockHentAktiviteter()
        mockHentAktiviteter()
    }

    @Nested
    inner class EndringIMålgruppe {
        @BeforeEach
        fun setUp() {
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom))
        }

        @Test
        fun `skal ikke finne treff hvis ytelsen blitt forlenget`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.plusMonths(1))
            mockHentYtelser(ytelse)

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis det ikke finnes noen vedtaksperioder`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom.plusDays(3), tom = vedtaksperiode.tom)
            mockHentYtelser(ytelse)
            mockVedtakTilsynBarn()

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis endring ikke påvirker vedtaksperiode`() {
            mockVedtakTilsynBarn(vedtaksperiode.copy(fom = LocalDate.now(), tom = LocalDate.now()))
            mockHentYtelser(periodeAAP(fom = vedtaksperiode.fom, tom = LocalDate.now()))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = LocalDate.now()))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis ytelsen slutter tidligere`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(5))
            mockHentYtelser(ytelse)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe())
                    .containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = ytelse.tom))
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal finne treff hvis ytelsen begynner senere`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom.plusDays(3), tom = vedtaksperiode.tom)
            mockHentYtelser(ytelse)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe())
                    .containsExactly(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = ytelse.fom))
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal finne treff hvis ytelsen begynner senere og slutter tidligere`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom.plusDays(3), tom = vedtaksperiode.tom.minusDays(3))
            mockHentYtelser(ytelse)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe()).containsExactly(
                    Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = ytelse.fom),
                    Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = ytelse.tom),
                )
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal ikke finne treff hvis man har en målgruppe men er av feil type`() {
            val ytelse = periodeEnsligForsørger(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
            mockHentYtelser(ytelse)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe()).containsExactly(Kontroll(ÅrsakKontroll.INGEN_TREFF))
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder målgruppe som vi ikke henter fra annet system`() {
            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(målgruppeNedsattArbeidsevne, aktivitet)

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder gjelder endring i AAP som gjelder fra og med siste dagen i neste måneden`() {
            val tom = YearMonth.now().plusMonths(2).atDay(1)
            mockVedtakTilsynBarn(vedtaksperiode.copy(tom = tom))
            mockHentYtelser(periodeAAP(fom = vedtaksperiode.fom, tom = tom.minusDays(1)))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis det gjelder gjelder endring i AAP som gjelder neste måned`() {
            val tom = YearMonth.now().plusMonths(1).atEndOfMonth()
            mockVedtakTilsynBarn(vedtaksperiode.copy(tom = tom))
            mockHentYtelser(periodeAAP(fom = vedtaksperiode.fom, tom = tom.minusDays(1)))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe())
                    .containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom.minusDays(1)))
            }
        }

        @Test
        fun `skal finne treff for hver vedtaksperiode som ikke er sammenhengende som blir påvirket`() {
            val vedtaksperiode1 =
                vedtaksperiode(
                    fom = LocalDate.now().minusDays(1),
                    tom = LocalDate.now(),
                )
            val vedtaksperiode2 =
                vedtaksperiode(
                    fom = LocalDate.now().plusDays(2),
                    tom = LocalDate.now().plusDays(2),
                )
            mockVedtakTilsynBarn(vedtaksperiode1, vedtaksperiode2)
            mockHentYtelser(periodeAAP(fom = vedtaksperiode1.fom, tom = vedtaksperiode1.fom))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode1.fom, tom = vedtaksperiode2.tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe()).containsExactly(
                    Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = vedtaksperiode1.fom),
                    Kontroll(ÅrsakKontroll.INGEN_TREFF),
                )
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal finne en treff for vedtaksperioder som er sammenhengende som blir påvirket`() {
            val vedtaksperiode1 =
                vedtaksperiode(
                    fom = LocalDate.now().minusDays(1),
                    tom = LocalDate.now(),
                )
            val vedtaksperiode2 =
                vedtaksperiode(
                    fom = LocalDate.now().plusDays(1),
                    tom = LocalDate.now().plusDays(2),
                )
            mockVedtakTilsynBarn(vedtaksperiode1, vedtaksperiode2)
            mockHentYtelser(periodeAAP(fom = vedtaksperiode1.fom, tom = vedtaksperiode1.fom))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode1.fom, tom = vedtaksperiode2.tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe()).containsExactly(
                    Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = vedtaksperiode1.fom),
                )
                this.assertIngenEndringForAktiviteter()
            }
        }
    }

    @Nested
    inner class OmstillingsstønadSkalHåndtereAtTomMangler {
        @Test
        fun `skal ikke finne treff hvis tom mangler verdi då den settes til LocalDate-MAX`() {
            val tom = vedtaksperiode.fom.plusYears(2)
            mockTilstandForOmstillingsstønad(tom)

            assertThat(opprettOppfølging()).isNull()
        }

        private fun mockTilstandForOmstillingsstønad(tom: LocalDate) {
            val målgruppe =
                målgruppe(
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OMSTILLINGSSTØNAD),
                    fom = vedtaksperiode.fom,
                    tom = tom,
                )
            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(målgruppe, aktivitet.copy(tom = tom))

            mockVedtakTilsynBarn(vedtaksperiode.copy(målgruppe = FaktiskMålgruppe.GJENLEVENDE, tom = tom))
            mockHentYtelser(periodeOmstillingsstønad(fom = vedtaksperiode.fom, tom = null))
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))
        }
    }

    @Nested
    inner class EndringIAktivitet {
        @BeforeEach
        fun setUp() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
            mockHentYtelser(ytelse)
        }

        @Test
        fun `skal ikke finne treff hvis aktiviteten blitt forlenget`() {
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.plusMonths(1)))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis aktiviteten slutter tidligere`() {
            val tom = vedtaksperiode.tom.minusDays(5)
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere`() {
            val fom = vedtaksperiode.fom.plusDays(3)
            mockHentAktiviteter(aktivitetArenaDto(fom = fom, tom = vedtaksperiode.tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = fom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere og slutter tidligere`() {
            val aktivitet =
                aktivitetArenaDto(fom = vedtaksperiode.fom.plusDays(3), tom = vedtaksperiode.tom.minusDays(3))
            mockHentAktiviteter(aktivitet)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(
                    Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = aktivitet.fom),
                    Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = aktivitet.tom),
                )
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis man har en aktivitet men er av feil type`() {
            mockHentAktiviteter(
                aktivitetArenaDto(
                    fom = vedtaksperiode.fom,
                    tom = vedtaksperiode.tom,
                    erUtdanning = true,
                ),
            )

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(
                    Kontroll(ÅrsakKontroll.INGEN_TREFF),
                    Kontroll(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE),
                )
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder aktivitet som vi ikke henter fra annet system`() {
            mockVedtakTilsynBarn(vedtaksperiode.copy(aktivitet = AktivitetType.REELL_ARBEIDSSØKER))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `gir ingen treff hvis man har 2 aktiviteter som løper innenfor en periode der begge delvis matcher`() {
            mockHentAktiviteter(
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.fom.plusDays(1)),
                aktivitetArenaDto(fom = vedtaksperiode.tom.minusDays(1), tom = vedtaksperiode.tom.minusDays(1)),
            )

            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                val kontroll = Kontroll(ÅrsakKontroll.INGEN_TREFF)
                assertThat(this.endringAktivitet()).containsExactly(kontroll)
                assertIngenEndringForMålgrupper()
            }
        }
    }

    @Test
    fun `endring i både målgruppe og aktivitet`() {
        val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.fom)
        mockHentYtelser(ytelse)

        val aktivitet = aktivitetArenaDto(fom = vedtaksperiode.fom.plusDays(1), tom = vedtaksperiode.tom)
        mockHentAktiviteter(aktivitet)

        with(opprettOppfølging()) {
            assertThat(this).isNotNull
            assertThat(this.endringMålgruppe()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = ytelse.tom))
            assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = aktivitet.fom))
        }
    }

    @Nested
    inner class HarLagretOppfølgingFraFør {
        val ignoreres =
            Kontrollert(saksbehandler = "saksbehandler", utfall = KontrollertUtfall.IGNORERES, kommentar = "")

        @Test
        fun `skal lagre ny hvis det finnes en fra før men som ikke er kontrollert`() {
            val førsteOppfølging = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull

            assertThat(oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)).isNotNull
        }

        @Test
        fun `skal lagre på nytt hvis dataen endret seg`() {
            val førsteOppfølging = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()))
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)).isNotNull
        }

        @Test
        fun `skal lagre på nytt hvis forrige ble håndtert og dataen endret seg`() {
            val førsteOppfølging = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(
                    kontrollert = ignoreres.copy(utfall = KontrollertUtfall.HÅNDTERT),
                    data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()),
                )
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)).isNotNull()
        }

        @Test
        fun `skal lagre på nytt hvis forrige skal ignoreres og dataen endret seg`() {
            val førsteOppfølging = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(
                    kontrollert = ignoreres,
                    data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()),
                )
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)).isNotNull()
        }

        @Test
        fun `skal ikke lagre på nytt hvis forrige skal ignoreres og dataen ikke endret seg`() {
            val førsteOppfølging = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder = førsteOppfølging!!.copy(kontrollert = ignoreres)
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)).isNull()
        }
    }

    // Sara er god i figma
    @Nested
    inner class SjekkAvRegisterAktivitetForInngangsvilkår {
        @BeforeEach
        fun setUp() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
            mockHentYtelser(ytelse)
        }

        @Test
        fun `fom har endret seg i vilkårsperiode sin aktivitet har endret seg, finnes en annen aktivitet som dekker hele perioden`() {
            mockHentAktiviteter(
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(1), id = "1"),
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom),
            )

            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom, kildeId = "1"))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                val kontroll = Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = vedtaksperiode.tom.minusDays(1))
                assertThat(this.endringAktivitet()).containsExactly(kontroll)
                assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `tom har endret seg i vilkårsperiode sin aktivitet har endret seg, finnes en annen aktivitet som dekker hele perioden`() {
            mockHentAktiviteter(
                aktivitetArenaDto(fom = vedtaksperiode.fom.plusDays(1), tom = vedtaksperiode.tom, id = "1"),
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom),
            )

            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom, kildeId = "1"))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                val kontroll = Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = vedtaksperiode.fom.plusDays(1))
                assertThat(this.endringAktivitet()).containsExactly(kontroll)
                assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal ikke gi doble treff av samme dato hvis det finnes annen aktivitet i registeret som dekker perioden`() {
            mockHentAktiviteter(
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(1), id = "1"),
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(1)),
            )

            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom, kildeId = "1"))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                val kontroll = Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = vedtaksperiode.tom.minusDays(1))
                assertThat(this.endringAktivitet()).containsExactly(kontroll)
                assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal ikke gi treff hvis endring i registeraktivitet ikke påvirker overlappsperiode`() {
            val vilkårTom = vedtaksperiode.tom.plusMonths(4)
            mockHentAktiviteter(
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vilkårTom.minusMonths(1), id = "1"),
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(1)),
            )
            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vilkårTom, kildeId = "1"))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `finner ikke registeraktivitet`() {
            val vilkårTom = vedtaksperiode.tom.plusMonths(4)
            mockHentAktiviteter(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom))
            every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns
                listOf(aktivitet(fom = vedtaksperiode.fom, tom = vilkårTom, kildeId = "1"))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                val kontroll = Kontroll(ÅrsakKontroll.FINNER_IKKE_REGISTERAKTIVITET)
                assertThat(this.endringAktivitet()).containsExactly(kontroll)
                assertIngenEndringForMålgrupper()
            }
        }
    }

    private fun opprettOppfølging(): Oppfølging? = oppfølgingOpprettKontrollerService.opprettOppfølging(behandling.id)

    private fun Oppfølging?.assertIngenEndringForMålgrupper() = assertThat(endringMålgruppe()).isEmpty()

    private fun Oppfølging?.assertIngenEndringForAktiviteter() = assertThat(endringAktivitet()).isEmpty()

    private fun Oppfølging?.endringAktivitet(): List<Kontroll> = endringer<AktivitetType>()

    private fun Oppfølging?.endringMålgruppe(): List<Kontroll> = endringer<MålgruppeType>()

    private inline fun <reified TYPE : VilkårperiodeType> Oppfølging?.endringer(): List<Kontroll> =
        this?.let {
            it.data.perioderTilKontroll
                .filter { it.type is TYPE }
                .flatMap { periode -> periode.endringer }
        } ?: emptyList()

    private fun mockVedtakTilsynBarn(vararg vedtaksperioder: Vedtaksperiode) {
        every { vedtakRepository.findByIdOrThrow(behandling.id) } returns
            innvilgetVedtak(
                vedtaksperioder = vedtaksperioder.toList(),
            )
    }

    private fun mockHentYtelser(vararg perioder: YtelsePeriode) {
        every {
            ytelseService.hentYtelser(any(), any(), any(), any())
        } returns ytelsePerioderDto(perioder = perioder.toList())
    }

    private fun mockHentAktiviteter(vararg perioder: AktivitetArenaDto) {
        every {
            registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any())
        } returns perioder.toList()
    }
}
