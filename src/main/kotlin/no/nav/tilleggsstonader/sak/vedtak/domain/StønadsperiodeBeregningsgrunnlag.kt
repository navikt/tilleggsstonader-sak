package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.VedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

/**
 * Brukes som grunnlag inne i beregningsgrunnlag. Endringer medfører endringer i db-modell.
 */
data class StønadsperiodeBeregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<StønadsperiodeBeregningsgrunnlag> {
    init {
        validatePeriode()
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): StønadsperiodeBeregningsgrunnlag = this.copy(fom = fom, tom = tom)
}

fun Stønadsperiode.tilStønadsperiodeBeregningsgrunnlag() =
    StønadsperiodeBeregningsgrunnlag(
        fom = this.fom,
        tom = this.tom,
        målgruppe = this.målgruppe,
        aktivitet = this.aktivitet,
    )

fun List<Stønadsperiode>.tilSortertStønadsperiodeBeregningsgrunnlag() =
    this
        .map { it.tilStønadsperiodeBeregningsgrunnlag() }
        .sorted()

fun List<StønadsperiodeBeregningsgrunnlag>.slåSammenSammenhengende(): List<StønadsperiodeBeregningsgrunnlag> =
    this.mergeSammenhengende(
        skalMerges = { a, b -> a.påfølgesAv(b) && a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet },
        merge = { a, b -> a.copy(tom = b.tom) },
    )

fun List<Stønadsperiode>.tilVedtaksperiodeBeregingsgrunnlag() = map { VedtaksperiodeBeregningsgrunnlag(it) }
