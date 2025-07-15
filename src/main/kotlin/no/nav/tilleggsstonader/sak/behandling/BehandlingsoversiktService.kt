package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDetaljer
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingsoversiktDto
import no.nav.tilleggsstonader.sak.behandling.dto.FagsakMedBehandlinger
import no.nav.tilleggsstonader.sak.behandling.dto.Vedtaksperiode
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingsoversiktService(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val vedtaksperiodeService: VedtaksperiodeService,
) {
    fun hentOversikt(fagsakPersonId: FagsakPersonId): BehandlingsoversiktDto {
        val fagsak = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)

        return BehandlingsoversiktDto(
            fagsakPersonId = fagsakPersonId,
            tilsynBarn = hentFagsakMedBehandlinger(fagsak.barnetilsyn),
            læremidler = hentFagsakMedBehandlinger(fagsak.læremidler),
            boutgifter = hentFagsakMedBehandlinger(fagsak.boutgifter),
            dagligReiseTso = hentFagsakMedBehandlinger(fagsak.dagligReiseTSO),
        )
    }

    private fun hentFagsakMedBehandlinger(fagsak: Fagsak?): FagsakMedBehandlinger? {
        if (fagsak == null) return null
        val behandlinger = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)

        val vedtaksperioder = hentVedtaksperioder(behandlinger)

        return FagsakMedBehandlinger(
            fagsakId = fagsak.id,
            eksternFagsakId = fagsak.eksternId.id,
            stønadstype = fagsak.stønadstype,
            erLøpende = fagsakService.erLøpende(fagsak.id),
            behandlinger =
                behandlinger.map {
                    BehandlingDetaljer(
                        id = it.id,
                        forrigeIverksatteBehandlingId = it.forrigeIverksatteBehandlingId,
                        forrigeBehandlingId = it.forrigeIverksatteBehandlingId,
                        fagsakId = it.fagsakId,
                        steg = it.steg,
                        kategori = it.kategori,
                        type = it.type,
                        status = it.status,
                        sistEndret = it.sporbar.endret.endretTid,
                        resultat = it.resultat,
                        opprettet = it.sporbar.opprettetTid,
                        opprettetAv = it.sporbar.opprettetAv,
                        behandlingsårsak = it.årsak,
                        vedtaksdato = it.vedtakstidspunkt,
                        henlagtÅrsak = it.henlagtÅrsak,
                        henlagtBegrunnelse = it.henlagtBegrunnelse,
                        revurderFra = it.revurderFra,
                        vedtaksperiode = vedtaksperioder[it.id],
                    )
                },
        )
    }

    /**
     * Denne henter alle vedtaken for de behandlinger som finnes
     * Ønsket om å vise vedtaksperiode i behandlingsoversikten føles ikke helt landet.
     * Man burde kanskje haft en vedtaksperiode på behandling eller direkt på vedtaket for å enkelt hente ut informasjonen
     */
    private fun hentVedtaksperioder(behandlinger: List<Behandling>): Map<BehandlingId, Vedtaksperiode?> {
        val revurderFraPåBehandlingId =
            behandlinger
                .filter { it.resultat != BehandlingResultat.HENLAGT }
                .associate { it.id to it.revurderFra }

        return revurderFraPåBehandlingId.mapValues { (behandlingId, revurderFra) ->
            slåSammenVedtaksperioderForBehandling(behandlingId, revurderFra)
        }
    }

    /**
     * Slår sammen alle vedtaksperioder som finnes i en behandling slik at oversikten kun viser en periode.
     * Hvis det er innvilget flere vedtaksperioder med mellomrom i samme behandling, vil disse vises
     * som en sammenhengende periode, med en fom = første fom-dato og tom = siste tom-dato.
     */
    private fun slåSammenVedtaksperioderForBehandling(
        behandlingId: BehandlingId,
        revurdererFra: LocalDate?,
    ): Vedtaksperiode {
        val vedtaksperioder = vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId, revurdererFra)
        val minFom = vedtaksperioder.minOfOrNull { it.fom }
        val maksTom = vedtaksperioder.maxOfOrNull { it.tom }

        return Vedtaksperiode(fom = minFom, tom = maksTom)
    }
}
