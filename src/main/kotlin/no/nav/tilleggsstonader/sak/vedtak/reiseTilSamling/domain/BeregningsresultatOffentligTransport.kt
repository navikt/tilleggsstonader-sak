package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class BeregningsresultatOffentligTransport(
    val reiseId: ReiseId,
    val grunnlag: BeregningsgrunnlagOffentligTransportForSamling,
    val beløp: BigDecimal,
)

data class BeregningsresultatPrivatBil(
    val reiseId: ReiseId,
    val grunnlag: BeregningsgrunnlagPrivatBilForSamling,
    val beløp: BigDecimal,
)

data class BeregningsgrunnlagPrivatBilForSamling(
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: BigDecimal,
    val totaltReiseavstand: BigDecimal,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
)

data class BeregningsgrunnlagOffentligTransportForSamling(
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
)

data class VedtaksperiodeGrunnlag(
    val id: UUID,
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
