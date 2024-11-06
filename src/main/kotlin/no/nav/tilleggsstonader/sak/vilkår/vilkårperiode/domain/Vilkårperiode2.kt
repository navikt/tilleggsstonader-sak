package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("vilkarperiode")
data class Vilkårperiode2(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,

    @Column("forrige_vilkarperiode_id")
    val forrigeVilkårperiodeId: UUID? = null,
    val resultat: ResultatVilkårperiode,

    val slettetKommentar: String? = null,
    val status: Vilkårstatus? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),

    // fom tom
    val vurderingOgFakta: VurderingOgFakta
)


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "kategori"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Aktivitet::class, name = "AKTIVITET"),
)
sealed class VurderingOgFakta(
    val kategori: KategoriVilkårperiode,
    open val stønadstype: Stønadstype,
    open val type: VilkårperiodeType,
)

enum class KategoriVilkårperiode { MÅLGRUPPE, AKTIVITET }

data class Målgruppe(
    override val stønadstype: Stønadstype,
    override val type: MålgruppeType,
    //val vurdering: VurderingMålgruppe
) : VurderingOgFakta(KategoriVilkårperiode.MÅLGRUPPE, stønadstype, type)

@JsonDeserialize(using = AktivitetDeserializer::class)
sealed class Aktivitet(
    stønadstype: Stønadstype,
    type: AktivitetType,
    open val vurdering: VurderingAktivitet,
    open val fakta: FaktaAktivitet
) : VurderingOgFakta(KategoriVilkårperiode.AKTIVITET, stønadstype, type)


data class TiltakTilsynBarn(
    override val vurdering: VurderingTiltakTilsynBarn,
    override val fakta: FaktaAktivitetTilsynBarn
) : Aktivitet(Stønadstype.BARNETILSYN, AktivitetType.TILTAK, vurdering, fakta)

data class UtdanningTilsynBarn(
    override val vurdering: TomVurdering,
    override val fakta: FaktaAktivitetTilsynBarn
) : Aktivitet(Stønadstype.BARNETILSYN, AktivitetType.UTDANNING, vurdering, fakta)


data class TiltakLæremidler(
    override val vurdering: TomVurdering,
    override val fakta: FaktaAktivitetLæremidler
) : Aktivitet(Stønadstype.LÆREMIDLER, AktivitetType.TILTAK, vurdering, fakta)

data class UtdanningLæremidler(
    override val vurdering: TomVurdering,
    override val fakta: FaktaAktivitetLæremidler
) : Aktivitet(Stønadstype.LÆREMIDLER, AktivitetType.UTDANNING, vurdering, fakta)

sealed interface Vurdering
sealed interface VurderingAktivitet: Vurdering

data object TomVurdering : VurderingAktivitet

data class VurderingTiltakTilsynBarn(
    val lønnet: DelvilkårVilkårperiode.Vurdering,
) : VurderingAktivitet

sealed interface Fakta
sealed interface FaktaAktivitet: Fakta

data class FaktaAktivitetTilsynBarn(
    val aktivitetsdager: Int?
) : FaktaAktivitet

data class FaktaAktivitetLæremidler(
    val prosent: Int?,
    val studienivå: Studienivå?
) : FaktaAktivitet

enum class Studienivå { LAV, HØY }

class AktivitetDeserializer : JsonDeserializer<Aktivitet>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Aktivitet {
        val node: JsonNode = p.codec.readTree(p)
        val aktivitet = AktivitetType.valueOf(node.get("type").asText())
        val stønadstype = Stønadstype.valueOf(node.get("stønadstype").asText())

        return when (aktivitet) {
            AktivitetType.TILTAK -> when (stønadstype) {
                Stønadstype.BARNETILSYN -> TiltakTilsynBarn(
                    vurdering = parseVurdering(p, node),
                    fakta = parseFakta(p, node),
                )
                Stønadstype.LÆREMIDLER -> TiltakLæremidler(
                    vurdering = parseVurdering(p, node),
                    fakta = parseFakta(p, node),
                )
            }
            AktivitetType.UTDANNING -> when (stønadstype) {
                Stønadstype.BARNETILSYN -> parseAktivitet(p, node)
                Stønadstype.LÆREMIDLER -> UtdanningLæremidler(
                    vurdering = parseVurdering(p, node),
                    fakta = parseFakta(p, node),
                )
            }

            AktivitetType.REELL_ARBEIDSSØKER -> TODO()
            AktivitetType.INGEN_AKTIVITET -> TODO()
        }
    }

    private inline fun <reified T: Aktivitet> parseAktivitet(
        p: JsonParser,
        node: JsonNode
    ): T = p.codec.treeToValue(node, T::class.java)

    private inline fun <reified T: Vurdering> parseVurdering(
        p: JsonParser,
        node: JsonNode
    ): T = p.codec.treeToValue(node.get("vurdering"), T::class.java)

    private inline fun <reified T: Fakta> parseFakta(
        p: JsonParser,
        node: JsonNode
    ): T = p.codec.treeToValue(node.get("fakta"), T::class.java)
}


fun main() {
    yolo(UtdanningTilsynBarn(TomVurdering, FaktaAktivitetTilsynBarn(aktivitetsdager = 3)))
    yolo(UtdanningLæremidler(TomVurdering, FaktaAktivitetLæremidler(1, Studienivå.LAV)))
}

private fun yolo(data: VurderingOgFakta) {
    val json = objectMapper.writeValueAsString(data)
    println(json)
    val obj = objectMapper.readValue<VurderingOgFakta>(json)
    println(obj is VurderingOgFakta)
    println(obj is Målgruppe)
    println(obj is Aktivitet)
    println(obj is UtdanningTilsynBarn)
}



