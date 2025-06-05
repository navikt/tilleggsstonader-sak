package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Utbetalingperiode for løpende måned
 * Eks 5jan - 4feb
 *
 * @param tom er maks dato for vedtaksperiode inne i en løpende måned.
 * I de tilfeller man kun har en vedtaksperiode som gjelder 5 jan - 7 jan så vil tom= 7 jan.
 *
 */
@ConsistentCopyVisibility
data class UtbetalingPeriode private constructor(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        require(tom <= fom.sisteDagenILøpendeMåned()) {
            "UtbetalingPeriode kan ikke løpe lengre enn en løpende måned"
        }
    }

    constructor(
        løpendeMåned: LøpendeMåned,
        skalAvkorte: Boolean,
    ) : this(
        fom = løpendeMåned.fom,
        tom = if (skalAvkorte) løpendeMåned.vedtaksperioder.maxOf { it.tom } else løpendeMåned.tom,
        /**
         * TODO: Prioriter hvilken målgruppe+aktivitet som skal være gjeldende til økonomi hvis ulike målgrupper havner innenfor samme løpende måned
         * Dette gjøres i [no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.UtbetalingPeriode]
         */
        målgruppe =
            løpendeMåned.vedtaksperioder
                .distinctBy { it.målgruppe }
                .singleOrNull()
                ?.målgruppe
                ?: brukerfeil(
                    "Det finnes flere ulike målgrupper i utbetalingsperioden ${løpendeMåned.formatertPeriodeNorskFormat()}. Dette er foreløpig ikke noe vi har støtte for.",
                ),
        aktivitet =
            løpendeMåned.vedtaksperioder
                .distinctBy { it.aktivitet }
                .singleOrNull()
                ?.aktivitet
                ?: brukerfeil(
                    "Det finnes flere ulike målgrupper i utbetalingsperioden ${løpendeMåned.formatertPeriodeNorskFormat()}. Dette er foreløpig ikke noe vi har støtte for.",
                ),
    )
}

data class LøpendeMåned(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    /**
     * backing property for vedtaksperioder.
     * Inneholder de vedtaksperioder som er innvilget innenfor en UtbetalingPeriode
     * Implementert som private backing property for å ikke kunne legge til perioder direkte til listen uten å validere den
     */
    private val _vedtaksperioder: MutableList<VedtaksperiodeInnenforLøpendeMåned> = mutableListOf()

    val vedtaksperioder: List<VedtaksperiodeInnenforLøpendeMåned> get() = _vedtaksperioder

    init {
        validatePeriode()
        _vedtaksperioder.forEach { this.inneholder(it) }
    }

    fun medVedtaksperiode(vedtaksperiode: VedtaksperiodeInnenforLøpendeMåned): LøpendeMåned {
        require(inneholder(vedtaksperiode)) {
            "Vedtaksperiode(${vedtaksperiode.formatertPeriodeNorskFormat()}) kan ikke gå utenfor utbetalingsperiode(${this.formatertPeriodeNorskFormat()})"
        }
        _vedtaksperioder.add(vedtaksperiode)
        return this
    }
}
