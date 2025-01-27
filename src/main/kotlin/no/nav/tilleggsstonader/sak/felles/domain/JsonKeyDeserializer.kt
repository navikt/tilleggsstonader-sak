package no.nav.tilleggsstonader.sak.felles.domain

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer

/**
 * Jackson håndterer ikke deserialsiering av en map av eks Map<BarnId, AnnenData>
 *     Workaround er å sette KeyDeserializer på BarnId
 * https://github.com/FasterXML/jackson-databind/issues/4444
 */
internal abstract class JsonKeyDeserializer<T>(
    private val fn: (String) -> T,
) : KeyDeserializer() {
    override fun deserializeKey(
        key: String,
        ctxt: DeserializationContext?,
    ): T = fn(key)
}

internal class BarnIdKeyDeserializer : JsonKeyDeserializer<BarnId>({ BarnId.fromString(it) })
