package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.ToggleId

sealed interface RoutingContext {
    val ident: String
    val søknadstype: Søknadstype
}

enum class Søknadstype {
    BARNETILSYN,
    LÆREMIDLER,
    BOUTGIFTER,
    DAGLIG_REISE,
}

data class SkalRouteAlleSøkereTilNyLøsning(
    override val ident: String,
    override val søknadstype: Søknadstype,
) : RoutingContext {
    companion object {
        fun fraIdentStønadstype(identStønadstype: RoutingContext) =
            SkalRouteAlleSøkereTilNyLøsning(
                ident = identStønadstype.ident,
                søknadstype = identStønadstype.søknadstype,
            )
    }
}

data class SkalRouteEnkelteSøkereTilNyLøsning(
    override val ident: String,
    override val søknadstype: Søknadstype,
    val toggleId: ToggleId,
    val harGyldigStateIArena: (ArenaStatusDto) -> Boolean,
) : RoutingContext {
    companion object {
        fun fraIdentStønadstype(
            identStønadstype: RoutingContext,
            toggleId: ToggleId,
            harGyldigStateIArena: (ArenaStatusDto) -> Boolean,
        ) = SkalRouteEnkelteSøkereTilNyLøsning(
            ident = identStønadstype.ident,
            søknadstype = identStønadstype.søknadstype,
            toggleId = toggleId,
            harGyldigStateIArena = harGyldigStateIArena,
        )
    }
}

fun Søknadstype.tilStønadstyper(): Set<Stønadstype> =
    when (this) {
        Søknadstype.DAGLIG_REISE ->
            setOf(
                Stønadstype.DAGLIG_REISE_TSO,
                Stønadstype.DAGLIG_REISE_TSR,
            )

        Søknadstype.BOUTGIFTER -> setOf(Stønadstype.BOUTGIFTER)
        Søknadstype.LÆREMIDLER -> setOf(Stønadstype.LÆREMIDLER)
        Søknadstype.BARNETILSYN -> setOf(Stønadstype.BARNETILSYN)
    }

fun RoutingContext.tilRoutingContext() =
    when (this.søknadsType) {
        SøknadsType.BARNETILSYN -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
        SøknadsType.LÆREMIDLER -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
        SøknadsType.BOUTGIFTER -> SkalRouteAlleSøkereTilNyLøsning.fraIdentStønadstype(this)
        SøknadsType.DAGLIG_REISE -> SkalRouteEnkelteSøkereTilNyLøsning.fraIdentStønadstype(this, "", ::harAAP)
    }

// TODO - her må vi enten utvide ArenaStatusDto eller hente saker via AAP eller direkte til aren - TBC
fun harAAP(arenaStatus: ArenaStatusDto): Boolean {
    if (arenaStatus.sak.harAktivSakUtenVedtak && AAP) {
        return true
    } else {
        return false
    }
}
