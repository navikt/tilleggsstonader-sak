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
    val oppfølgningRepository = mockk<OppfølgningRepository>()

    val oppfølgingService =
        OppfølgingService(
            behandlingRepository = behandlingRepository,
            stønadsperiodeService = stønadsperiodeService,
            registerAktivitetService = registerAktivitetService,
            ytelseService = ytelseService,
            fagsakService = fagsakService,
            taskService = taskService,
            oppfølgningRepository = oppfølgningRepository,
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
        every { behandlingRepository.finnGjeldendeIverksatteBehandlinger() } returns listOf(behandling)
        every { fagsakService.hentMetadata(any()) } answers {
            val fagsakIds = firstArg<List<FagsakId>>()
            fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
        }

        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()
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

            assertThat(oppfølgingService.hentBehandlingerForOppfølging()).isEmpty()
        }

        @Test
        fun `skal finne treff hvis ytelsen slutter tidligere`() {
            val ytelse = periodeAAP(fom = stønadsperiode.fom, tom = stønadsperiode.tom.minusDays(5))
            every { ytelseService.hentYtelseForGrunnlag(any(), any(), any(), any()) } returns
                ytelsePerioderDto(perioder = listOf(ytelse))
            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
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

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
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

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
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

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
                assertThat(this.endringMålgruppe()).containsExactly(Kontroll(ÅrsakKontroll.INGEN_TREFF))
                this.assertIngenEndringForAktiviteter()
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

            assertThat(oppfølgingService.hentBehandlingerForOppfølging()).isEmpty()
        }

        @Test
        fun `skal finne treff hvis aktiviteten slutter tidligere`() {
            val tom = stønadsperiode.tom.minusDays(5)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = tom))

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
                assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = tom))
                this.assertIngenEndringForMålgrupper()
            }
        }

        @Test
        fun `skal finne treff hvis aktiviteten begynner senere`() {
            val fom = stønadsperiode.fom.plusDays(3)
            every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
                listOf(aktivitetArenaDto(fom = fom, tom = stønadsperiode.tom))

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
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

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
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

            with(oppfølgingService.hentBehandlingerForOppfølging()) {
                assertThat(this).hasSize(1)
                assertThat(this.endringAktivitet()).containsExactly(
                    Kontroll(ÅrsakKontroll.INGEN_TREFF),
                    Kontroll(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE),
                )
                this.assertIngenEndringForMålgrupper()
            }
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

        with(oppfølgingService.hentBehandlingerForOppfølging()) {
            assertThat(this).hasSize(1)
            assertThat(this.endringMålgruppe()).containsExactly(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = ytelse.tom))
            assertThat(this.endringAktivitet()).containsExactly(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = aktivitet.fom))
        }
    }

    private fun List<BehandlingForOppfølgingDto>.assertIngenEndringForMålgrupper() =
        assertThat(endringMålgruppe().map { it.årsak }).containsExactly(ÅrsakKontroll.INGEN_ENDRING)

    private fun List<BehandlingForOppfølgingDto>.assertIngenEndringForAktiviteter() =
        assertThat(endringAktivitet().map { it.årsak }).containsExactly(ÅrsakKontroll.INGEN_ENDRING)

    private fun List<BehandlingForOppfølgingDto>.endringAktivitet(): List<Kontroll> =
        this.flatMap { it.stønadsperioderForKontroll.flatMap { stønadsperiode -> stønadsperiode.endringAktivitet } }

    private fun List<BehandlingForOppfølgingDto>.endringMålgruppe(): List<Kontroll> =
        this.flatMap { it.stønadsperioderForKontroll.flatMap { stønadsperiode -> stønadsperiode.endringMålgruppe } }
}
