package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.DokarkivBruker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Sak
import no.nav.tilleggsstonader.kontrakter.felles.Tema

enum class Behandlingstema(@JsonValue val value: String) {
    TilsynBarn("ab0300"),
    Læremidler("ab0292"),
    Feilutbetaling("ab0006"),
    Tilbakebetaling("ab0007"),
    Klage("ae0058"),
    BarnetilsynEF("ab0028"),
    ;

    companion object {
        private val behandlingstemaMap = values().associateBy(Behandlingstema::value) + values().associateBy { it.name }

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): Behandlingstema {
            return behandlingstemaMap[value] ?: error("Fant ikke Behandlingstema for value=$value")
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: DokarkivBruker? = null,
    val tema: Tema? = null,
    val behandlingstema: Behandlingstema? = null,
    val tittel: String? = null,
    val journalfoerendeEnhet: String? = null,
    val sak: Sak? = null,
    val dokumenter: List<DokumentInfo>? = null,
)
