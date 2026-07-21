package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

interface BeregningsresultatForReiseTilSamling

data class BeregningsresultatOffentligTransport(
    val reiser: List<BeregningsresultatOffentligTransportForSamling>,
) : BeregningsresultatForReiseTilSamling

data class BeregningsresultatOffentligTransportForSamling(
    val reiseId: ReiseId,
    val grunnlag: BeregningsgrunnlagOffentligTransportForSamling,
    val beløp: Int?,
)

data class BeregningsgrunnlagOffentligTransportForSamling(
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
