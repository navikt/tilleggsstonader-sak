package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
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
    val beregningsDetaljer: List<BeregningsresultatForPeriodeDto>?,
//    val beregningsDetaljer: BeregningsresultatDagligReiseDto?
) : Periode<LocalDate>,
    DetaljertVedtaksperiode,
    Mergeable<LocalDate, DetaljertVedtaksperiodeDagligReise> {
    init {
        validatePeriode()
    }

    override fun merge(other: DetaljertVedtaksperiodeDagligReise): DetaljertVedtaksperiodeDagligReise = this.copy(tom = other.tom)

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
