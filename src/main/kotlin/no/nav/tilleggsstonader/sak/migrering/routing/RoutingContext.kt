package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.ToggleId

sealed interface RoutingContext {
    val ident: String
    val stønadstype: Stønadstype
}

data class SkalRouteAlleSøkereTilNyLøsning(
    override val ident: String,
    override val stønadstype: Stønadstype,
) : RoutingContext {
    companion object {
        fun fraIdentStønadstype(identStønadstype: IdentStønadstype) =
            SkalRouteAlleSøkereTilNyLøsning(
                ident = identStønadstype.ident,
                stønadstype = identStønadstype.stønadstype,
            )
    }
}

data class FeatureTogglet(
    override val ident: String,
    override val stønadstype: Stønadstype,
    val toggleId: ToggleId,
    val harGyldigStateIArena: (ArenaStatusDto) -> Boolean,
) : RoutingContext {
    companion object {
        fun fraIdentStønadstype(
            identStønadstype: IdentStønadstype,
            toggleId: ToggleId,
            harGyldigStateIArena: (ArenaStatusDto) -> Boolean,
        ) = FeatureTogglet(
            ident = identStønadstype.ident,
            stønadstype = identStønadstype.stønadstype,
            toggleId = toggleId,
            harGyldigStateIArena = harGyldigStateIArena,
        )
    }
}

fun IdentStønadstype.tilRoutingContext() =
    when (this.stønadstype) {
        Stønadstype.BARNETILSYN -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
        Stønadstype.LÆREMIDLER -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
        Stønadstype.BOUTGIFTER -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
    }
