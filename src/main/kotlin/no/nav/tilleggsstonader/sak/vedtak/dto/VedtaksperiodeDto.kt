package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.VedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class VedtaksperiodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun List<VedtaksperiodeDto>.tilVedtaksperiodeBeregingsgrunnlag() = map { VedtaksperiodeBeregningsgrunnlag(it) }
