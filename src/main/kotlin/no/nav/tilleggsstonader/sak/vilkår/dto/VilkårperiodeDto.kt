package no.nav.tilleggsstonader.sak.vilkår.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.domain.vilkårperiodetyper
import java.time.LocalDate

data class VilkårperiodeDto(
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val vilkår: VilkårDto,
) : Periode<LocalDate>

fun Vilkårperiode.tilDto(vilkår: VilkårDto) =
    VilkårperiodeDto(
        type = this.type,
        fom = this.fom,
        tom = this.tom,
        vilkår = vilkår,
    )

data class OpprettVilkårperiode(
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class Vilkårperioder(
    val målgrupper: List<VilkårperiodeDto>,
    val aktiviteter: List<VilkårperiodeDto>,
)

// TODO test controller sånn at vi får testet denne
class VilkårperiodeTypeDeserializer : JsonDeserializer<VilkårperiodeType>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): VilkårperiodeType {
        return vilkårperiodetyper[p!!.text] ?: error("Finner ikke mapping for ${p.text}")
    }
}
