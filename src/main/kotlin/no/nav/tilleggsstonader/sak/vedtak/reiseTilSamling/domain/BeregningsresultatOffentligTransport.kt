package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate

// TODO burde fil bytte navn til BeregningReiseTilSamling?

data class BeregningReiseTilSamling(
    val offentligTransport: List<BeregningsresultatOffentligTransport>,
    val privatBil: List<BeregningsresultatPrivatBil>,
)

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
    val brukersNavKontor: String?,
)

data class BeregningsgrunnlagOffentligTransportForSamling(
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    val brukersNavKontor: String?,
)

data class VedtaksperiodeGrunnlag(
    val id: VedtaksperiodeId,
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) {
    constructor(
        vedtaksperiode: Vedtaksperiode,
    ) : this(
        id = vedtaksperiode.id,
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
        målgruppe = vedtaksperiode.målgruppe,
        aktivitet = vedtaksperiode.aktivitet,
    )
}
