package no.nav.tilleggsstonader.sak.oppfølging

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.OppfølgingRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class OppfølgingServiceTest {
    val behandling =
        behandling(
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = LocalDateTime.now(),
        )

    val behandlingRepository = mockk<BehandlingRepository>()
    val fagsakService = mockk<FagsakService>()
    val stønadsperiodeService = mockk<StønadsperiodeService>()
    val registerAktivitetService = mockk<RegisterAktivitetService>()
    val ytelseService = mockk<YtelseService>()
    val taskService = mockk<TaskService>()
    val oppfølgingRepository = spyk(OppfølgingRepositoryFake())

    val oppfølgingService =
        OppfølgingService(
            behandlingRepository = behandlingRepository,
            stønadsperiodeService = stønadsperiodeService,
            registerAktivitetService = registerAktivitetService,
            ytelseService = ytelseService,
            fagsakService = fagsakService,
            taskService = taskService,
            oppfølgingRepository = oppfølgingRepository,
        )

    val stønadsperiode =
        stønadsperiode(
            behandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        )

    @BeforeEach
    fun setUp() {
        oppfølgingRepository.deleteAll()
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { fagsakService.hentMetadata(any()) } answers {
            val fagsakIds = firstArg<List<FagsakId>>()
            fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
        }

        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()
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
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom))
        }

        @Test
        fun `skal ikke finne treff hvis ytelsen blitt forlenget`() {
            val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = stønadsperiode.tom.plusMonths(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis ytelsen slutter tidligere`() {
            val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = stønadsperiode.tom.minusDays(5))
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
            val ytelse = periodeAAP(fom = stønadsperiode.fom.plusDays(3), tom = stønadsperiode.tom)
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
            val ytelse = periodeAAP(fom = stønadsperiode.fom.plusDays(3), tom = stønadsperiode.tom.minusDays(3))
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
            val ytelse = periodeEnsligForsørger(fom = stønadsperiode.fom, tom = stønadsperiode.tom)
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
            every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
                listOf(stønadsperiode.copy(målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE)).tilSortertDto()
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = emptyList())

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal ikke finne treff hvis det gjelder gjelder endring i AAP som gjelder etter neste måned`() {
            val tom = YearMonth.now().plusMonths(2).atDay(2)
            every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
                listOf(stønadsperiode.copy(tom = tom)).tilSortertDto()
            val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = tom.minusDays(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = tom))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis det gjelder gjelder endring i AAP som gjelder neste måned`() {
            val tom = YearMonth.now().plusMonths(2).atDay(1)
            every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
                listOf(stønadsperiode.copy(tom = tom)).tilSortertDto()
            val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = tom.minusDays(1))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = tom))

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
                ytelsePerioderDto(perioder = listOf(periodeAAP(fom = stønadsperiode.fom, tom = stønadsperiode.tom)))
        }

        @Test
        fun `skal ikke finne treff hvis aktiviteten blitt forlenget`() {
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom.plusMonths(1)))

            assertThat(opprettOppfølging()).isNull()
        }

        @Test
        fun `skal finne treff hvis aktiviteten slutter tidligere`() {
            val tom = stønadsperiode.tom.minusDays(5)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere`() {
            val fom = stønadsperiode.fom.plusDays(3)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = fom, tom = stønadsperiode.tom))

            with(opprettOppfølging()) {
                assertThat(this).isNotNull
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = fom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere og slutter tidligere`() {
            val aktivitet =
                aktivitetArenaDto(fom = stønadsperiode.fom.plusDays(3), tom = stønadsperiode.tom.minusDays(3))
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
                aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom, erUtdanning = true)
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
            every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
                listOf(stønadsperiode.copy(aktivitet = AktivitetType.REELL_ARBEIDSSØKER)).tilSortertDto()

            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                emptyList()

            assertThat(opprettOppfølging()).isNull()
        }
    }

    @Test
    fun `endring i både målgruppe og aktivitet`() {
        val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = stønadsperiode.tom.minusDays(1))
        every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
            ytelsePerioderDto(perioder = listOf(ytelse))
        val aktivitet = aktivitetArenaDto(fom = stønadsperiode.fom.plusDays(1), tom = stønadsperiode.tom)
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
    }

    private fun opprettOppfølging(): Oppfølging? = oppfølgingService.opprettOppfølging(behandling.id)

    private fun Oppfølging?.assertIngenEndringForMålgrupper() =
        assertThat(endringMålgruppe().map { it.årsak }).containsExactly(ÅrsakKontroll.INGEN_ENDRING)

    private fun Oppfølging?.assertIngenEndringForAktiviteter() =
        assertThat(endringAktivitet().map { it.årsak }).containsExactly(ÅrsakKontroll.INGEN_ENDRING)

    private fun Oppfølging?.endringAktivitet(): List<Kontroll> =
        this?.let { it.data.perioderTilKontroll.flatMap { stønadsperiode -> stønadsperiode.endringAktivitet } }
            ?: emptyList()

    private fun Oppfølging?.endringMålgruppe(): List<Kontroll> =
        this?.let { it.data.perioderTilKontroll.flatMap { stønadsperiode -> stønadsperiode.endringMålgruppe } }
            ?: emptyList()
}
