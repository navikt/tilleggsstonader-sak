package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilSortertGrunnlagStønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFaktaOrThrow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private val PROSENT_50 = BigDecimal(0.5)
private val PROSENTGRENSE_HALV_SATS = 50

@Service
class LæremidlerBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) {
    /**
     * Beregning av læremidler er foreløpig kun basert på antakelser.
     * Nå beregnes det med hele måneder, splittet i månedsskifte, men dette er ikke avklart som korrekt.
     * Det er ikke tatt hensyn til begrensninger på semester og maks antall måneder i et år som skal dekkes.
     */

    // ANTAR: En aktitivet hele vedtaksperioden
    // ANTAR: En stønadsperiode per vedtaksperiode
    // ANTAR: Sats ikke endrer seg i perioden
    fun beregn(vedtaksPeriode: List<Vedtaksperiode>, behandlingId: BehandlingId): BeregningsresultatLæremidler {
        val stønadsperioder =
            stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertGrunnlagStønadsperiode()
        val aktiviteter = finnAktiviteter(behandlingId)

        validerVedtaksperioder(vedtaksPeriode, stønadsperioder)

        val vedtaksperioderMedStøndsperioder = finnVedtaksperioderMedStønadsperioder(vedtaksPeriode, stønadsperioder)

        val beregningsresultatForMåned = vedtaksperioderMedStøndsperioder.flatMap { vedtaksperiodeMedStønadsperioder ->
            val utbetalingsperioder = vedtaksperiodeMedStønadsperioder.delTilUtbetalingsPerioder()

            utbetalingsperioder.map { utbetalingsperiode ->
                val relevantStønadsperiode = utbetalingsperiode.finnRelevantStønadsperiode(stønadsperioder)

                lagBeregningsresultatForMåned(
                    utbetalingsperiode = utbetalingsperiode,
                    stønadsperiode = relevantStønadsperiode,
                    aktivitet = utbetalingsperiode.finnRelevantAktivitet(
                        aktiviteter = aktiviteter,
                        aktivitetType = relevantStønadsperiode.aktivitet
                    ),
                )
            }
        }

        return BeregningsresultatLæremidler(beregningsresultatForMåned)
    }

