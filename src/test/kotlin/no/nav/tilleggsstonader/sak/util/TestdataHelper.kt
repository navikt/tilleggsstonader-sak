package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn

fun søknadBarnTilBehandlingBarn(
    barn: Collection<SøknadBarn>,
    behandlingId: BehandlingId = BehandlingId.randomUUID(),
): List<BehandlingBarn> =
    barn.map {
        it.tilBehandlingBarn(behandlingId)
    }

fun SøknadBarn.tilBehandlingBarn(behandlingId: BehandlingId) = BehandlingBarn(
    behandlingId = behandlingId,
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
