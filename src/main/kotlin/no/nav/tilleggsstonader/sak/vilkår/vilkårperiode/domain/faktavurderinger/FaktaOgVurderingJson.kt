package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import java.time.LocalDate

/**
 * Ønsket å legge json-spesifike saker i en egen fil for å unngå at de andre filene forsøples.
 * Prøvde å lage denne som en custom annotation men fikk det ikke til å virke [FaktaOgVurdering] extender nå [FaktaOgVurderingJson]
 */

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(UtdanningTilsynBarn::class, name = "UTDANNING_TILSYN_BARN"),
    JsonSubTypes.Type(TiltakTilsynBarn::class, name = "TILTAK_TILSYN_BARN"),
    JsonSubTypes.Type(ReellArbeidsøkerTilsynBarn::class, name = "REELL_ARBEIDSSØKER_TILSYN_BARN"),
    JsonSubTypes.Type(IngenAktivitetTilsynBarn::class, name = "INGEN_AKTIVITET_TILSYN_BARN"),

    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "AAP_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "OMSTILLINGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "OVERGANGSSTØNAD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "NEDSATT_ARBEIDSEVNE_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "UFØRETRYGD_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "SYKEPENGER_100_PROSENT_TILSYN_BARN"),
    JsonSubTypes.Type(MålgruppeTilsynBarn::class, name = "INGEN_MÅLGRUPPE_TILSYN_BARN"),
)
sealed interface FaktaOgVurderingJson

/*
private val typer: Map<String, TypeFaktaOgVurdering> =
    listOf(
        AktivitetTilsynBarnType.entries,
        MålgruppeTilsynBarnType.entries
    ).flatten().associateBy { it.name }

class TypeFaktaOgVurderingDeserializer : JsonDeserializer<TypeFaktaOgVurdering>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TypeFaktaOgVurdering {
        return typer[p.text] ?: error("Finner ikke mapping for ${p.text}")
    }
}

 */

fun main() {
    val målgruppe = MålgruppeTilsynBarn(
        type = MålgruppeTilsynBarnType.AAP_TILSYN_BARN,
        vurderinger = MålgruppeVurderinger(
            medlemskap = DelvilkårVilkårperiode.Vurdering(SvarJaNei.JA, ResultatDelvilkårperiode.OPPFYLT),
            dekketAvAnnetRegelverk = DelvilkårVilkårperiode.Vurdering(SvarJaNei.NEI, ResultatDelvilkårperiode.OPPFYLT),
        ),
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        begrunnelse = "yolo",
    )
    val json = objectMapper.writeValueAsString(målgruppe)
    val faktaOgVurdering = objectMapper.readValue<FaktaOgVurdering>(json)
    println(json)
    println(faktaOgVurdering)
    println(faktaOgVurdering is MålgruppeTilsynBarn)
    println(faktaOgVurdering is AktivitetTilsynBarn)
}
