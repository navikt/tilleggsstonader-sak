package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import java.math.BigDecimal
import java.math.RoundingMode

// Kopiert fra V1
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

class TilsynBarnBeregningServiceV2(
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
) {
    fun beregn(
        vedtaksperioder: List<VedtaksperiodeDto>,
        behandlingId: BehandlingId,
    ): BeregningsresultatTilsynBarn {
        // Valider ingen overlapp mellom vedtaksperioder

        val perioder = beregnAktuellePerioder(behandlingId, vedtaksperioder)
        // Hent inn perioder fra tidligere behandlinger og legg til disse
        // eg: return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
        return BeregningsresultatTilsynBarn(perioder)
    }

    private fun beregnAktuellePerioder(
        behandlingId: BehandlingId,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandlingId)
        val aktiviteter = finnAktiviteter(behandlingId)

        // Validere noe data?

        val beregningsgrunnlag =
            BeregingsgrunnlagUtilsV2.lagBeregningsgrunnlagPerMåned(
                vedtaksperioder = vedtaksperioder,
                aktiviteter = aktiviteter,
                utgifterPerBarn = utgifterPerBarn,
            )
        // Oppdatere til kun etter revurderFra
        // eg: .brukPerioderFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    // Kopiert fra V1
    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    // Kopiert fra V1
    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<BeregningsresultatForMåned> =
        beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            BeregningsresultatForMåned(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }

    // Kopiert logikk fra V1, oppdatert navngivning
    private fun lagBeløpsperioder(
        dagsats: BigDecimal,
        beregningsgrunnlag: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        beregningsgrunnlag.vedtaksperioderGrunnlag.map {
            val dato = it.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallAktivitetsDager),
                målgruppe = it.målgruppeType,
            )
        }

    // Kopiert fra V1

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes =
            utgifter
                .multiply(DEKNINGSGRAD_TILSYN_BARN)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    // Kopiert fra V1
    private fun beregnBeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ) = dagsats
        .multiply(antallDager.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
}
