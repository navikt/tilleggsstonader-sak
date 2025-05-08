package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.lørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Utbetalingperiode for løpende måned
 * Eks 5jan - 4feb
 *
 * @param tom er maks dato for vedtaksperiode inne i en løpende måned.
 * I de tilfeller man kun har en vedtaksperiode som gjelder 5 jan - 7 jan så vil tom= 7 jan.
 *
 * @param utbetalingsdato utbetalingsdato for når en utbetalingsperiode skal utbetales.
 * Eks hvis man innvilger jan-juni så skal man utbetale hele beløpet for fom i første utbetalingsperioden,
 * dvs 5 jan i tidligere eksemplet
 *
 */
data class UtbetalingPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
    val utbetalingsdato: LocalDate,
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
//        tom = if (skalAvkorte) løpendeMåned.vedtaksperioder.maxOf { it.tom } else løpendeMåned.tom,
        tom = løpendeMåned.tom,
        // TODO: Prioriter hvilken målgruppe+aktivitet som skal være gjeldende til økonomi hvis ulike målgrupper havner innenfor samme løpende måned
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
        utbetalingsdato = løpendeMåned.fom,
    )
}

data class LøpendeMåned(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsdato: LocalDate,
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

    fun harDatoerIUkedager(): Boolean = vedtaksperioder.any { it.alleDatoer().any { !it.lørdagEllerSøndag() } }
}
