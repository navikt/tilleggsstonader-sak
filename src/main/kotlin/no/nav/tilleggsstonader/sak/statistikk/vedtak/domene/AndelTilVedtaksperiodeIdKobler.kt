package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.iDagHvisMandagEllerForrigeMandag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.finnPeriodeFraAndel

/**
 * Kobler en [AndelTilkjentYtelse] til den/de [Vedtaksperiode] den stammer fra.
 * Brukes for å berike vedtaksstatistikk (DVH) med vedtaksperiodeId per utbetaling,
 * se [UtbetalingerDvh].
 */
fun interface AndelTilVedtaksperiodeIdKobler {
    fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode>
}

/**
 * Finner riktig [AndelTilVedtaksperiodeIdKobler] basert på hvilken type vedtaksdata vedtaket har,
 * og bruker denne til å finne vedtaksperiodene som andelen stammer fra.
 *
 * For avslag finnes det ingen vedtaksperioder/andeler, så det returneres en tom liste.
 */
object AndelTilVedtaksperiodeMapper {
    fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtak: Vedtak,
    ): List<Vedtaksperiode> = finnKobler(vedtak.data)?.finnVedtaksperioder(andel, vedtak) ?: emptyList()

    private fun finnKobler(vedtaksdata: Vedtaksdata): AndelTilVedtaksperiodeIdKobler? =
        when (vedtaksdata) {
            is InnvilgelseEllerOpphørPassAvBarn -> BarnetilsynAndelTilVedtaksperiodeIdKobler
            is InnvilgelseEllerOpphørLæremidler -> LæremidlerAndelTilVedtaksperiodeIdKobler
            is InnvilgelseEllerOpphørBoutgifter -> BoutgifterAndelTilVedtaksperiodeIdKobler
            is InnvilgelseEllerOpphørDagligReise -> DagligReiseAndelTilVedtaksperiodeIdKobler
            is InnvilgelseEllerOpphørReiseTilSamling -> ReiseTilSamlingAndelTilVedtaksperiodeIdKobler
            is InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise -> ReiseOppstartAndelTilVedtaksperiodeIdKobler
            is AvslagBoutgifter, is AvslagLæremidler, is AvslagPassAvBarn, is AvslagDagligReise, is AvslagReiseTilSamling -> null
        }
}

data object BarnetilsynAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørPassAvBarn

        val periode = finnPeriodeFraAndel(vedtak.beregningsresultat, andel)
        return vedtak.vedtaksperioder.filter { it.overlapper(periode) }
    }
}

data object LæremidlerAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørLæremidler
        val beregningsperioder =
            vedtak.beregningsresultat.perioder.filter {
                it.grunnlag.utbetalingsdato == andel.fom
            }

        return vedtak.vedtaksperioder.filter { vedtaksperiode ->
            beregningsperioder.any { b ->
                b.overlapper(vedtaksperiode)
            }
        }
    }
}

data object BoutgifterAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørBoutgifter

        val beregningsperiode =
            vedtak.beregningsresultat.perioder.filter {
                it.fom.tilFørsteDagIMåneden().datoEllerNesteMandagHvisLørdagEllerSøndag() == andel.fom
            }

        return vedtak.vedtaksperioder.filter {
            beregningsperiode.any { b -> b.overlapper(it) }
        }
    }
}

data object DagligReiseAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        val vedtak = vedtaksdata.data as InnvilgelseEllerOpphørDagligReise

        val andelTilhørerPrivatBil = andel.reiseId != null

        return if (andelTilhørerPrivatBil) {
            val beregningsresultat = vedtak.beregningsresultat.privatBil!!

            val reiseperioder =
                beregningsresultat.reiser
                    .single { it.reiseId == andel.reiseId }
                    .perioder

            val periode =
                reiseperioder.single {
                    it.fom.iDagHvisMandagEllerForrigeMandag() == andel.fom
                }

            vedtak.vedtaksperioder.filter {
                periode.overlapper(it)
            }
        } else {
            val beregningsresultat =
                vedtak.beregningsresultat.offentligTransport
                    ?: throw RuntimeException(
                        "Mangler beregningsresultat for offentlig transport i vedtak for behandling ${vedtaksdata.behandlingId}",
                    )

            val perioder =
                beregningsresultat.reiser.flatMap { reise ->
                    reise.perioder.filter {
                        it.grunnlag.fom.datoEllerNesteMandagHvisLørdagEllerSøndag() == andel.fom
                    }
                }

            val helPeriode =
                Datoperiode(fom = perioder.minOf { it.grunnlag.fom }, tom = perioder.maxOf { it.grunnlag.tom })

            vedtak.vedtaksperioder.filter {
                helPeriode.overlapper(it)
            }
        }
    }
}

data object ReiseTilSamlingAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        vedtaksdata.data as InnvilgelseEllerOpphørReiseTilSamling
        TODO("Implementeres av ansvarlig for REISE_TIL_SAMLING")
    }
}

data object ReiseOppstartAndelTilVedtaksperiodeIdKobler : AndelTilVedtaksperiodeIdKobler {
    override fun finnVedtaksperioder(
        andel: AndelTilkjentYtelse,
        vedtaksdata: GeneriskVedtak<out Vedtaksdata>,
    ): List<Vedtaksperiode> {
        vedtaksdata.data as InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
        TODO("Implementeres av ansvarlig for REISE_OPPSTART_AVSLUTNING_HJEMREISE")
    }
}
