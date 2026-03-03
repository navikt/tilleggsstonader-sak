package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import java.time.LocalDate

data class DetaljertBeregningsperioderDagligReise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val antallReisedager: Int,
    val stønadstype: Stønadstype,
) : Periode<LocalDate>,
    DetaljertVedtaksperiode,
    Mergeable<LocalDate, DetaljertBeregningsperioderDagligReise> {
    init {
        validatePeriode()
    }

//    data class DetaljertBeregningsperiodeDagligReise(
//        override val fom: LocalDate,
//        override val tom: LocalDate,
//        val prisEnkeltbillett: Int?,
//        val prisSyvdagersbillett: Int?,
//        val pris30dagersbillett: Int?,
//        val beløp: Int,
//        val billettdetaljer: Map<Billettype, Int>,
//        val antallReisedager: Int,
//    ) : Periode<LocalDate>,
//        DetaljertVedtaksperiode,
//        Mergeable<LocalDate, DetaljertVedtaksperiodeDagligReise> {
//        init {
//            validatePeriode()
//        }
//    }

    override fun merge(other: DetaljertBeregningsperioderDagligReise): DetaljertBeregningsperioderDagligReise = this.copy(tom = other.tom)

//    fun erLikOgOverlapperEllerPåfølgesAv(other: DetaljertVedtaksperiodeDagligReise): Boolean {
//        val erLik =
//            this.aktivitet == other.aktivitet &&
//                this.målgruppe == other.målgruppe &&
//                this.typeDagligReise == other.typeDagligReise &&
//                this.typeAktivtet == other.typeAktivtet
//        return erLik && this.overlapperEllerPåfølgesAv(other)
//    }
}

// fun List<DetaljertVedtaksperiodeDagligReise>.sorterOgMergeSammenhengendeEllerOverlappende() =
//    this.sorted().mergeSammenhengende { p1, p2 -> p1.erLikOgOverlapperEllerPåfølgesAv(p2) }
