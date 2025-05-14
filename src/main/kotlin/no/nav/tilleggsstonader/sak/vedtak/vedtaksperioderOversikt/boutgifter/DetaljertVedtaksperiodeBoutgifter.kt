package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class UtgifterBo(
    val fom: LocalDate,
    val tom: LocalDate,
    val utgift: Int,
    val beløpSomDekkes: Int,
)

data class DetaljertVedtaksperiodeBoutgifterV2(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int = 1,
    val erLøpendeUtgift: Boolean,
    val totalUtgiftMåned: Int,
    val stønadsbeløpMnd: Int,
    val utgifter: List<UtgifterBo>? = null,
) : Periode<LocalDate>

fun BoOvernatting.tilDetaljertVedtaksperiodeV2() = DetaljertVedtaksperiodeBoutgifterV2(
    fom = fom,
    tom = tom,
    aktivitet = aktivitet,
    målgruppe = målgruppe,
    erLøpendeUtgift = false,
    totalUtgiftMåned = utgifter.sumOf { it.utgift }, // kan droppes om vi ikke bruker den
    stønadsbeløpMnd = utgifter.sumOf { it.beløpSomDekkes }, // kan droppes om vi ikke bruker den
    utgifter = utgifter,
)

fun BoLøpendeUtgift.tilDetaljertVedtaksperiodeV2() = DetaljertVedtaksperiodeBoutgifterV2(
    fom = fom,
    tom = tom,
    aktivitet = aktivitet,
    målgruppe = målgruppe,
    erLøpendeUtgift = true,
    totalUtgiftMåned = utgift,
    stønadsbeløpMnd = stønad,
    antallMåneder = antallMåneder,
)

data class BoOvernatting(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val aktivitet: AktivitetType,
    override val målgruppe: FaktiskMålgruppe,
    val utgifter: List<UtgifterBo>,
) : Test(
    tom = tom,
    fom = fom,
    aktivitet = aktivitet,
    målgruppe = målgruppe,
    erLøpendeUtgift = false,
)

sealed class Test(
    override val tom: LocalDate,
    override val fom: LocalDate,
    open val aktivitet: AktivitetType,
    open val målgruppe: FaktiskMålgruppe,
    open val erLøpendeUtgift: Boolean,
) : Periode<LocalDate>

data class BoLøpendeUtgift(
    override val tom: LocalDate,
    override val fom: LocalDate,
    val aktivitet: AktivitetType,
    val målgruppe: FaktiskMålgruppe,
    val antallMåneder: Int = 1,
    val utgift: Int,
    val stønad: Int,
) : Periode<LocalDate>, Mergeable<LocalDate, BoLøpendeUtgift> {
    init {
        validatePeriode()
    }

    override fun merge(other: BoLøpendeUtgift): BoLøpendeUtgift =
        this.copy(
            tom = other.tom,
            antallMåneder =
                this.antallMåneder + 1,
        )

    fun erLikOgPåfølgesAv(other: BoLøpendeUtgift): Boolean {
        val erLik =
            this.aktivitet == other.aktivitet &&
                    this.målgruppe == other.målgruppe &&
                    this.utgift == other.utgift &&
                    this.stønad == other.stønad

        return erLik && this.påfølgesAv(other)
    }
}

fun List<BoLøpendeUtgift>.sorterOgMergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }

//    data class DetaljertVedtaksperiodeBoutgifter(
//        override val fom: LocalDate,
//        override val tom: LocalDate,
//        val aktivitet: AktivitetType,
//        val målgruppe: FaktiskMålgruppe,
//        val antallMåneder: Int,
//        val type: TypeBoutgift,
//        val utgift: Int,
//        val stønad: Int,
//    ) : Periode<LocalDate>,
//        Mergeable<LocalDate, DetaljertVedtaksperiodeBoutgifter> {
//        init {
//            validatePeriode()
//        }
//
//        /**
//         * Ettersom vedtaksperiode ikke overlapper er det tilstrekkelig å kun merge TOM.
//         * Akkummelerer antall måneder etterhvert som man legger til flere
//         */
//        override fun merge(other: DetaljertVedtaksperiodeBoutgifter): DetaljertVedtaksperiodeBoutgifter =
//            this.copy(
//                tom = other.tom,
//                antallMåneder =
//                    this.antallMåneder + 1,
//            )
//
//        fun erLikOgPåfølgesAv(other: DetaljertVedtaksperiodeBoutgifter): Boolean {
//            val erLik =
//                this.aktivitet == other.aktivitet &&
//                        this.målgruppe == other.målgruppe &&
//                        this.type == other.type &&
//                        this.utgift == other.utgift &&
//                        this.stønad == other.stønad
//            return erLik && this.påfølgesAv(other)
//        }
//    }

//    fun List<DetaljertVedtaksperiodeBoutgifter>.sorterOgMergeSammenhengende() =
//        this
//            .sorted()
//            .mergeSammenhengende { p1, p2 -> p1.erLikOgPåfølgesAv(p2) }
