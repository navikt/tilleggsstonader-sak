package no.nav.tilleggsstonader.sak.vedtak.flytting

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.AvslagFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.OpphørFlyttingRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class FlyttingBeregnYtelseStegTest {
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val beregningService = mockk<FlyttingBeregningService>()
    private val steg =
        FlyttingBeregnYtelseSteg(
            beregningService = beregningService,
            beregningsplanUtleder = mockk<BeregningsplanUtleder>(),
            opphørValideringService = mockk<OpphørValideringService>(relaxed = true),
            vedtakRepository = vedtakRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            simuleringService = mockk<SimuleringService>(relaxed = true),
        )

    @Test
    fun `avslag lagres uten å opprette tilkjent ytelse`() {
        val vedtak = slot<GeneriskVedtak<*>>()
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))
        every { vedtakRepository.insert(capture(vedtak)) } answers { firstArg() }

        steg.utførSteg(
            saksbehandling = behandling,
            data =
                AvslagFlyttingDto(
                    årsakerAvslag = listOf(ÅrsakAvslag.LØNN_I_TILTAK),
                    begrunnelse = "begrunnelse",
                ),
        )

        assertThat(vedtak.captured.type).isEqualTo(TypeVedtak.AVSLAG)
        assertThat(vedtak.captured.data)
            .isEqualTo(AvslagFlytting(årsaker = listOf(ÅrsakAvslag.LØNN_I_TILTAK), begrunnelse = "begrunnelse"))
        verify(exactly = 0) { tilkjentYtelseService.lagreTilkjentYtelse(any(), any()) }
    }

    @Test
    fun `opphør avvises for førstegangsbehandling`() {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO))

        assertThatThrownBy {
            steg.utførSteg(
                saksbehandling = behandling,
                data = opphørRequest(opphørsdato = 1 januar 2025),
            )
        }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling")
    }

    @Test
    fun `opphør avvises når opphørsdato mangler`() {
        val behandling =
            saksbehandling(
                type = BehandlingType.REVURDERING,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
                fagsak = fagsak(stønadstype = Stønadstype.FLYTTING_TSO),
            )

        assertThatThrownBy {
            steg.utførSteg(
                saksbehandling = behandling,
                data = opphørRequest(opphørsdato = null),
            )
        }.hasMessage("Opphørsdato er ikke satt")
    }

    @ParameterizedTest
    @EnumSource(value = Stønadstype::class, names = ["FLYTTING_TSO", "FLYTTING_TSR"])
    fun `opphør avkorter forrige vedtaksperiode og lagrer beregningsresultat og andeler`(stønadstype: Stønadstype) {
        val forrigeBehandlingId = BehandlingId.random()
        val behandling =
            saksbehandling(
                type = BehandlingType.REVURDERING,
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
                fagsak = fagsak(stønadstype = stønadstype),
            )
        val forrigeVedtaksperiode = vedtaksperiode(1 januar 2025, 28 februar 2025)
        val forrigeVedtak =
            GeneriskVedtak(
                behandlingId = forrigeBehandlingId,
                data =
                    InnvilgelseFlytting(
                        vedtaksperioder = listOf(forrigeVedtaksperiode),
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                        beregningsresultat = BeregningsresultatFlytting(emptyList()),
                    ),
                gitVersjon = null,
                tidligsteEndring = null,
            )
        val beregningsresultat =
            BeregningsresultatFlytting(
                listOf(
                    BeregningsresultatFlyttevilkår(
                        fom = 1 januar 2025,
                        tom = 31 januar 2025,
                        beløp = BigDecimal("100"),
                        målgruppe = forrigeVedtaksperiode.målgruppe,
                        grunnlag =
                            BeregningsgrunnlagFlyttebyrå(
                                tilbud =
                                    listOf(
                                        BeregningsgrunnlagFlyttebyråTilbud("Byrå A", BigDecimal("100"), erValgt = true),
                                        BeregningsgrunnlagFlyttebyråTilbud("Byrå B", BigDecimal("120"), erValgt = false),
                                    ),
                                erUtgifterDokumentert = true,
                            ),
                    ),
                ),
            )
        val opphørsdato = 1 februar 2025
        val lagretVedtak = slot<GeneriskVedtak<*>>()
        every { vedtakRepository.findById(forrigeBehandlingId) } returns Optional.of(forrigeVedtak)
        every { beregningService.beregn(any(), any(), any(), TypeVedtak.OPPHØR) } returns beregningsresultat
        every { vedtakRepository.insert(capture(lagretVedtak)) } answers { firstArg() }

        steg.utførSteg(
            saksbehandling = behandling,
            data = opphørRequest(opphørsdato),
        )

        assertThat(lagretVedtak.captured.type).isEqualTo(TypeVedtak.OPPHØR)
        assertThat(lagretVedtak.captured.opphørsdato).isEqualTo(opphørsdato)
        assertThat(lagretVedtak.captured.data).isInstanceOf(OpphørFlytting::class.java)
        assertThat((lagretVedtak.captured.data as OpphørFlytting).vedtaksperioder.single().tom)
            .isEqualTo(31 januar 2025)
        verify {
            tilkjentYtelseService.lagreTilkjentYtelse(
                behandlingId = behandling.id,
                andeler = match { it.single().utbetalingsdato == 1 januar 2025 },
            )
        }
    }

    private fun opphørRequest(opphørsdato: LocalDate?) =
        OpphørFlyttingRequest(
            årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
            begrunnelse = "Opphørsbegrunnelse",
            opphørsdato = opphørsdato,
        )
}
