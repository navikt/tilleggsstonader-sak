package no.nav.tilleggsstonader.sak.migrering.arena

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

data class ArenaFinnesPersonRequest(
    val ident: String,
    val rettighet: String,
) {
    @get:JsonIgnore
    val stønadstype: Stønadstype get() = Rettighet.fraKodeArena(rettighet).stønadstype
}

data class ArenaFinnesPersonResponse(
    val ident: String,
    val finnes: Boolean,
)
