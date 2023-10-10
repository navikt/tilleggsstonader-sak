package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import java.util.UUID

fun søknadBarnTilBehandlingBarn(barn: Collection<SøknadBarn>, behandlingId: UUID = UUID.randomUUID()): List<BehandlingBarn> =
    barn.map {
        it.tilBehandlingBarn(behandlingId)
    }

fun SøknadBarn.tilBehandlingBarn(behandlingId: UUID) = BehandlingBarn(
    behandlingId = behandlingId,
    søknadBarnId = this.id,
    ident = this.ident,
)
/*
fun BarnMedIdent.tilBehandlingBarn(behandlingId: UUID) = BehandlingBarn(
    behandlingId = behandlingId,
    søknadBarnId = null,
    personIdent = this.personIdent,
    navn = this.navn.visningsnavn(),
    fødselTermindato = null,
)
*/
