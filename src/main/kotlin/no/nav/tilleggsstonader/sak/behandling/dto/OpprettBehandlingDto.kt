package no.nav.tilleggsstonader.sak.behandling.dto

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerKilde
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandling.opprettelse.ForenkletBehandlingstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate

data class OpprettBehandlingDto(
    val fagsakId: FagsakId,
    val årsak: BehandlingÅrsak,
    val nyeOpplysningerMetadata: NyeOpplysningerMetadataDto?,
    val valgteBarn: Set<String> = emptySet(),
    val kravMottatt: LocalDate?,
    val forenkletBehandlingstype: ForenkletBehandlingstype,
    val skalTillateFlereÅpneBehandlinger: Boolean = true,
    val skalSetteSaksbehandlerSomOppgaveEier: Boolean = true,
) {
    fun tilDomene() =
        OpprettRevurdering(
            fagsakId = fagsakId,
            årsak = årsak,
            nyeOpplysningerMetadata = nyeOpplysningerMetadata?.tilDomene(),
            valgteBarn = valgteBarn,
            kravMottatt = kravMottatt,
            skalOppretteOppgave = true,
            behandlingMetode = BehandlingMetode.MANUELL,
            forenkletBehandlingstype = forenkletBehandlingstype,
            skalTillateFlereÅpneBehandlinger = skalTillateFlereÅpneBehandlinger,
            skalSetteSaksbehandlerSomOppgaveEier = skalSetteSaksbehandlerSomOppgaveEier,
        )
}

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

data class OpprettRevurderingResponseDto(
    val status: Status,
    val behandlingId: BehandlingId? = null,
) {
    enum class Status {
        OPPRETTET,
        ÅPNE_BEHANDLINGER_FUNNET,
    }
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
