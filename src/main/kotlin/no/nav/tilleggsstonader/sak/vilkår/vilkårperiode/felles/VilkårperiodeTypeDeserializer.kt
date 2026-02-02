package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

class VilkårperiodeTypeDeserializer : ValueDeserializer<VilkårperiodeType>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): VilkårperiodeType = vilkårperiodetyper[p.text] ?: error("Finner ikke mapping for ${p.text}")
}
