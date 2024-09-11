package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId

data class FagsakDto(
    val id: FagsakId,
    val fagsakPersonId: FagsakPersonId,
    val personIdent: String,
    val stønadstype: Stønadstype,
    val erLøpende: Boolean,
    // val erMigrert: Boolean,
    val behandlinger: List<BehandlingDto>,
    val eksternId: Long,
)

fun Fagsak.tilDto(behandlinger: List<BehandlingDto>, erLøpende: Boolean): FagsakDto =
    FagsakDto(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personIdent = this.hentAktivIdent(),
        stønadstype = this.stønadstype,
        erLøpende = erLøpende,
        behandlinger = behandlinger,
        eksternId = this.eksternId.id,
    )
