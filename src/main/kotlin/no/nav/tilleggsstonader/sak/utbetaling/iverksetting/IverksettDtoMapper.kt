package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import java.util.UUID

object IverksettDtoMapper {

    fun map(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
        totrinnskontroll: Totrinnskontroll,
        iverksettingId: UUID,
        forrigeIverksetting: ForrigeIverksettingDto?,
    ): IverksettDto {
        feilHvisIkke(tilkjentYtelse.andelerTilkjentYtelse.any { it.iverksetting?.iverksettingId == iverksettingId }) {
            "Må inneholde en andel med iverksettingId=$iverksettingId"
        }
        return IverksettDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.id,
            iverksettingId = iverksettingId,
            personident = behandling.ident,
            forrigeIverksetting = forrigeIverksetting,
            vedtak = mapVedtak(behandling, totrinnskontroll, tilkjentYtelse),
        )
    }

    private fun mapVedtak(
        behandling: Saksbehandling,
        totrinnskontroll: Totrinnskontroll,
        tilkjentYtelse: TilkjentYtelse,
    ): VedtaksdetaljerDto {
        return VedtaksdetaljerDto(
            vedtakstidspunkt = behandling.vedtakstidspunkt
                ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
            saksbehandlerId = totrinnskontroll.saksbehandler,
            beslutterId = totrinnskontroll.beslutter ?: error("Mangler beslutter"),
            utbetalinger = mapUtbetalinger(tilkjentYtelse),
        )
    }

    private fun mapUtbetalinger(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelse.andelerTilkjentYtelse.map {
            UtbetalingDto(
                beløp = it.beløp,
                satstype = it.satstype.tilSatstype(),
                fraOgMedDato = it.fom,
                tilOgMedDato = it.tom,
                stønadstype = it.type.tilStønadstype(),
                brukersNavKontor = null,
            )
        }

    private fun Satstype.tilSatstype(): SatstypeIverksetting = when (this) {
        Satstype.DAG -> SatstypeIverksetting.DAGLIG
        Satstype.MÅNED -> SatstypeIverksetting.MÅNEDLIG
        Satstype.ENGANGS -> SatstypeIverksetting.ENGANGS
    }

    private fun TypeAndel.tilStønadstype(): StønadstypeIverksetting = when (this) {
        TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> StønadstypeIverksetting.TILSYN_BARN_ENSLIG_FORSØRGER
        TypeAndel.TILSYN_BARN_AAP -> StønadstypeIverksetting.TILSYN_BARN_AAP
        TypeAndel.TILSYN_BARN_ETTERLATTE -> StønadstypeIverksetting.TILSYN_BARN_ETTERLATTE
    }
}
