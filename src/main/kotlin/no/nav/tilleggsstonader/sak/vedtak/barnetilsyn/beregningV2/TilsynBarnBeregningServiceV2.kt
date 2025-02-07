package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningFellesService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.brukBeregningsgrunnlagFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2.TilsynBarnBeregningValideringUtilV2.validerPerioderForInnvilgelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.math.BigDecimal

class TilsynBarnBeregningServiceV2(
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val tilsynBarnBeregningFellesService: TilsynBarnBeregningFellesService,
) {
    fun beregn(
        vedtaksperioder: List<VedtaksperiodeDto>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        // Valider ingen overlapp mellom vedtaksperioder

        val perioder = beregnAktuellePerioder(behandling, vedtaksperioder, typeVedtak)
        val relevantePerioderFraForrigeVedtak =
            tilsynBarnBeregningFellesService.finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    private fun beregnAktuellePerioder(
        behandling: Saksbehandling,
        vedtaksperioder: List<VedtaksperiodeDto>,
        typeVedtak: TypeVedtak,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)
        val aktiviteter = tilsynBarnBeregningFellesService.finnAktiviteter(behandling.id)
        val vedtaksperioderEtterRevurderFra = vedtaksperioder.sorted().splitFraRevurderFra(behandling.revurderFra)

        validerPerioderForInnvilgelse(
            vedtaksperioder = vedtaksperioderEtterRevurderFra,
            aktiviteter = aktiviteter,
            utgifter = utgifterPerBarn,
            typeVedtak = typeVedtak,
            revurderFra = behandling.revurderFra,
        )

        val beregningsgrunnlag =
            BeregingsgrunnlagUtilsV2
                .lagBeregningsgrunnlagPerMåned(
                    vedtaksperioder = vedtaksperioderEtterRevurderFra,
                    aktiviteter = aktiviteter,
                    utgifterPerBarn = utgifterPerBarn,
                ).brukBeregningsgrunnlagFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    // Kopiert fra V1
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
        beregningsgrunnlag: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        beregningsgrunnlag.vedtaksperioderGrunnlag.map {
            val dato = it.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = tilsynBarnBeregningFellesService.beregnBeløp(dagsats, it.antallAktivitetsDager),
                målgruppe = it.målgruppeType,
            )
        }
}
