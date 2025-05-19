package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class DetaljertVedtaksperiodeBoutgifter(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int = 1,
    val erLøpendeUtgift: Boolean,
    val totalUtgiftMåned: Int,
    val stønadsbeløpMnd: Int,
    val utgifterTilOvernatting: List<UtgiftTilOvernatting>? = null,
) : Periode<LocalDate>,
    Mergeable<LocalDate, DetaljertVedtaksperiodeBoutgifter> {
    override fun merge(other: DetaljertVedtaksperiodeBoutgifter): DetaljertVedtaksperiodeBoutgifter =
        this.copy(
            tom = other.tom,
            antallMåneder =
                this.antallMåneder + 1,
        )

    fun skalMerges(other: DetaljertVedtaksperiodeBoutgifter): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                this.målgruppe == other.målgruppe &&
                this.totalUtgiftMåned == other.totalUtgiftMåned &&
                this.stønadsbeløpMnd == other.stønadsbeløpMnd

        val beggeErLøpendeUtgift = this.erLøpendeUtgift && other.erLøpendeUtgift
        return erLik && beggeErLøpendeUtgift && this.påfølgesAv(other)
    }
}

data class UtgiftTilOvernatting(
    val fom: LocalDate,
    val tom: LocalDate,
    val utgift: Int,
    val beløpSomDekkes: Int,
)

fun List<DetaljertVedtaksperiodeBoutgifter>.sorterOgMergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { p1, p2 -> p1.skalMerges(p2) }
