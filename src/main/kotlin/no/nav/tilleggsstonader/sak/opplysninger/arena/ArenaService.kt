package no.nav.tilleggsstonader.sak.opplysninger.arena

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdenterRequest
import no.nav.tilleggsstonader.kontrakter.felles.IdenterStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ArenaService(
    private val arenaClient: ArenaClient,
    private val personService: PersonService,
    private val behandlingService: BehandlingService,
) {

    fun hentStatus(behandlingId: UUID): ArenaStatusDto {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        return hentStatus(saksbehandling.ident, saksbehandling.stønadstype)
    }

    fun hentStatus(ident: String, stønadstype: Stønadstype): ArenaStatusDto {
        val identer = personService.hentPersonIdenter(ident).identer()
        return arenaClient.hentStatus(IdenterStønadstype(identer, stønadstype))
    }

    fun harSaker(ident: String): Boolean {
        val identer = personService.hentPersonIdenter(ident).identer()
        return arenaClient.harSaker(IdenterRequest(identer)).harSaker
    }
}
