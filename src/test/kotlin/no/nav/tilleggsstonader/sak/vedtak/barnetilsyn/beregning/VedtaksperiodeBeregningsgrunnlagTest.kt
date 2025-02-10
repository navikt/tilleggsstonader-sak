package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregingsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilVedtaksperiodeBeregingsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeBeregningsgrunnlagTest {
    val fom = LocalDate.of(2025, 1, 1)
    val tom = LocalDate.of(2025, 3, 1)
    val målgruppe = MålgruppeType.AAP
    val aktivitet = AktivitetType.TILTAK

    val vedtaksperiodeBeregningsgrunnlag =
        listOf(
            VedtaksperiodeBeregningsgrunnlag(
                fom = fom,
                tom = tom,
                målgruppe = målgruppe,
                aktivitet = aktivitet,
            ),
        )

    @Test
    fun `mapper VedtaksperiodeDto til VedtaksperiodeBeregningsgrunnlag`() {
        val vedtaksperiodeDto =
            listOf(
                VedtaksperiodeDto(
                    fom = fom,
                    tom = tom,
                    målgruppe = målgruppe,
                    aktivitet = aktivitet,
                ),
            )

        assertThat(vedtaksperiodeDto.tilVedtaksperiodeBeregingsgrunnlag()).isEqualTo(vedtaksperiodeBeregningsgrunnlag)
    }

    @Test
    fun `mapper StønadsperiodeBeregningsgrunnlag til VedtaksperiodeBeregningsgrunnlag`() {
        val stønadsperiodeBeregningsgrunnlag =
            listOf(
                stønadsperiode(
                    behandlingId = BehandlingId.random(),
                    fom = fom,
                    tom = tom,
                    målgruppe = målgruppe,
                    aktivitet = aktivitet,
                ),
            )
        assertThat(stønadsperiodeBeregningsgrunnlag.tilVedtaksperiodeBeregingsgrunnlag()).isEqualTo(
            vedtaksperiodeBeregningsgrunnlag,
        )
    }
}
