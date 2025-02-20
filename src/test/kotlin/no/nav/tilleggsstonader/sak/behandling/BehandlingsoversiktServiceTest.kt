package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BehandlingsoversiktServiceTest {
    val fagsakService = mockk<FagsakService>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val vedtakRepository = mockk<VedtakRepository>()

    val service =
        BehandlingsoversiktService(
            fagsakService = fagsakService,
            behandlingRepository = behandlingRepository,
            vedtakRepository = vedtakRepository,
        )

    val fagsak = fagsak()
    val behandling = behandling(fagsak)

    @BeforeEach
    fun setUp() {
        every { fagsakService.finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId) } returns
            Fagsaker(
                barnetilsyn = fagsak,
                læremidler = null,
            )
        every { behandlingRepository.findByFagsakId(fagsakId = fagsak.id) } returns listOf(behandling)
        every { fagsakService.erLøpende(fagsak.id) } returns true
        mockVedtakRepository()
    }

    @Test
    fun `skal mappe behandling`() {
        val oversikt = service.hentOversikt(fagsak.fagsakPersonId)

        assertThat(oversikt.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(oversikt.tilsynBarn!!.behandlinger).hasSize(1)
    }

    @Nested
    inner class Vedtaksperiode {
        @Test
        fun `vedtaksperioden skal mappes til min og max av vedtaksperiode fra beregningsresultatet`() {
            val oversikt = service.hentOversikt(fagsak.fagsakPersonId)

            val behandling = oversikt.tilsynBarn!!.behandlinger.single()
            assertThat(behandling.vedtaksperiode?.fom).isEqualTo(LocalDate.of(2024, 3, 1))
            assertThat(behandling.vedtaksperiode?.tom).isEqualTo(LocalDate.of(2024, 3, 14))
        }

        @Test
        fun `ved et opphør så skal revurderFra bruker som vedtaksperiode dersom revurderFra er etter vedtaksperioder som er igjen`() {
            every { behandlingRepository.findByFagsakId(fagsak.id) } returns
                listOf(behandling.copy(type = BehandlingType.REVURDERING, revurderFra = LocalDate.of(2024, 4, 1)))

            val oversikt = service.hentOversikt(fagsak.fagsakPersonId)

            val behandling = oversikt.tilsynBarn!!.behandlinger.single()
            assertThat(behandling.vedtaksperiode?.fom).isNull()
            assertThat(behandling.vedtaksperiode?.tom).isNull()
        }
    }

    private fun mockVedtakRepository() {
        val stønadsperiodeGrunnlag =
            StønadsperiodeGrunnlag(
                VedtaksperiodeBeregning(
                    fom = LocalDate.of(2024, 3, 1),
                    tom = LocalDate.of(2024, 3, 13),
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                ),
                emptyList(),
                10,
            )
        val stønadsperiodeGrunnlag2 =
            StønadsperiodeGrunnlag(
                VedtaksperiodeBeregning(
                    fom = LocalDate.of(2024, 3, 2),
                    tom = LocalDate.of(2024, 3, 14),
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                ),
                emptyList(),
                10,
            )
        val beregningsresultatForMåned =
            beregningsresultatForMåned(
                YearMonth.of(2024, 3),
                stønadsperioder = listOf(stønadsperiodeGrunnlag, stønadsperiodeGrunnlag2),
            )
        val beregningsresultat = BeregningsresultatTilsynBarn(perioder = listOf(beregningsresultatForMåned))

        every { vedtakRepository.findAllById(any()) } returns
            listOf(
                innvilgetVedtak(beregningsresultat = beregningsresultat, behandlingId = behandling.id),
            )
    }
}