    private fun lagBeregningsresultatForMåned(
        utbetalingsperiode: UtbetalingsPeriode,
        stønadsperiode: Stønadsperiode,
        aktivitet: Aktivitet,
    ): BeregningsresultatForMåned {
        val grunnlagsdata =
            lagBeregningsGrunnlag(
                periode = utbetalingsperiode,
                aktivitet = aktivitet,
                målgruppe = stønadsperiode.målgruppe,
            )

        return BeregningsresultatForMåned(
            beløp = finnBeløpForStudieprosent(grunnlagsdata.sats, grunnlagsdata.studieprosent),
            grunnlag = grunnlagsdata,
        )
    }

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }

    private fun UtbetalingsPeriode.finnRelevantStønadsperiode(stønadsperioder: List<Stønadsperiode>): Stønadsperiode {
        val relevanteStønadsperioderForPeriode = stønadsperioder
            .mergeSammenhengende(
                skalMerges = { a, b -> a.tom.plusDays(1) == b.fom && a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet },
                merge = { a, b -> a.copy(tom = b.tom) },
            )
            .filter { it.inneholder(this) }

        feilHvis(relevanteStønadsperioderForPeriode.isEmpty()) {
            "Det finnes ingen periode med overlapp mellom målgruppe og aktivitet for perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        feilHvis(relevanteStønadsperioderForPeriode.size > 1) {
            "Det er for mange stønadsperioder som inneholder utbetalingsperioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        return relevanteStønadsperioderForPeriode.single()
    }

    private fun UtbetalingsPeriode.finnRelevantAktivitet(
        aktiviteter: List<Aktivitet>,
        aktivitetType: AktivitetType
    ): Aktivitet {
        val relevanteAktiviteter = aktiviteter.filter { it.type == aktivitetType && it.inneholder(this) }

        brukerfeilHvis(relevanteAktiviteter.isEmpty()) {
            "Det finnes ingen aktiviteter av type $aktivitetType som varer i hele perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}"
        }

        brukerfeilHvis(relevanteAktiviteter.size > 1) {
            "Det finnes mer enn 1 aktivitet i perioden ${this.fom.norskFormat()} - ${this.tom.norskFormat()}. Dette støttes ikke enda. Ta kontakt med TS-sak teamet."
        }

        return relevanteAktiviteter.single()
    }

    private fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
    ) {
        feilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder overlapper"
        }

        val sammenslåtteStønadsperioder = stønadsperioder
            .mergeSammenhengende(
                skalMerges = { a, b -> a.tom.plusDays(1) == b.fom },
                merge = { a, b -> a.copy(tom = b.tom) },
            )

        feilHvis(
            vedtaksperioder.any { vedtaksperiode ->
                sammenslåtteStønadsperioder.none { it.inneholder(vedtaksperiode) }
            },
        ) {
            "Vedtaksperiode er ikke innenfor en stønadsperiode"
        }
    }

    private fun finnVedtaksperioderMedStønadsperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
    ): List<VedtaksperiodeMedStøndasperioder> {
        return vedtaksperioder.map { vedtaksperiode ->
            val overlappendeStøndasperioder = stønadsperioder.filter { it.overlapper(vedtaksperiode) }
            VedtaksperiodeMedStøndasperioder(vedtaksperiode, overlappendeStøndasperioder)
        }
    }

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingsPeriode,
        aktivitet: Aktivitet,
        målgruppe: MålgruppeType,
    ): Beregningsgrunnlag {
        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            studienivå = aktivitet.studienivå ?: throw IllegalStateException("Studienivå finnes ikke på aktiviteten"),
            studieprosent = aktivitet.prosent,
            sats = finnSatsForStudienivå(periode, aktivitet.studienivå),
            utbetalingsMåned = periode.utbetalingsMåned,
            målgruppe = målgruppe,
        )
    }

    private fun finnBeløpForStudieprosent(sats: Int, studieprosent: Int): Int {
        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats).multiply(PROSENT_50).setScale(0, RoundingMode.HALF_UP).toInt()
        }
        return sats
    }
}

data class Aktivitet(
    val id: UUID?, // Må være null pga bakåtkompatibilitet
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prosent: Int,
    val studienivå: Studienivå?, // TODO skal ikke være nullable

) : Periode<LocalDate>

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> {
    return this
        .ofType<AktivitetLæremidler>()
        .map {
            val fakta = it.faktaOgVurdering.fakta
            Aktivitet(
                id = it.id,
                type = it.faktaOgVurdering.type.vilkårperiodeType,
                fom = it.fom,
                tom = it.tom,
                prosent = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().prosent,
                studienivå = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().studienivå,
            )
        }
}

data class VedtaksperiodeMedStøndasperioder(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val stønadsperiode: List<Stønadsperiode>,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }

    constructor(vedtaksPeriode: Vedtaksperiode, stønadsperiode: List<Stønadsperiode>) : this(
        vedtaksPeriode.fom,
        vedtaksPeriode.tom,
        stønadsperiode,
    )

    fun delTilUtbetalingsPerioder(): List<UtbetalingsPeriode> {
        return delIÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.delIDatoTilDatoMåneder { fom, tom ->
                    UtbetalingsPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsMåned = periode.fom.toYearMonth(),
                    )
                }
            }
    }

    // TODO flytt til Kontrakter
    fun <P : Periode<LocalDate>, VAL> P.delIDatoTilDatoMåneder(value: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = minOf(gjeldeneFom.plusMonths(1).minusDays(1), tom)
            perioder.add(value(gjeldeneFom, nyTom))
            gjeldeneFom = gjeldeneFom.plusMonths(1)
        }
        return perioder
    }

    // TODO flytt til Kontrakter
    fun <P : Periode<LocalDate>> P.delIÅr(value: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = minOf(LocalDate.of(gjeldeneFom.year, 12, 31), tom)
            perioder.add(value(gjeldeneFom, nyTom))
            gjeldeneFom = LocalDate.of(gjeldeneFom.year + 1, 1, 1)
        }
        return perioder
    }
}

data class Vedtaksperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

data class UtbetalingsPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsMåned: YearMonth,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}
