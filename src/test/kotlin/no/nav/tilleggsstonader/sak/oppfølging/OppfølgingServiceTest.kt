package no.nav.tilleggsstonader.sak.oppfølging

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
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
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Disabled
class OppfølgingServiceTest {
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

    val oppfølgingService =
        OppfølgingService(
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
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    @BeforeEach
    fun setUp() {
        oppfølgingRepository.deleteAll()
        every { vilkårperiodeRepository.findByBehandlingIdAndResultat(any(), any()) } returns emptyList()
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { fagsakService.hentMetadata(any()) } answers {
            val fagsakIds = firstArg<List<FagsakId>>()
            fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
        }

        mockVedtakTilsynBarn(vedtaksperiode)
        every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
            ytelsePerioderDto(perioder = emptyList())
        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            emptyList()
    }

    @Nested
    inner class EndringIMålgruppe {
        @BeforeEach
        fun setUp() {
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom))
        }

        @Test
        fun `skal ikke finne treff hvis ytelsen blitt forlenget`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.plusMonths(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis det ikke finnes noen vedtaksperioder`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom.plusDays(3), tom = vedtaksperiode.tom)
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            mockVedtakTilsynBarn()

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis ytelsen slutter tidligere`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(5))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
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
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

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
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

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
        fun `skal ikke finne treff hvis man har en aktivitet men er av feil type`() {
            val ytelse = periodeEnsligForsørger(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe()).containsExactly(Kontroll(ÅrsakKontroll.INGEN_TREFF))
                this.assertIngenEndringForAktiviteter()
            }
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder målgruppe som vi ikke henter fra annet system`() {
            mockVedtakTilsynBarn(vedtaksperiode.copy(målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = emptyList())

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder gjelder endring i AAP som gjelder fra og med siste dagen i neste måneden`() {
            val tom = YearMonth.now().plusMonths(2).atDay(1)
            mockVedtakTilsynBarn(vedtaksperiode.copy(tom = tom))
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = tom.minusDays(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis det gjelder gjelder endring i AAP som gjelder neste måned`() {
            val tom = YearMonth.now().plusMonths(1).atEndOfMonth()
            mockVedtakTilsynBarn(vedtaksperiode.copy(tom = tom))
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = tom.minusDays(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe())
                    .containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom.minusDays(1)))
            }
        }
    }

    @Nested
    inner class EndringIAktivitet {
        @BeforeEach
        fun setUp() {
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)))
        }

        @Test
        fun `skal ikke finne treff hvis aktiviteten blitt forlenget`() {
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.plusMonths(1)))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis aktiviteten slutter tidligere`() {
            val tom = vedtaksperiode.tom.minusDays(5)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere`() {
            val fom = vedtaksperiode.fom.plusDays(3)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = fom, tom = vedtaksperiode.tom))

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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitet)

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
            val aktivitet =
                aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom, erUtdanning = true)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitet)

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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                emptyList()

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `gir ingen treff hvis man har 2 aktiviteter som løper innenfor en periode der begge delvis matcher`() {
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
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
        val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(1))
        every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
            ytelsePerioderDto(perioder = listOf(ytelse))
        val aktivitet = aktivitetArenaDto(fom = vedtaksperiode.fom.plusDays(1), tom = vedtaksperiode.tom)
        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitet)

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
            val førsteOppfølging = oppfølgingService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull

            assertThat(oppfølgingService.opprettOppfølging(behandling.id)).isNotNull
        }

        @Test
        fun `skal lagre på nytt hvis dataen endret seg`() {
            val førsteOppfølging = oppfølgingService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()))
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingService.opprettOppfølging(behandling.id)).isNotNull
        }

        @Test
        fun `skal lagre på nytt hvis forrige ble håndtert og dataen endret seg`() {
            val førsteOppfølging = oppfølgingService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(
                    kontrollert = ignoreres.copy(utfall = KontrollertUtfall.HÅNDTERT),
                    data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()),
                )
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingService.opprettOppfølging(behandling.id)).isNotNull()
        }

        @Test
        fun `skal lagre på nytt hvis forrige skal ignoreres og dataen endret seg`() {
            val førsteOppfølging = oppfølgingService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder =
                førsteOppfølging!!.copy(
                    kontrollert = ignoreres,
                    data = førsteOppfølging.data.copy(perioderTilKontroll = emptyList()),
                )
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingService.opprettOppfølging(behandling.id)).isNotNull()
        }

        @Test
        fun `skal ikke lagre på nytt hvis forrige skal ignoreres og dataen ikke endret seg`() {
            val førsteOppfølging = oppfølgingService.opprettOppfølging(behandling.id)
            assertThat(førsteOppfølging).isNotNull
            val oppfølgingMedFjernedePerioder = førsteOppfølging!!.copy(kontrollert = ignoreres)
            oppfølgingRepository.update(oppfølgingMedFjernedePerioder)

            assertThat(oppfølgingService.opprettOppfølging(behandling.id)).isNull()
        }
    }

    @Nested
    inner class SjekkAvRegisterAktivitetForInngangsvilkår {
        @BeforeEach
        fun setUp() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom)
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
        }

        @Test
        fun `fom har endret seg i vilkårsperiode sin aktivitet har endret seg, finnes en annen aktivitet som dekker hele perioden`() {
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
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
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(
                    aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom),
                )
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

    @Disabled // TODO fikse
    @Nested
    inner class EndringForLæremidlerSomBrukerBeregningsresultat {
        @BeforeEach
        fun setUp() {
            every { fagsakService.hentMetadata(any()) } answers {
                val fagsakIds = firstArg<List<FagsakId>>()
                fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.LÆREMIDLER, "1") }
            }
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom))
        }

        @Test
        fun `skal finne treff hvis ytelsen slutter tidligere`() {
            val ytelse = periodeAAP(fom = vedtaksperiode.fom, tom = vedtaksperiode.tom.minusDays(5))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

            mockVedtakLæremidler(vedtaksperiode)

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringMålgruppe())
                    .containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = ytelse.tom))
                this.assertIngenEndringForAktiviteter()
            }
        }
    }

    @Nested
    inner class IkkeAktiv {
        @Test
        fun `skal ikke kunne redigere en oppfølging som ikke lengre er aktiv`() {
            val oppfølging =
                oppfølgingRepository.insert(
                    Oppfølging(
                        behandlingId = behandling.id,
                        data = OppfølgingData(emptyList()),
                        aktiv = false,
                    ),
                )

            assertThatThrownBy {
                oppfølgingService.kontroller(
                    KontrollerOppfølgingRequest(
                        oppfølging.id,
                        oppfølging.version,
                        KontrollertUtfall.HÅNDTERT,
                        "kommentar",
                    ),
                )
            }.hasMessageContaining("Kan ikke redigere en oppfølging som ikke lengre er aktiv")
        }
    }

    private fun opprettOppfølging(): Oppfølging? = oppfølgingService.opprettOppfølging(behandling.id)

    private fun Oppfølging?.assertIngenEndringForMålgrupper() = assertThat(endringMålgruppe()).isEmpty()

    private fun Oppfølging?.assertIngenEndringForAktiviteter() = assertThat(endringAktivitet()).isEmpty()

    private fun Oppfølging?.endringAktivitet(): List<Kontroll> =
        this?.let { it.data.perioderTilKontroll.flatMap { periode -> periode.endringAktivitet!! } }
            ?: emptyList()

    private fun Oppfølging?.endringMålgruppe(): List<Kontroll> =
        this?.let { it.data.perioderTilKontroll.flatMap { periode -> periode.endringMålgruppe!! } }
            ?: emptyList()

    private fun mockVedtakTilsynBarn(vararg vedtaksperioder: Vedtaksperiode) {
        every { vedtakRepository.findByIdOrThrow(behandling.id) } returns
            innvilgetVedtak(
                vedtaksperioder = vedtaksperioder.toList(),
            )
    }

    private fun mockVedtakLæremidler(vararg perioder: Vedtaksperiode) {
        every { vedtakRepository.findByIdOrThrow(behandling.id) } returns
            LæremidlerTestUtil.innvilgelse(
                beregningsresultat =
                    BeregningsresultatLæremidler(
                        perioder.map {
                            beregningsresultatForMåned(
                                fom = it.fom,
                                tom = it.tom,
                                målgruppe = it.målgruppe,
                                aktivitet = it.aktivitet,
                            )
                        },
                    ),
            )
    }
}
