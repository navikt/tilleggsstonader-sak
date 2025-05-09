package no.nav.tilleggsstonader.sak.vedtak.vedtakOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class VedtaksperiodeOversikt(
    val tilsynBarn: List<VedtaksperiodeOversiktTilsynBarn>,
    val læremidler: List<VedtaksperiodeOversiktLæremidler>,
)

data class VedtaksperiodeOversiktTilsynBarn(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallBarn: Int,
    val totalMånedsUtgift: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, VedtaksperiodeOversiktTilsynBarn> {
    init {
        validatePeriode()
    }

    /**
     * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM
     */
    override fun merge(other: VedtaksperiodeOversiktTilsynBarn): VedtaksperiodeOversiktTilsynBarn = this.copy(tom = other.tom)

    fun erLikOgPåfølgesAv(other: VedtaksperiodeOversiktTilsynBarn): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.antallBarn == other.antallBarn &&
                this.totalMånedsUtgift == other.totalMånedsUtgift
        val påfølgesAv = this.tom.plusDays(1) == other.fom
        return erLik && påfølgesAv
    }
}

data class VedtaksperiodeOversiktLæremidler(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val månedsbeløp: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, VedtaksperiodeOversiktLæremidler> {
    init {
        validatePeriode()
    }

    /**
     * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM
     */

    override fun merge(other: VedtaksperiodeOversiktLæremidler): VedtaksperiodeOversiktLæremidler =
        this.copy(
            tom = other.tom,
            antallMåneder =
                this.antallMåneder + 1,
        )

    fun erLikOgPåfølgesAv(other: VedtaksperiodeOversiktLæremidler): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.studienivå == other.studienivå &&
                this.studieprosent == other.studieprosent &&
                this.månedsbeløp == other.månedsbeløp
        val påfølgesAv = this.tom.plusDays(1) == other.fom
        return erLik && påfølgesAv
    }
}
