package no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class OppdaterBehandlingstypeRequest(
    val versjon: Int,
    @JsonInclude(JsonInclude.Include.ALWAYS) val behandlingstype: String?,
    val endretAvEnhetsnr: String?,
)
