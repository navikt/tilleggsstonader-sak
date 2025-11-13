package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeDagligReiseTso(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val typeDagligReise: TypeDagligReise,
) : Periode<LocalDate>,
    DetaljertVedtaksperiode,
    Mergeable<LocalDate, DetaljertVedtaksperiodeDagligReiseTso> {
    init {
        validatePeriode()
    }

    override fun merge(other: DetaljertVedtaksperiodeDagligReiseTso): DetaljertVedtaksperiodeDagligReiseTso = this.copy(tom = other.tom)

    fun erLikOgOverlapperEllerPåfølgesAv(other: DetaljertVedtaksperiodeDagligReiseTso): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.typeDagligReise == other.typeDagligReise
        return erLik && this.overlapperEllerPåfølgesAv(other)
    }
}

fun List<DetaljertVedtaksperiodeDagligReiseTso>.sorterOgMergeSammenhengende() =
    this.sorted().mergeSammenhengende { p1, p2 -> p1.erLikOgOverlapperEllerPåfølgesAv(p2) }
