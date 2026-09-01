package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningReiseOppstartAvslutningHjemreise(
    val offentligTransport: List<BeregningsresultatOffentligTransport>,
    val privatBil: List<BeregningsresultatPrivatBil>,
)

data class BeregningsresultatOffentligTransport(
    val reiseId: ReiseId,
    val grunnlag: BeregningsgrunnlagOffentligTransport,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
)

data class BeregningsresultatPrivatBil(
    val reiseId: ReiseId,
    val grunnlag: BeregningsgrunnlagPrivatBil,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
)

data class BeregningsgrunnlagPrivatBil(
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: BigDecimal,
    val totaltReiseavstand: BigDecimal,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
)

data class BeregningsgrunnlagOffentligTransport(
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
)

data class VedtaksperiodeGrunnlag(
    val id: VedtaksperiodeId,
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(
        vedtaksperiode: Vedtaksperiode,
    ) : this(
        id = vedtaksperiode.id,
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
    )
}
