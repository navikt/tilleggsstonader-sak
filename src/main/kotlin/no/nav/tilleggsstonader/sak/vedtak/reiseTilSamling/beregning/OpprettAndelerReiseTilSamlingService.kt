package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.mapTilAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.springframework.stereotype.Service

@Service
class OpprettAndelerReiseTilSamlingService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun lagreAndelerForBehandling(saksbehandling: Saksbehandling) {
        val beregningsresultat = vedtakService.hentVedtak<InnvilgelseEllerOpphørReiseTilSamling>(saksbehandling.id).data.beregningsresultat

        val andeler =
            beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling) {
                hentTiltaksvariantHvisTsr(saksbehandling, it)
            }

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = andeler,
        )
    }

    private fun hentTiltaksvariantHvisTsr(
        saksbehandling: Saksbehandling,
        aktivitetId: VilkårperiodeGlobalId?,
    ): TypeAktivitet? =
        if (saksbehandling.stønadstype == Stønadstype.REISE_TIL_SAMLING_TSR) {
            hentTiltaksvariantFraAktivitet(aktivitetId, saksbehandling.id)
        } else {
            null
        }

    private fun hentTiltaksvariantFraAktivitet(
        aktivitetId: VilkårperiodeGlobalId?,
        behandlingId: BehandlingId,
    ): TypeAktivitet {
        feilHvis(aktivitetId == null) {
            "Aktivitet må være satt for Reise til samling TSR"
        }
        val aktivitet =
            vilkårperiodeService.hentAktivitet(aktivitetId, behandlingId)
                ?: feil("Fant ikke aktivitet for aktivitetId=$aktivitetId")

        return aktivitet.tiltaksvariant
            ?: feil("Tiltaksvariant mangler på aktivitet med aktivitetId=$aktivitetId")
    }
}
