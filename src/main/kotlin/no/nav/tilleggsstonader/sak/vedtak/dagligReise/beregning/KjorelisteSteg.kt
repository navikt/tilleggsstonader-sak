package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service

@Service
class KjorelisteSteg(
    private val behandlingService: BehandlingService,
    private val vedtakRepository: VedtakRepository,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    override fun utførOgReturnerNesteSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
        kanBehandlePrivatBil: Boolean,
    ): StegType {
        val beregningsResultatOffentligTransport =
            vedtakRepository
                .findByIdOrThrow(saksbehandling.id)
                .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
                .data
                .beregningsresultat
                ?.offentligTransport

        if (beregningsResultatOffentligTransport == null) {
            return StegType.SEND_TIL_BESLUTTER
        }
        return super.utførOgReturnerNesteSteg(saksbehandling, data, kanBehandlePrivatBil)
    }

    override fun stegType(): StegType = StegType.KJØRELISTE
}
