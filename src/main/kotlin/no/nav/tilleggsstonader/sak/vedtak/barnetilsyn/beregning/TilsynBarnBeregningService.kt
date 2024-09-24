package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.tilAktiviteter
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
private val DEKNINGSGRAD = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
) {

    fun beregn(behandlingId: BehandlingId): BeregningsresultatTilsynBarnDto {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandlingId)
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        val aktiviteter = finnAktiviteter(behandlingId)

        validerPerioder(stønadsperioder, aktiviteter, utgifterPerBarn)

        val beregningsgrunnlag = lagBeregningsgrunnlagPerMåned(stønadsperioder, aktiviteter, utgifterPerBarn)
        val perioder = beregn(beregningsgrunnlag)

        return BeregningsresultatTilsynBarnDto(perioder)
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<Beregningsresultat> {
        return beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            Beregningsresultat(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }
    }

    private fun lagBeløpsperioder(dagsats: BigDecimal, it: Beregningsgrunnlag): List<Beløpsperiode> {
        return it.stønadsperioderGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.stønadsperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.stønadsperiode.målgruppe,
            )
        }
    }

    private fun beregnBeløp(dagsats: BigDecimal, antallDager: Int) =
        dagsats.multiply(antallDager.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes = utgifter.multiply(DEKNINGSGRAD)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun lagBeregningsgrunnlagPerMåned(
        stønadsperioder: List<StønadsperiodeDto>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioderGrunnlag = finnStønadsperioderMedAktiviteter(stønadsperioder, aktiviteterForMåned),
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnStønadsperioderMedAktiviteter(
        stønadsperioder: List<StønadsperiodeDto>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<StønadsperiodeGrunnlag> {
        return stønadsperioder.map { stønadsperiode ->
            val relevanteAktiviteter = finnAktiviteterForStønadsperiode(stønadsperiode, aktiviteter)

            StønadsperiodeGrunnlag(
                stønadsperiode = stønadsperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = antallDager(stønadsperiode, relevanteAktiviteter),
            )
        }
    }

    private fun finnAktiviteterForStønadsperiode(
        stønadsperiode: StønadsperiodeDto,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> {
        return aktiviteter[stønadsperiode.aktivitet]?.filter { it.overlapper(stønadsperiode) }
            ?: error("Finnes ingen aktiviteter av type ${stønadsperiode.aktivitet} som passer med stønadsperiode med fom=${stønadsperiode.fom} og tom=${stønadsperiode.tom}")
    }

    private fun antallDager(stønadsperiode: StønadsperiodeDto, aktiviteter: List<Aktivitet>): Int {
        val stønadsperioderUker = stønadsperiode.tilUke()
        val aktiviteterUker = aktiviteter.tilDagerPerUke()

        val antallDagerMedAktivitetPerUke = stønadsperioderUker.map { (uke, periode) ->
            val maksAntallDagerIUke = periode.antallDager

            val aktiviteterForUke = aktiviteterUker[uke]
            val antallDagerMedAktivitet = aktiviteterForUke?.sumOf { it.antallDager }
                ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

            min(antallDagerMedAktivitet, maksAntallDagerIUke)
        }

        return antallDagerMedAktivitetPerUke.sum()
    }

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }

    private fun validerPerioder(
        stønadsperioder: List<StønadsperiodeDto>,
        aktiviteter: List<Aktivitet>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
        validerStønadsperioder(stønadsperioder)
        validerAktiviteter(aktiviteter)
        validerUtgifter(utgifter)
    }

    private fun validerStønadsperioder(stønadsperioder: List<StønadsperiodeDto>) {
        feilHvis(stønadsperioder.isEmpty()) {
            "Stønadsperioder mangler"
        }
    }

    private fun validerAktiviteter(aktiviteter: List<Aktivitet>) {
        feilHvis(aktiviteter.isEmpty()) {
            "Aktiviteter mangler"
        }
    }

    private fun validerUtgifter(utgifter: Map<BarnId, List<UtgiftBeregning>>) {
        feilHvis(utgifter.values.flatten().isEmpty()) {
            "Utgiftsperioder mangler"
        }
        utgifter.entries.forEach { (_, utgifterForBarn) ->
            feilHvis(utgifterForBarn.overlapper()) {
                "Utgiftsperioder overlapper"
            }

            val ikkePositivUtgift = utgifterForBarn.firstOrNull { it.utgift < 0 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
            }
        }
    }
}
