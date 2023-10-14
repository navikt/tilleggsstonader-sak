package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService

/**
 * Splitter opp BeregnYtelseSteg for ulike stønadstyper
 * Denne håndterer sletting av tidligere vedtak og andeler
 */
abstract class BeregnYtelseSteg<T>(
    private val stønadstype: Stønadstype,
    open val tilkjentytelseService: TilkjentYtelseService,
    open val simuleringService: SimuleringService,
) : BehandlingSteg<T> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: T) {
        validerStønadstype(saksbehandling)
        nullstillEksisterendeVedtakPåBehandling(saksbehandling)
        lagreVedtak(saksbehandling, data)
    }

    abstract fun lagreVedtak(saksbehandling: Saksbehandling, data: T)

    abstract fun slettVedtak(saksbehandling: Saksbehandling)

    private fun nullstillEksisterendeVedtakPåBehandling(saksbehandling: Saksbehandling) {
        slettVedtak(saksbehandling)
        tilkjentytelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    private fun validerStønadstype(saksbehandling: Saksbehandling) {
        feilHvisIkke(saksbehandling.stønadstype == stønadstype) {
            "${this::class.java.simpleName} tillater kun $stønadstype"
        }
    }

    override fun stegType(): StegType = StegType.BEREGNE_YTELSE
}
