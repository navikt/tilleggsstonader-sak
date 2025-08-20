package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BehandlingsoversiktServiceTest {
    val fagsakService = mockk<FagsakService>()
    val vedtakService = mockk<VedtakService>()
    val behandlingRepository = mockk<BehandlingRepository>()

    val service =
        BehandlingsoversiktService(
            fagsakService = fagsakService,
            behandlingRepository = behandlingRepository,
            vedtakService = vedtakService,
        )

    val fagsak = fagsak()
    val behandling = behandling(fagsak)

    val vedtaksperiode =
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 3, 1),
            tom = LocalDate.of(2024, 3, 14),
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    val vedtaksperiodeGrunnlag =
        VedtaksperiodeGrunnlag(
            VedtaksperiodeBeregning(
                fom = LocalDate.of(2024, 3, 1),
                tom = LocalDate.of(2024, 3, 13),
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
            emptyList(),
            10,
        )
    val vedtaksperiodeGrunnlag2 =
        VedtaksperiodeGrunnlag(
            VedtaksperiodeBeregning(
                fom = LocalDate.of(2024, 3, 2),
                tom = LocalDate.of(2024, 3, 14),
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
            emptyList(),
            10,
        )
    val beregningsresultatForMåned =
        beregningsresultatForMåned(
            YearMonth.of(2024, 3),
            vedtaksperioder = listOf(vedtaksperiodeGrunnlag, vedtaksperiodeGrunnlag2),
        )
    val beregningsresultat = BeregningsresultatTilsynBarn(perioder = listOf(beregningsresultatForMåned))

    var vedtak: Vedtak =
        innvilgetVedtak(
            beregningsresultat = beregningsresultat,
            behandlingId = behandling.id,
            vedtaksperioder = listOf(vedtaksperiode),
        )

    @BeforeEach
    fun setUp() {
        every { fagsakService.finnFagsakerForFagsakPersonId(fagsak.fagsakPersonId) } returns
            Fagsaker(
                barnetilsyn = fagsak,
                læremidler = null,
                boutgifter = null,
                dagligReiseTso = null,
                dagligReiseTsr = null,
            )
        every { behandlingRepository.findByFagsakId(fagsakId = fagsak.id) } returns listOf(behandling)
        every { fagsakService.erLøpende(fagsak.id) } returns true
        every { vedtakService.hentVedtak(any()) } answers { vedtak }
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
        fun `ved et opphør så skal vedtaksperiode være tom og opphørsdato skal være satt`() {
            every { behandlingRepository.findByFagsakId(fagsak.id) } returns
                listOf(behandling.copy(type = BehandlingType.REVURDERING))
            vedtak =
                opphørVedtak(
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "opphør nuuuuu",
                    opphørsdato = LocalDate.of(2024, 4, 1),
                )

            val oversikt = service.hentOversikt(fagsak.fagsakPersonId)

            val behandling = oversikt.tilsynBarn!!.behandlinger.single()
            assertThat(behandling.vedtaksperiode?.fom).isNull()
            assertThat(behandling.vedtaksperiode?.tom).isNull()
        }
    }
}
