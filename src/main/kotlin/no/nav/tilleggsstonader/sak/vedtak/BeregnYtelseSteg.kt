package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory

/**
 * Splitter opp BeregnYtelseSteg for ulike stønadstyper
 * Denne håndterer sletting av tidligere vedtak og andeler
 */
abstract class BeregnYtelseSteg<DTO : Any>(
    private val stønadstype: Stønadstype,
    open val vedtakRepository: VedtakRepository,
    open val tilkjentytelseService: TilkjentYtelseService,
    open val simuleringService: SimuleringService,
) : BehandlingSteg<DTO> {

    val logger = LoggerFactory.getLogger(javaClass)

    override fun utførSteg(saksbehandling: Saksbehandling, data: DTO) {
        logger.info("Lagrer vedtak for behandling=${saksbehandling.id} vedtak=${data::class.simpleName}")
        validerStønadstype(saksbehandling)
        nullstillEksisterendeVedtakPåBehandling(saksbehandling)
        lagreVedtak(saksbehandling, data)
    }

    protected abstract fun lagreVedtak(saksbehandling: Saksbehandling, vedtak: DTO)

    private fun nullstillEksisterendeVedtakPåBehandling(saksbehandling: Saksbehandling) {
        vedtakRepository.deleteById(saksbehandling.id)
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
