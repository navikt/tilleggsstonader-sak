package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatReiseTilSamling(
    val offentligTransport: List<BeregningsresultatOffentligTransport>,
    val privatBil: List<BeregningsresultatPrivatBil>,
) {
    fun alleSamlinger(): Collection<BeregningsresultatForSamling> = offentligTransport + privatBil
}

sealed interface BeregningsresultatForSamling {
    val reiseId: ReiseId
    val grunnlag: BeregningsgrunnlagForSamling
}

data class BeregningsresultatOffentligTransport(
    override val reiseId: ReiseId,
    override val grunnlag: BeregningsgrunnlagOffentligTransportForSamling,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId?,
    /**
     * Markerer at denne reisen er kopiert uendret fra forrige iverksatte vedtak ved revurdering,
     * altså at den ikke ligger innenfor beregningsplanens `beregnFra` og derfor ikke er reberegnet.
     */
    val fraTidligereVedtak: Boolean = false,
) : BeregningsresultatForSamling

data class BeregningsresultatPrivatBil(
    override val reiseId: ReiseId,
    override val grunnlag: BeregningsgrunnlagPrivatBilForSamling,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId?,
    /**
     * Markerer at denne reisen er kopiert uendret fra forrige iverksatte vedtak ved revurdering,
     * altså at den ikke ligger innenfor beregningsplanens `beregnFra` og derfor ikke er reberegnet.
     */
    val fraTidligereVedtak: Boolean = false,
) : BeregningsresultatForSamling

interface BeregningsgrunnlagForSamling {
    val fom: LocalDate
    val tom: LocalDate
    val vedtaksperioder: List<VedtaksperiodeGrunnlag>
}

data class BeregningsgrunnlagPrivatBilForSamling(
    val adresse: String?,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val sats: BigDecimal,
    val totalReiseavstand: BigDecimal,
    val bompenger: BigDecimal?,
    val fergekostnad: BigDecimal?,
    val parkering: BigDecimal?,
    val piggdekkavgift: BigDecimal?,
    override val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    val brukersNavKontor: String?,
) : BeregningsgrunnlagForSamling

data class BeregningsgrunnlagOffentligTransportForSamling(
    val adresse: String?,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    val brukersNavKontor: String?,
) : BeregningsgrunnlagForSamling

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
