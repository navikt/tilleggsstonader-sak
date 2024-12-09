package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }

    // ANTAR: En aktitivet hele vedtaksperioden
    // ANTAR: En stønadsperiode per vedtaksperiode
    // ANTAR: Sats ikke endrer seg i perioden
    fun beregn(vedtaksPeriode: List<VedtaksPeriode>, behandlingId: BehandlingId): BeregningsresultatLæremidler {
        val stønadsperioder =
            stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertGrunnlagStønadsperiode()

        validerVedtaksperioder(vedtaksPeriode, stønadsperioder)

        val vedtaksPerioderMedStøndsperioder = finnVedtaksperioderMedStønadsperioder(vedtaksPeriode, stønadsperioder)

        val beregningsresultatForMåned = vedtaksPerioderMedStøndsperioder.flatMap { vedtaksPeriode ->
            val aktivitet =
                finnAktiviteter(behandlingId).filter { aktivitet -> aktivitet.type == vedtaksPeriode.stønadsperiode.single().aktivitet }
                    .single()

            val perioderDeltIMåneder = vedtaksPeriode.delIÅr { fom, tom -> VedtaksPeriode(fom, tom) }
                .flatMap { periode ->
                    periode.delIDatoTilDatoMånder { fom, tom ->
                        UtbetalingsPeriode(
                            fom = fom,
                            tom = tom,
                            utbetalingsMåned = periode.fom.toYearMonth()
                        )
                    }
                }

            perioderDeltIMåneder.map { periode ->
                val grunnlagsdata =
                    lagBeregningsGrunnlag(periode, aktivitet, vedtaksPeriode.stønadsperiode.single().målgruppe)
                BeregningsresultatForMåned(
                    beløp = finnBeløpForStudieprosent(grunnlagsdata.sats, aktivitet.prosent),
                    grunnlag = grunnlagsdata,
                )
            }

        }

        return BeregningsresultatLæremidler(beregningsresultatForMåned)

    }



    private fun validerVedtaksperioder(
        vedtaksperioder: List<VedtaksPeriode>,
        stønadsperioder: List<Stønadsperiode>,
    ) {
        feilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder overlapper"
        }

        val sammenslåtteStønadsperioder = stønadsperioder
            .mergeSammenhengende(
                skalMerges = { a, b -> a.tom.plusDays(1) == b.fom },
                merge = { a, b -> a.copy(tom = b.tom) })

        feilHvis(vedtaksperioder.any { vedtaksperiode ->
            sammenslåtteStønadsperioder.none { it.inneholder(vedtaksperiode) }
        }) {
            "Vedtaksperiode er ikke innenfor en stønadsperiode"
        }

    }

    fun finnVedtaksperioderMedStønadsperioder(
        vedtaksperioder: List<VedtaksPeriode>,
        stønadsperioder: List<Stønadsperiode>,
    ): List<VedtaksPeriodeMedStøndasperioder> {
        return vedtaksperioder.map { vedtaksperiode ->
            val overlappendeStøndasperioder = stønadsperioder.filter { it.overlapper(vedtaksperiode) }
            VedtaksPeriodeMedStøndasperioder(vedtaksperiode, overlappendeStøndasperioder)
        }
    }

    //TODO flytt til Kontrakter
    fun <P : Periode<LocalDate>, VAL> P.delIDatoTilDatoMånder(value: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = minOf(gjeldeneFom.plusMonths(1).minusDays(1), tom)
            perioder.add(value(gjeldeneFom, nyTom))
            gjeldeneFom = gjeldeneFom.plusMonths(1)
        }
        return perioder
    }

    //TODO flytt til Kontrakter
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

    fun lagBeregningsGrunnlag(
        periode: UtbetalingsPeriode,
        aktivitet: Aktivitet,
        målgruppe: MålgruppeType
    ): Beregningsgrunnlag {
        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            studienivå = aktivitet.studienivå!!,
            studieprosent = aktivitet.prosent,
            sats = finnSatsForStudienivå(periode, aktivitet.studienivå),
            utbetalingsMåned = periode.utbetalingsMåned,
            målgruppe = målgruppe
        )
    }

    fun finnBeløpForStudieprosent(sats: Int, studieprosent: Int): Int {
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


data class VedtaksPeriodeMedStøndasperioder(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val stønadsperiode: List<Stønadsperiode>
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }

    constructor(vedtaksPeriode: VedtaksPeriode, stønadsperiode: List<Stønadsperiode>) : this(vedtaksPeriode.fom, vedtaksPeriode.tom, stønadsperiode)
}

data class VedtaksPeriode(
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

