package no.nav.tilleggsstonader.sak.util

/**
 * Sorterer et [Map] på nøkler, og gjør det samme rekursivt for verdier som selv er [Map] eller inneholder [Map]
 * (f.eks. i en [List]). Brukes for å kunne sammenligne to json-strukturer uavhengig av rekkefølgen på feltene.
 */
fun Map<String, Any?>.toDeepSortedMap(): Map<String, Any?> = toSortedMap().mapValues { (_, value) -> value.toDeepSorted() }

/**
 * Sorterer en json-struktur (Map/List, evt. nøstet i hverandre) rekursivt på nøkler.
 * I motsetning til [toDeepSortedMap] fungerer denne uansett om roten er et objekt eller en liste,
 * noe som trengs når man f.eks. sammenligner json der roten er en json-array.
 */
@Suppress("UNCHECKED_CAST")
fun Any?.toDeepSorted(): Any? =
    when (this) {
        is Map<*, *> -> (this as Map<String, Any?>).toDeepSortedMap()
        is List<*> -> this.map { it.toDeepSorted() }
        else -> this
    }
