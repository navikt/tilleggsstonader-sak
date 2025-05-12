package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeBoutgifter(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int,
    val type: TypeBoutgift,
    val utgift: Int,
    val stønad: Int,
) : Periode<LocalDate>,
    Mergeable<LocalDate, DetaljertVedtaksperiodeBoutgifter> {
    init {
        validatePeriode()
    }

    /**
     * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM.
     * Akkummelerer antall måneder etterhvert som man legger til flere
     */
    override fun merge(other: DetaljertVedtaksperiodeBoutgifter): DetaljertVedtaksperiodeBoutgifter =
        this.copy(
            tom = other.tom,
            antallMåneder =
                this.antallMåneder + 1,
        )

    fun erLikOgPåfølgesAv(other: DetaljertVedtaksperiodeBoutgifter): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.type == other.type &&
                this.utgift == other.utgift &&
                this.stønad == other.stønad
        return erLik && this.påfølgesAv(other)
    }
}

fun List<DetaljertVedtaksperiodeBoutgifter>.sorterOgMergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }
