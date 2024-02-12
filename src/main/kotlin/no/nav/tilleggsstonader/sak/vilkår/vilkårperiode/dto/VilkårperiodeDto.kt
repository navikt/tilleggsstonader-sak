package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.erSortert
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class VilkårperiodeDto(
    val id: UUID,
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val delvilkår: DelvilkårVilkårperiodeDto,
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val kilde: KildeVilkårsperiode,
    val slettetKommentar: String?,
    val sistEndret: LocalDateTime,
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
        delvilkår = this.delvilkår.tilDto(),
        resultat = this.resultat,
        begrunnelse = this.begrunnelse,
        kilde = this.kilde,
        slettetKommentar = this.slettetKommentar,
        sistEndret = this.sporbar.endret.endretTid,
    )

fun DelvilkårVilkårperiode.tilDto() = when (this) {
    is DelvilkårMålgruppe -> DelvilkårMålgruppeDto(
        medlemskap = medlemskap.tilDto(),
    )

    is DelvilkårAktivitet -> DelvilkårAktivitetDto(
        lønnet = lønnet.tilDto(),
        mottarSykepenger = mottarSykepenger.tilDto(),
    )
}

// Returnerer ikke vurdering hvis resultatet er IKKE_AKTUELT
fun DelvilkårVilkårperiode.Vurdering.tilDto() =
    this.takeIf { resultat != ResultatDelvilkårperiode.IKKE_AKTUELT }
        ?.let {
            VurderingDto(
                svar = svar,
                begrunnelse = begrunnelse,
            )
        }

data class Datoperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>, Mergeable<LocalDate, Datoperiode> {
    override fun merge(other: Datoperiode): Datoperiode {
        return if (this.inneholder(other)) this else this.copy(tom = other.tom)
    }
}

// TODO flytt til kontrakter
fun Periode<LocalDate>.formattertPeriodeNorskFormat() = "${this.fom.norskFormat()} - ${this.tom.norskFormat()}"

/**
 *  @receiver En sortert liste av vilkårsperioder
 *  @return En map kategorisert på periodetype med de oppfylte vilkårsperiodene. Periodene slåes sammen dersom
 *  de er sammenhengende, også selv om de har overlapp.
 */
fun List<VilkårperiodeDto>.mergeSammenhengendeOppfylteVilkårperioder(): Map<VilkårperiodeType, List<Datoperiode>> {
    feilHvisIkke(this.erSortert()) {
        "Vilkårsperioder må være sortert for merging"
    }

    return this.filter { it.resultat == ResultatVilkårperiode.OPPFYLT }.groupBy { it.type }
        .mapValues {
            it.value.map { Datoperiode(it.fom, it.tom) }
                .mergeSammenhengende { a, b -> a.overlapper(b) || a.tom.plusDays(1) == b.fom }
        }
}

data class LagreVilkårperiode(
    val behandlingId: UUID,
    @JsonDeserialize(using = VilkårperiodeTypeDeserializer::class)
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val delvilkår: DelvilkårVilkårperiodeDto,
    val begrunnelse: String? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppeDto::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitetDto::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiodeDto

data class DelvilkårMålgruppeDto(
    val medlemskap: VurderingDto?,
) : DelvilkårVilkårperiodeDto()

data class DelvilkårAktivitetDto(
    val lønnet: VurderingDto?,
    val mottarSykepenger: VurderingDto?,
) : DelvilkårVilkårperiodeDto()

data class VurderingDto(
    val svar: SvarJaNei? = null,
    val begrunnelse: String? = null,
)

data class SlettVikårperiode(
    val behandlingId: UUID,
    val kommentar: String,
)

data class VilkårperioderDto(
    val målgrupper: List<VilkårperiodeDto>,
    val aktiviteter: List<VilkårperiodeDto>,
)

fun Vilkårperioder.tilDto() = VilkårperioderDto(
    målgrupper = målgrupper.map(Vilkårperiode::tilDto),
    aktiviteter = aktiviteter.map(Vilkårperiode::tilDto),
)

class VilkårperiodeTypeDeserializer : JsonDeserializer<VilkårperiodeType>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): VilkårperiodeType {
        return vilkårperiodetyper[p.text] ?: error("Finner ikke mapping for ${p.text}")
    }
}
