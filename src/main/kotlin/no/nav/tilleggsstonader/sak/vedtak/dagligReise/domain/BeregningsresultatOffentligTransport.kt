package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class BeregningsresultatOffentligTransport(
    val reiser: List<BeregningsresultatForReise>,
)

data class BeregningsresultatForReise(
    val reiseId: UUID,
    val perioder: List<BeregningsresultatForPeriode>,
)

data class BeregningsresultatForPeriode(
    val grunnlag: BeregningsgrunnlagOffentligTransport,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
)

data class BeregningsgrunnlagOffentligTransport(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val antallReisedagerPerUke: Int,
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    val antallReisedager: Int,
) : Periode<LocalDate>

data class VedtaksperiodeGrunnlag(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val antallReisedagerIVedtaksperioden: Int,
) {
    constructor(vedtaksperiode: Vedtaksperiode, antallReisedager: Int) : this(
        id = vedtaksperiode.id,
        fom = vedtaksperiode.fom,
        tom = vedtaksperiode.tom,
        målgruppe = vedtaksperiode.målgruppe,
        aktivitet = vedtaksperiode.aktivitet,
        antallReisedagerIVedtaksperioden = antallReisedager,
    )
}
