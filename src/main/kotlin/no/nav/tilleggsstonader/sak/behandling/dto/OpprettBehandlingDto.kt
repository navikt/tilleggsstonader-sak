package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandling.domain.ÅrsakMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.ÅrsakMetadataKilde
import no.nav.tilleggsstonader.sak.behandling.opprettelse.ForenkletBehandlingstype
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate

data class OpprettBehandlingDto(
    val fagsakId: FagsakId,
    val årsak: BehandlingÅrsak,
    val årsakMetadata: ÅrsakMetadataDto?,
    val valgteBarn: Set<String> = emptySet(),
    val kravMottatt: LocalDate?,
    val forenkletBehandlingstype: ForenkletBehandlingstype,
) {
    fun tilDomene() =
        OpprettRevurdering(
            fagsakId = fagsakId,
            årsak = årsak,
            årsakMetadata = årsakMetadata?.tilDomene(),
            valgteBarn = valgteBarn,
            kravMottatt = kravMottatt,
            skalOppretteOppgave = true,
            behandlingMetode = BehandlingMetode.MANUELL,
            forenkletBehandlingstype = forenkletBehandlingstype,
        )
}

data class ÅrsakMetadataDto(
    val kilde: ÅrsakMetadataKilde,
    val beskrivelse: String?,
    val endringer: List<NyeOpplysningerEndring>,
) {
    fun tilDomene() =
        ÅrsakMetadata(
            kilde = kilde,
            beskrivelse = beskrivelse,
            endringer = endringer,
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
