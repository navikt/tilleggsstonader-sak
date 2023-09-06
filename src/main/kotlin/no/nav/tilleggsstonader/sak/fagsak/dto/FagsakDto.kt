package no.nav.tilleggsstonader.sak.fagsak.dto

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import java.util.*

data class FagsakDto(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdent: String,
    val stønadstype: Stønadstype,
    // val erLøpende: Boolean, TODO vurdere om disse skal være med
    // val erMigrert: Boolean,
    // val behandlinger: List<BehandlingDto>,
    val eksternId: Long,
)

fun FagsakDomain.tilDto(fagsakPerson: FagsakPerson): FagsakDto =
    FagsakDto(
        id = this.id,
        fagsakPersonId = this.fagsakPersonId,
        personIdent = fagsakPerson.hentAktivIdent(),
        stønadstype = this.stønadstype,
        //  behandlinger = behandlinger,
        eksternId = this.eksternId.id,
    )
