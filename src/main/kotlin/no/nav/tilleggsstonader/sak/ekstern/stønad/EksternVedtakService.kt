package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.VedtaksinformasjonTilsynBarnDto
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    private val personService: PersonService,
    private val arenaService: ArenaService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {

    /**
     * Henter foreløpig kun informasjon om det finnes et vedtak i ny løsning og/eller i arena
     * Når vi legger inn informasjon om hvilke perioder det gjelder,
     * så må vi slå sammen beregningsresultat tvers flere behandlinger pga revurderFra som ikke tar med perioder før måneden som det revurderes fra
     */
    fun hentVedtaksinformasjonTilsynBarn(request: IdentRequest): VedtaksinformasjonTilsynBarnDto {
        return VedtaksinformasjonTilsynBarnDto(
            harInnvilgetVedtak = harVedtak(request) || harVedtakIArena(request),
        )
    }

    private fun harVedtak(request: IdentRequest): Boolean {
        val identer = personService.hentPersonIdenter(request.ident).identer()
        val sisteIverksatteBehandling = fagsakService.finnFagsak(identer, stønadstype = Stønadstype.BARNETILSYN)
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
        return sisteIverksatteBehandling != null
    }

    private fun harVedtakIArena(request: IdentRequest) =
        arenaService.hentStatus(request.ident, Stønadstype.BARNETILSYN).vedtak.harInnvilgetVedtak
}
