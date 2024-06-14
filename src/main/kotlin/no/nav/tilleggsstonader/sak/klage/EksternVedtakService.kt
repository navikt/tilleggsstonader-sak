package no.nav.tilleggsstonader.sak.klage

import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.kontrakter.klage.Regelverk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {

    fun hentVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        return hentFerdigstilteBehandlinger(fagsak)
    }

    private fun hentFerdigstilteBehandlinger(fagsak: Fagsak): List<FagsystemVedtak> {
        return behandlingService.hentBehandlinger(fagsakId = fagsak.id)
            .filter { it.erAvsluttet() && it.resultat != BehandlingResultat.HENLAGT }
            .map { tilFagsystemVedtak(it) }
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {


        return FagsystemVedtak(
            eksternBehandlingId = behandlingService.hentEksternBehandlingId(behandling.id).id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = behandling.resultat.name,
            vedtakstidspunkt = behandling.vedtakstidspunkt
                ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR,
            regelverk = mapTilRegelverk(behandling.kategori),
        )
    }

    private fun mapTilRegelverk(kategori: BehandlingKategori) = when (kategori) {
        BehandlingKategori.EØS -> Regelverk.EØS
        BehandlingKategori.NASJONAL -> Regelverk.NASJONAL
    }
}
