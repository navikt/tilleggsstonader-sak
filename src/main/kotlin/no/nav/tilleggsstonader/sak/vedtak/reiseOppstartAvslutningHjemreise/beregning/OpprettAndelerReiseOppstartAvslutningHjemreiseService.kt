package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.mapTilAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class OpprettAndelerReiseOppstartAvslutningHjemreiseService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun lagreAndelerForBehandling(saksbehandling: Saksbehandling) {
        val vedtak =
            vedtakService.hentVedtak<InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise>(saksbehandling.id)
        val beregningsresultat = vedtak.data.beregningsresultat
        val vedtaksperioder = vedtak.data.vedtaksperioder
        val aktiviteter = vilkårperiodeService.hentVilkårperioder(saksbehandling.id).aktiviteter

        val andelerOffentligTransport =
            beregningsresultat.offentligTransport.map {
                it.mapTilAndelTilkjentYtelse(saksbehandling, vedtaksperioder, aktiviteter)
            }

        val andelerPrivatBil =
            beregningsresultat.privatBil.map {
                it.mapTilAndelTilkjentYtelse(saksbehandling, vedtaksperioder, aktiviteter)
            }

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = andelerOffentligTransport + andelerPrivatBil,
        )
    }
}
