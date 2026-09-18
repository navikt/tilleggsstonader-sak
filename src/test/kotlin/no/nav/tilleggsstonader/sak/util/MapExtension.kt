package no.nav.tilleggsstonader.sak.util

/**
 * Sorterer et [Map] på nøkler, og gjør det samme rekursivt for verdier som selv er [Map] eller inneholder [Map]
 * (f.eks. i en [List]). Brukes for å kunne sammenligne to json-strukturer uavhengig av rekkefølgen på feltene.
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toDeepSortedMap(): Map<String, Any?> = toSortedMap().mapValues { (_, value) -> value.toDeepSortedValue() }

@Suppress("UNCHECKED_CAST")
private fun Any?.toDeepSortedValue(): Any? =
    when (this) {
        is Map<*, *> -> (this as Map<String, Any?>).toDeepSortedMap()
        is List<*> -> this.map { it.toDeepSortedValue() }
        else -> this
    }