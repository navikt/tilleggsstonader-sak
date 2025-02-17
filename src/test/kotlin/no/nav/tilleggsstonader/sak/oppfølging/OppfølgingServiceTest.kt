package no.nav.tilleggsstonader.sak.oppfølging

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppfølgingServiceTest {
    val behandling =
        behandling(
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = LocalDateTime.now(),
        )

    val behandlingRepository =
        mockk<BehandlingRepository>().apply {
            val repository = this
            every { repository.finnGjeldendeIverksatteBehandlinger() } returns listOf(behandling)
        }
    val fagsakService =
        mockk<FagsakService>().apply {
            val service = this
            every { service.hentMetadata(any()) } answers {
                val fagsakIds = firstArg<List<FagsakId>>()
                fagsakIds.associateWith { FagsakMetadata(it, 1, Stønadstype.BARNETILSYN, "1") }
            }
        }
    val stønadsperiodeService = mockk<StønadsperiodeService>()
    val registerAktivitetService = mockk<RegisterAktivitetService>()

    val oppfølgingService =
        OppfølgingService(
            behandlingRepository = behandlingRepository,
            stønadsperiodeService = stønadsperiodeService,
            registerAktivitetService = registerAktivitetService,
            fagsakService = fagsakService,
        )

    val stønadsperiode =
        stønadsperiode(
            behandling.id,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        )

    @Test
    fun `skal ikke finne treff hvis aktiviteten blitt forlenget`() {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()

        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom.plusMonths(1)))

        assertThat(oppfølgingService.hentBehandlingerForOppfølging()).isEmpty()
    }

    @Test
    fun `skal finne treff hvis aktiviteten slutter tidligere`() {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()

        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom.minusDays(5)))

        with(oppfølgingService.hentBehandlingerForOppfølging()) {
            assertThat(this).hasSize(1)
            assertThat(this.årsaker()).containsExactly(ÅrsakKontroll.TOM_ENDRET)
        }
    }

    @Test
    fun `skal finne treff hvis aktiviteten begynner senere`() {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()

        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitetArenaDto(fom = stønadsperiode.fom.plusDays(3), tom = stønadsperiode.tom))

        with(oppfølgingService.hentBehandlingerForOppfølging()) {
            assertThat(this).hasSize(1)
            assertThat(this.årsaker()).containsExactly(ÅrsakKontroll.FOM_ENDRET)
        }
    }

    @Test
    fun `skal finne treff hvis aktiviteten begynner senere og slutter tidligere`() {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()

        val aktivitet = aktivitetArenaDto(fom = stønadsperiode.fom.plusDays(3), tom = stønadsperiode.tom.minusDays(3))
        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitet)

        with(oppfølgingService.hentBehandlingerForOppfølging()) {
            assertThat(this).hasSize(1)
            assertThat(this.årsaker()).containsExactly(ÅrsakKontroll.FOM_ENDRET, ÅrsakKontroll.TOM_ENDRET)
        }
    }

    @Test
    fun `skal finne treff hvis man har en aktivitet men er av feil type`() {
        every { stønadsperiodeService.hentStønadsperioder(behandling.id) } returns
            listOf(stønadsperiode).tilSortertDto()

        val aktivitet =
            aktivitetArenaDto(fom = stønadsperiode.fom, tom = stønadsperiode.tom.minusDays(5), erUtdanning = true)
        every { registerAktivitetService.hentAktiviteterForGrunnlagsdata(any(), any(), any()) } returns
            listOf(aktivitet)

        with(oppfølgingService.hentBehandlingerForOppfølging()) {
            assertThat(this).hasSize(1)
            assertThat(this.årsaker()).containsExactly(ÅrsakKontroll.INGEN_MATCH)
        }
    }

    private fun List<BehandlingForOppfølgingDto>.årsaker(): List<ÅrsakKontroll> =
        this.flatMap { it.stønadsperioderForKontroll.flatMap { stønadsperiode -> stønadsperiode.årsaker } }
}
