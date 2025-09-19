package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.ToggleId

sealed interface RoutingContext {
    val ident: String
    val søknadsType: SøknadsType
}

enum class SøknadsType {
    BARNETILSYN,
    LÆREMIDLER,
    BOUTGIFTER,
    DAGLIG_REISE,
}

data class SkalRouteAlleSøkereTilNyLøsning(
    override val ident: String,
    override val søknadsType: SøknadsType,
) : RoutingContext {
    companion object {
        fun fraIdentStønadstype(identStønadstype: RoutingContext) =
            SkalRouteAlleSøkereTilNyLøsning(
                ident = identStønadstype.ident,
                søknadsType = identStønadstype.søknadsType,
            )
    }
}

data class SkalRouteEnkelteSøkereTilNyLøsning(
    override val ident: String,
    override val søknadsType: SøknadsType,
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
            søknadsType = identStønadstype.søknadsType,
            toggleId = toggleId,
            harGyldigStateIArena = harGyldigStateIArena,
        )
    }
}

// Extension som gir deg mappingen
fun SøknadsType.tilStønadstyper(): Set<Stønadstype> =
    when (this) {
        SøknadsType.DAGLIG_REISE ->
            setOf(
                Stønadstype.DAGLIG_REISE_TSO,
                Stønadstype.DAGLIG_REISE_TSR,
            )

        SøknadsType.BOUTGIFTER -> setOf(Stønadstype.BOUTGIFTER)
        SøknadsType.LÆREMIDLER -> setOf(Stønadstype.LÆREMIDLER)
        SøknadsType.BARNETILSYN -> setOf(Stønadstype.BARNETILSYN)
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
