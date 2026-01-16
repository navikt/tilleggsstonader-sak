package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DagligReiseVedtaksperioderValideringService(
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakRepository: VedtakRepository,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ) {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )
        validerIkkeOverlappendeVedtaksperioderForTsrOgTso(
            behandling = behandling,
            vedtaksperioder = vedtaksperioder,
        )
    }

    fun validerIkkeOverlappendeVedtaksperioderForTsrOgTso(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val fagsakIdAnnenEnhet =
            when (behandling.stønadstype) {
                Stønadstype.DAGLIG_REISE_TSR -> fagsakService.finnFagsakerForFagsakPersonId(behandling.fagsakPersonId).dagligReiseTso?.id
                Stønadstype.DAGLIG_REISE_TSO -> fagsakService.finnFagsakerForFagsakPersonId(behandling.fagsakPersonId).dagligReiseTsr?.id
                else -> error("Kan ikke finne fagsakId for annen enhet for daglige reiser når stønadstype er: ${behandling.stønadstype}")
            }

        val vedtaksperioderAnnenEnhet =
            fagsakIdAnnenEnhet?.let {
                hentVedtaksdataForSisteIverksatteBehandling(it)?.vedtaksperioder
            }
        if (vedtaksperioderAnnenEnhet != null) {
            brukerfeilHvis(
                harOverlappendeVedtaksperioderPåTversAvEnheter(
                    vedtaksperioderDenneEnhenten = vedtaksperioder,
                    vedtaksperioderAnnenEnhet = vedtaksperioderAnnenEnhet,
                ),
            ) {
                "Kan ikke ha overlappende vedtaksperioder for Nay og Tiltaksenheten. Se oversikt øverst på siden for å finne overlappende vedtaksperiode."
            }
        }
    }

    private fun harOverlappendeVedtaksperioderPåTversAvEnheter(
        vedtaksperioderDenneEnhenten: List<Vedtaksperiode>,
        vedtaksperioderAnnenEnhet: List<Vedtaksperiode>,
    ) = vedtaksperioderDenneEnhenten.any { vedaksperiodeDenneEnheten ->
        vedtaksperioderAnnenEnhet.any { vedtaksperiodeAnnenEnhet ->
            vedaksperiodeDenneEnheten.overlapper(
                vedtaksperiodeAnnenEnhet,
            )
        }
    }

    private fun hentVedtaksdataForSisteIverksatteBehandling(fagsakId: FagsakId): InnvilgelseEllerOpphørDagligReise? =
        behandlingService
            .finnSisteIverksatteBehandling(fagsakId)
            ?.let {
                vedtakRepository.findByIdOrNull(it.id)?.withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
            }?.data
}
