package no.nav.tilleggsstonader.sak.migrering.arena

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil

data class ArenaFinnesPersonRequest(
    val ident: String,
    val rettighet: String,
) {
    @get:JsonIgnore
    val stønadstype: Stønadstype
        get() = try {
            Rettighet.fraKodeArena(rettighet).stønadstype
        } catch (e: IllegalStateException) {
            throw Feil(e.message ?: "Ukjent feil")
        }
}

data class ArenaFinnesPersonResponse(
    val ident: String,
    val finnes: Boolean,
)
