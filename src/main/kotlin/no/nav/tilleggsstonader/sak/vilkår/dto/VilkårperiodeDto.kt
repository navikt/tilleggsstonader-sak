package no.nav.tilleggsstonader.sak.vilkår.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.domain.vilkårperiodetyper
import java.time.LocalDate
import java.util.UUID

data class VilkårperiodeDto(
    val id: UUID,
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val detaljer: DetaljerVilkårperiode,
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val kilde: KildeVilkårsperiode
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun Vilkårperiode.tilDto() =
    VilkårperiodeDto(
        id = this.id,
        type = this.type,
        fom = this.fom,
        tom = this.tom,
        detaljer = this.detaljer,
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
        kilde = this.kilde,
    )

data class Datoperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>, Mergeable<LocalDate, Datoperiode> {
    override fun merge(other: Datoperiode): Datoperiode {
        return this.copy(tom = other.tom)
    }
}

// TODO flytt til kontrakter
fun Periode<LocalDate>.formattertPeriodeNorskFormat() = "${this.fom.norskFormat()} - ${this.tom.norskFormat()}"

fun List<VilkårperiodeDto>.mergeSammenhengendeVilkårperioder(): Map<VilkårperiodeType, List<Datoperiode>> =
    this.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }.groupBy { it.type }
        .mapValues {
            it.value.map { Datoperiode(it.fom, it.tom) }
                .mergeSammenhengende { a, b -> a.tom.plusDays(1) == b.fom }
        }

data class OpprettVilkårperiode(
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val detaljer: DetaljerVilkårperiode,
    val begrunnelse: String? = null
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
