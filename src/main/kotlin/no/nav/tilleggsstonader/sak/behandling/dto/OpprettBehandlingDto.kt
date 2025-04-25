package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerKilde
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate

data class OpprettBehandlingDto(
    val fagsakId: FagsakId,
    val årsak: BehandlingÅrsak,
    val nyeOpplysningerMetadata: NyeOpplysningerMetadataDto?,
    val valgteBarn: Set<String> = emptySet(),
    val kravMottatt: LocalDate?,
)

data class NyeOpplysningerMetadataDto(
    val kilde: NyeOpplysningerKilde,
    val endringer: List<NyeOpplysningerEndring>,
    val beskrivelse: String?,
) {
    fun tilDomene() =
        NyeOpplysningerMetadata(
            kilde = kilde,
            endringer = endringer,
            beskrivelse = beskrivelse,
        )
}

data class BarnTilRevurderingDto(
    val barn: List<Barn>,
) {
    data class Barn(
        val ident: String,
        val navn: String,
        val finnesPåForrigeBehandling: Boolean,
    )
}
