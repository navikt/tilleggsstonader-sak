package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper

class VilkårperiodeTypeDeserializer : JsonDeserializer<VilkårperiodeType>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): VilkårperiodeType = vilkårperiodetyper[p.text] ?: error("Finner ikke mapping for ${p.text}")
}
