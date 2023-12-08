package no.nav.tilleggsstonader.sak.vilkår.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
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

// TODO: Slett når kontrakter oppdateres
data class Datoperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>, Mergeable<LocalDate, Datoperiode> {
    override fun merge(other: Datoperiode): Datoperiode {
        return this.copy(tom = other.tom)
    }

    fun inneholder(other: Periode<LocalDate>): Boolean {
        return this.fom <= other.fom && this.tom >= other.tom
    }
}

fun List<VilkårperiodeDto>.mergeSammenhengendeVilkårperioder(): Map<VilkårperiodeType, List<Datoperiode>> =
    this.filter { it.vilkår.resultat == Vilkårsresultat.OPPFYLT }.groupBy { it.type }
        .mapValues {
            it.value.map { Datoperiode(it.fom, it.tom) }
                .mergeSammenhengende { a, b -> a.tom.plusDays(1) == b.fom }
        }

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

class VilkårperiodeTypeDeserializer : JsonDeserializer<VilkårperiodeType>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): VilkårperiodeType {
        return vilkårperiodetyper[p.text] ?: error("Finner ikke mapping for ${p.text}")
    }
}
