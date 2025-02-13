package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object VedtaksperiodeTilsynBarnMapper {
    fun mapTilVedtaksperiode(beregningsresultatForMåned: List<BeregningsresultatForMåned>): List<VedtaksperiodeTilsynBarn> =
        beregningsresultatForMåned
            .flatMap { tilVedtaksperioder(it) }
            .sorted()
            .mergeSammenhengende { s1, s2 -> s1.erLikOgPåfølgesAv(s2) }

    private fun tilVedtaksperioder(it: BeregningsresultatForMåned) =
        it.grunnlag.stønadsperioderGrunnlag
            .map { it.stønadsperiode }
            .map { vedtaksperiode ->
                VedtaksperiodeTilsynBarn(vedtaksperiode, it.grunnlag.utgifter.map { it.barnId })
            }

    data class VedtaksperiodeTilsynBarn(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
        val antallBarn: Int,
        val barn: List<BarnId>,
    ) : Periode<LocalDate>,
        Mergeable<LocalDate, VedtaksperiodeTilsynBarn> {
        init {
            validatePeriode()
        }

        constructor(vedtaksperiode: Vedtaksperiode, barn: List<BarnId>) : this(
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            målgruppe = vedtaksperiode.målgruppe,
            aktivitet = vedtaksperiode.aktivitet,
            antallBarn = barn.size,
            barn = barn,
        )

        /**
         * Ettersom stønadsperiode ikke overlapper er det tilstrekkelig å kun merge TOM
         */
        override fun merge(other: VedtaksperiodeTilsynBarn): VedtaksperiodeTilsynBarn = this.copy(tom = other.tom)

        fun erLikOgPåfølgesAv(other: VedtaksperiodeTilsynBarn): Boolean {
            val erLik =
                this.aktivitet == other.aktivitet &&
                    this.målgruppe == other.målgruppe &&
                    this.antallBarn == other.antallBarn
            val påfølgesAv = this.tom.plusDays(1) == other.fom
            return erLik && påfølgesAv
        }
    }
}
