package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeLæremidler(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val månedsbeløp: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, DetaljertVedtaksperiodeLæremidler> {
    init {
        validatePeriode()
    }

    /**
     * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM.
     * Akkummelerer antall måneder etterhvert som man legger til flere
     */
    override fun merge(other: DetaljertVedtaksperiodeLæremidler): DetaljertVedtaksperiodeLæremidler =
        this.copy(
            tom = other.tom,
            antallMåneder =
                this.antallMåneder + 1,
        )

    fun erLikOgPåfølgesAv(other: DetaljertVedtaksperiodeLæremidler): Boolean {
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

fun List<DetaljertVedtaksperiodeLæremidler>.sorterOgMergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }
