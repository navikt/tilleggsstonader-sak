package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object VedtaksperiodeTilsynBarnMapper {

    fun mapTilVedtaksperiode(
        beregningsresultatForMåned: List<BeregningsresultatForMåned>,
    ): List<VedtaksperiodeTilsynBarn> {
        return beregningsresultatForMåned
            .flatMap { tilVedtaksperioder(it) }
            .sorted()
            .mergeSammenhengende { s1, s2 -> s1.erLikOgPåfølgesAv(s2) }
    }

    private fun tilVedtaksperioder(
        it: BeregningsresultatForMåned,
    ) = it.grunnlag.stønadsperioderGrunnlag
        .map { it.stønadsperiode }
        .map { stønadsperiode -> VedtaksperiodeTilsynBarn(stønadsperiode, it.grunnlag.antallBarn) }
}

data class VedtaksperiodeTilsynBarn(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val antallBarn: Int,
) : Periode<LocalDate>, Mergeable<LocalDate, VedtaksperiodeTilsynBarn> {

    init {
        validatePeriode()
    }

    constructor(stønadsperiode: StønadsperiodeBeregningsgrunnlag, antallBarn: Int) : this(
        fom = stønadsperiode.fom,
        tom = stønadsperiode.tom,
        målgruppe = stønadsperiode.målgruppe,
        aktivitet = stønadsperiode.aktivitet,
        antallBarn = antallBarn,
    )

    /**
     * Ettersom stønadsperiode ikke overlapper er det tilstrekkelig å kun merge TOM
     */
    override fun merge(other: VedtaksperiodeTilsynBarn): VedtaksperiodeTilsynBarn {
        return this.copy(tom = other.tom)
    }

    fun erLikOgPåfølgesAv(other: VedtaksperiodeTilsynBarn): Boolean {
        val erLik = this.aktivitet == other.aktivitet &&
            this.målgruppe == other.målgruppe &&
            this.antallBarn == other.antallBarn
        val påfølgesAv = this.tom.plusDays(1) == other.fom
        return erLik && påfølgesAv
    }
}
