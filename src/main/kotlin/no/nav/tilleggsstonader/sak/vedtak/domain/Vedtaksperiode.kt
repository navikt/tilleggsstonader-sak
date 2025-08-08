package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

interface PeriodeMedId : Periode<LocalDate> {
    val id: UUID

    fun kopier(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeMedId
}

data class Vedtaksperiode(
    override val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<Vedtaksperiode>,
    PeriodeMedId,
    Mergeable<LocalDate, Vedtaksperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)

    override fun kopier(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)

    fun erSammenhengendeMedLikMålgruppeOgAktivitet(other: Vedtaksperiode): Boolean =
        this.målgruppe == other.målgruppe &&
            this.aktivitet == other.aktivitet &&
            this.overlapperEllerPåfølgesAv(other)

    override fun merge(other: Vedtaksperiode): Vedtaksperiode {
        require(this.målgruppe == other.målgruppe) {
            "Kan ikke slå sammen vedtaksperioder med ulike målgrupper: $this og $other"
        }
        require(this.aktivitet == other.aktivitet) {
            "Kan ikke slå sammen vedtaksperioder med ulike aktiviteter: $this og $other"
        }
        return this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))
    }
}

fun List<Vedtaksperiode>.tilVedtaksperiodeBeregning() =
    map {
        VedtaksperiodeBeregning(
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }

fun List<Vedtaksperiode>.mergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { v1, v2 -> v1.erSammenhengendeMedLikMålgruppeOgAktivitet(v2) }
