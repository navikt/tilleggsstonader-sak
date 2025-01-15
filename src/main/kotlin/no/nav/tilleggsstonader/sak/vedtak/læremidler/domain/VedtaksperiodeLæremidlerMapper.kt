package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object VedtaksperiodeLæremidlerMapper {

    fun mapTilVedtaksperiode(
        beregningsresultatForMåned: List<BeregningsresultatForMåned>,
    ): List<VedtaksperiodeLæremidler> {
        return beregningsresultatForMåned
            .map { tilVedtaksperioder(it) }
            .sorted()
            .mergeSammenhengende { s1, s2 -> s1.erLikOgPåfølgesAv(s2) }
    }

    private fun tilVedtaksperioder(
        it: BeregningsresultatForMåned,
    ) = with(it.grunnlag) {
        VedtaksperiodeLæremidler(
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet,
//            studienivå = TOOD()
        )
    }

    data class VedtaksperiodeLæremidler(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
    ) : Periode<LocalDate>, Mergeable<LocalDate, VedtaksperiodeLæremidler> {

        init {
            validatePeriode()
        }

        constructor(stønadsperiode: StønadsperiodeBeregningsgrunnlag, antallBarn: Int) : this(
            fom = stønadsperiode.fom,
            tom = stønadsperiode.tom,
            målgruppe = stønadsperiode.målgruppe,
            aktivitet = stønadsperiode.aktivitet,
            // studienivå
        )

        /**
         * Ettersom stønadsperiode ikke overlapper er det tilstrekkelig å kun merge TOM
         */
        override fun merge(other: VedtaksperiodeLæremidler): VedtaksperiodeLæremidler {
            return this.copy(tom = other.tom)
        }

        fun erLikOgPåfølgesAv(other: VedtaksperiodeLæremidler): Boolean {
            val erLik = this.aktivitet == other.aktivitet &&
                    this.målgruppe == other.målgruppe // && studienivå 🤔?
            val påfølgesAv = this.tom.plusDays(1) == other.fom
            return erLik && påfølgesAv
        }
    }
}