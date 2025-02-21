package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object VedtaksperiodeLæremidlerMapper {
    fun mapTilVedtaksperiode(beregningsresultatForMåned: List<BeregningsresultatForMåned>): List<VedtaksperiodeLæremidler> =
        beregningsresultatForMåned
            .map { VedtaksperiodeLæremidler(it.grunnlag) }
            .sorted()
            .mergeSammenhengende { s1, s2 -> s1.erLikOgPåfølgesAv(s2) }

    data class VedtaksperiodeLæremidler(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
        val studienivå: Studienivå,
    ) : Periode<LocalDate>,
        Mergeable<LocalDate, VedtaksperiodeLæremidler> {
        init {
            validatePeriode()
        }

        constructor(beregningsgrunnlag: Beregningsgrunnlag) :
            this(
                fom = beregningsgrunnlag.fom,
                tom = beregningsgrunnlag.tom,
                målgruppe = beregningsgrunnlag.målgruppe,
                aktivitet = beregningsgrunnlag.aktivitet,
                studienivå = beregningsgrunnlag.studienivå,
            )

        /**
         * Ettersom stønadsperiode ikke overlapper er det tilstrekkelig å kun merge TOM
         */
        override fun merge(other: VedtaksperiodeLæremidler): VedtaksperiodeLæremidler = this.copy(tom = other.tom)

        fun erLikOgPåfølgesAv(other: VedtaksperiodeLæremidler): Boolean {
            val erLik =
                this.målgruppe == other.målgruppe &&
                    this.aktivitet == other.aktivitet &&
                    this.studienivå == other.studienivå
            return erLik && this.påfølgesAv(other)
        }
    }
}
