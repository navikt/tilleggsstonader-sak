package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningFellesService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.BeregningsgrunnlagUtils.lagBeregningsgrunnlagPerMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBarnBeregningValideringUtil.validerPerioderForInnvilgelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.brukBeregningsgrunnlagFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
val DEKNINGSGRAD_TILSYN_BARN = BigDecimal("0.64")

@Service
class TilsynBarnBeregningService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val tilsynBarnBeregningFellesService: TilsynBarnBeregningFellesService,
) {
    fun beregn(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }
        val perioder = beregnAktuellePerioder(behandling, typeVedtak)
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    /**
     * Dersom behandling er en revurdering beregnes perioder fra og med måneden for revurderFra
     * Ellers beregnes perioder for hele perioden som man har stønadsperioder og utgifter
     */
    private fun beregnAktuellePerioder(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)
        val stønadsperioder =
            stønadsperiodeRepository
                .findAllByBehandlingId(behandling.id)
                .tilSortertStønadsperiodeBeregningsgrunnlag()
                .splitFraRevurderFra(behandling.revurderFra)

        val aktiviteter = tilsynBarnBeregningFellesService.finnAktiviteter(behandling.id)

        validerPerioderForInnvilgelse(stønadsperioder, aktiviteter, utgifterPerBarn, typeVedtak, behandling.revurderFra)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagPerMåned(stønadsperioder, aktiviteter, utgifterPerBarn)
                .brukBeregningsgrunnlagFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<BeregningsresultatForMåned> =
        beregningsgrunnlag.map {
            val dagsats = tilsynBarnBeregningFellesService.beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            BeregningsresultatForMåned(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }

    private fun lagBeløpsperioder(
        dagsats: BigDecimal,
        it: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        it.stønadsperioderGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.stønadsperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = tilsynBarnBeregningFellesService.beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.stønadsperiode.målgruppe,
            )
        }
}
