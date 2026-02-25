package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeDagligReise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val typeAktivtet: TypeAktivitet?,
    val målgruppe: FaktiskMålgruppe,
    val typeDagligReise: TypeDagligReise,
    val stønadstype: Stønadstype,
    val beregningsresultat: List<BeregningsresultatForPeriodeDto>,
) : Periode<LocalDate>,
    DetaljertVedtaksperiode,
    Mergeable<LocalDate, DetaljertVedtaksperiodeDagligReise> {
    init {
        validatePeriode()
    }

    override fun merge(other: DetaljertVedtaksperiodeDagligReise): DetaljertVedtaksperiodeDagligReise =
        this.copy(
            fom = minOf(this.fom, other.fom),
            tom = maxOf(this.tom, other.tom),
            beregningsresultat =
                this.beregningsresultat + other.beregningsresultat,
        )

    fun erLikOgOverlapperEllerPåfølgesAv(other: DetaljertVedtaksperiodeDagligReise): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.typeDagligReise == other.typeDagligReise &&
                this.typeAktivtet == other.typeAktivtet
        return erLik && this.overlapperEllerPåfølgesAv(other)
    }
}

fun List<DetaljertVedtaksperiodeDagligReise>.sorterOgMergeSammenhengendeEllerOverlappende() =
    this.sorted().mergeSammenhengende { p1, p2 -> p1.erLikOgOverlapperEllerPåfølgesAv(p2) }

data class BeregningsresultatForPeriodeMedFomOgTom(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val grunnlag: BeregningsgrunnlagOffentligTransport,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val fraTidligereVedtak: Boolean = false,
) : Periode<LocalDate>,
    Mergeable<LocalDate, BeregningsresultatForPeriodeMedFomOgTom> {
    override fun merge(other: BeregningsresultatForPeriodeMedFomOgTom): BeregningsresultatForPeriodeMedFomOgTom = this.copy(
        fom = minOf(this.fom, other.fom),
        tom = maxOf(this.tom, other.tom),
    )
}

fun BeregningsresultatForPeriode.tilBeregningsresultatForPeriodeMedFomOgTom() =
    BeregningsresultatForPeriodeMedFomOgTom(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        grunnlag = grunnlag,
        beløp = beløp,
        billettdetaljer = billettdetaljer,
        fraTidligereVedtak = fraTidligereVedtak,
    )
