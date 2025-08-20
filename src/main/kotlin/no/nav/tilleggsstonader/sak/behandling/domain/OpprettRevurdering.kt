package no.nav.tilleggsstonader.sak.behandling.domain

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate

data class OpprettRevurdering(
    val fagsakId: FagsakId,
    val årsak: BehandlingÅrsak,
    val nyeOpplysningerMetadata: NyeOpplysningerMetadata?,
    val valgteBarn: Set<String>,
    val kravMottatt: LocalDate?,
    val skalOppretteOppgave: Boolean,
)
