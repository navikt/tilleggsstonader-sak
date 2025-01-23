package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
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
            studienivå = studienivå,
        )
    }

    data class VedtaksperiodeLæremidler(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
        val studienivå: Studienivå,
    ) : Periode<LocalDate>, Mergeable<LocalDate, VedtaksperiodeLæremidler> {

        init {
            validatePeriode()
        }

        /**
         * Ettersom stønadsperiode ikke overlapper er det tilstrekkelig å kun merge TOM
         */
        override fun merge(other: VedtaksperiodeLæremidler): VedtaksperiodeLæremidler {
            return this.copy(tom = other.tom)
        }

        fun erLikOgPåfølgesAv(other: VedtaksperiodeLæremidler): Boolean {
            val erLik = this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.studienivå == other.studienivå
            val påfølgesAv = this.tom.plusDays(1) == other.fom
            return erLik && påfølgesAv
        }
    }
}
