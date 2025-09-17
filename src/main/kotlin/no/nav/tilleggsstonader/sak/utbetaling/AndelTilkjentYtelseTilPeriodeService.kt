package no.nav.tilleggsstonader.sak.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import org.springframework.stereotype.Service
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.finnPeriodeFraAndel as finnPeriodeTilsynBarnFraAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.finnPeriodeFraAndel as finnPeriodeBoutgifterFraAndel
import no.nav.tilleggsstonader.sak.vedtak.læremidler.finnPerioderFraAndel as finnPerioderLæremidlerFraAndel

@Service
class AndelTilkjentYtelseTilPeriodeService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
) {
    fun mapAndelerTilVedtaksperiodeForBehandling(behandlingId: BehandlingId): List<AndelMedVedtaksperioder> {
        val andeler = tilkjentYtelseService.hentForBehandling(behandlingId).andelerTilkjentYtelse
        val vedtak = vedtakService.hentVedtak(behandlingId) ?: error("Behandling $behandlingId har ingen vedtak")

        return andeler.map {
            AndelMedVedtaksperioder(
                andelTilkjentYtelse = it,
                vedtaksperiode = if (it.erNullandel()) null else finnFaktiskPeriodeForAndel(it, vedtak),
            )
        }
    }

    private fun finnFaktiskPeriodeForAndel(
        andelTilkjentYtelse: AndelTilkjentYtelse,
        vedtak: Vedtak,
    ): Periode<LocalDate> {
        val vedtakdata = vedtak.data
        return when (vedtakdata) {
            is InnvilgelseEllerOpphørBoutgifter ->
                finnPeriodeBoutgifterFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { Datoperiode(it.fom, it.tom) }

            is InnvilgelseEllerOpphørTilsynBarn ->
                finnPeriodeTilsynBarnFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { Datoperiode(it.fom, it.tom) }

            is InnvilgelseEllerOpphørLæremidler ->
                finnPerioderLæremidlerFraAndel(vedtakdata.beregningsresultat, andelTilkjentYtelse)
                    .let { perioder -> Datoperiode(perioder.minOf { it.fom }, perioder.maxOf { it.tom }) }

            is InnvilgelseEllerOpphørDagligReise -> TODO() // Vil gi mer mening å returnere flere perioder?
            else -> error("Behandling ${vedtak.behandlingId} har ikke et iverksatt vedtak")
        }
    }
}

data class AndelMedVedtaksperioder(
    val andelTilkjentYtelse: AndelTilkjentYtelse,
    val vedtaksperiode: Periode<LocalDate>?,
)
