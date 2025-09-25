package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

enum class Søknadstype {
    BARNETILSYN,
    LÆREMIDLER,
    BOUTGIFTER,
    DAGLIG_REISE,
}

fun Stønadstype.tilSøknadstype() =
    when (this) {
        Stønadstype.BARNETILSYN -> Søknadstype.BARNETILSYN
        Stønadstype.LÆREMIDLER -> Søknadstype.LÆREMIDLER
        Stønadstype.BOUTGIFTER -> Søknadstype.BOUTGIFTER
        Stønadstype.DAGLIG_REISE_TSO -> Søknadstype.DAGLIG_REISE
        Stønadstype.DAGLIG_REISE_TSR -> Søknadstype.DAGLIG_REISE
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
