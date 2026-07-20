package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

object VedtaksperiodePassAvBarnMapper {
    fun mapTilVedtaksperiode(beregningsresultatForMåned: List<BeregningsresultatForMåned>): List<VedtaksperiodePassAvBarn> =
        beregningsresultatForMåned
            .flatMap { tilVedtaksperioder(it) }
            .sorted()
            .mergeSammenhengende { s1, s2 -> s1.erLikOgPåfølgesAv(s2) }

    private fun tilVedtaksperioder(it: BeregningsresultatForMåned) =
        it.grunnlag.vedtaksperiodeGrunnlag
            .map { it.vedtaksperiode }
            .map { vedtaksperiode ->
                VedtaksperiodePassAvBarn(vedtaksperiode, it.grunnlag.utgifter.map { it.barnId })
            }

    data class VedtaksperiodePassAvBarn(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: FaktiskMålgruppe,
        val aktivitet: AktivitetType,
        val antallBarn: Int,
        val barn: List<BarnId>,
    ) : Periode<LocalDate>,
        Mergeable<LocalDate, VedtaksperiodePassAvBarn> {
        init {
            validatePeriode()
        }

        constructor(vedtaksperiode: VedtaksperiodeBeregning, barn: List<BarnId>) : this(
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            målgruppe = vedtaksperiode.målgruppe,
            aktivitet = vedtaksperiode.aktivitet,
            antallBarn = barn.size,
            barn = barn,
        )

        /**
         * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM
         */
        override fun merge(other: VedtaksperiodePassAvBarn): VedtaksperiodePassAvBarn = this.copy(tom = other.tom)

        fun erLikOgPåfølgesAv(other: VedtaksperiodePassAvBarn): Boolean {
            val erLik =
                this.aktivitet == other.aktivitet &&
                    this.målgruppe == other.målgruppe &&
                    this.antallBarn == other.antallBarn
            val påfølgesAv = this.tom.plusDays(1) == other.fom
            return erLik && påfølgesAv
        }
    }
}
