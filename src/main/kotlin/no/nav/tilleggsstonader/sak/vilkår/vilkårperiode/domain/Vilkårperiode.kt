package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("vilkar_periode")
data class Vilkårperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val kilde: KildeVilkårsperiode,

    val fom: LocalDate,
    val tom: LocalDate,
    val type: VilkårperiodeType,
    @Column("delvilkar")
    val delvilkår: DelvilkårVilkårperiode,
    val begrunnelse: String?,
    val resultat: ResultatVilkårperiode,

    val slettetKommentar: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }

        val ugyldigTypeOgDetaljer = (type is MålgruppeType && delvilkår !is DelvilkårMålgruppe) ||
            (type is AktivitetType && delvilkår !is DelvilkårAktivitet)
        feilHvis(ugyldigTypeOgDetaljer) {
            "Ugyldig kombinasjon type=${type.javaClass.simpleName} detaljer=${delvilkår.javaClass.simpleName}"
        }

        validerSlettefelter()
    }

    private fun validerSlettefelter() {
        if (resultat == ResultatVilkårperiode.SLETTET) {
            feilHvis(kilde != KildeVilkårsperiode.MANUELL) {
                "Kan ikke slette når kilde=$kilde"
            }
            feilHvis(slettetKommentar.isNullOrBlank()) {
                "Mangler kommentar for resultat=$resultat"
            }
        } else {
            feilHvis(!slettetKommentar.isNullOrBlank()) {
                "Kan ikke ha slettetkommentar med resultat=$resultat"
            }
        }
    }
}

enum class KildeVilkårsperiode {
    MANUELL,
    SYSTEM,
}

enum class ResultatVilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    SLETTET,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DelvilkårMålgruppe::class, name = "MÅLGRUPPE"),
    JsonSubTypes.Type(DelvilkårAktivitet::class, name = "AKTIVITET"),
)
sealed class DelvilkårVilkårperiode {
    data class Vurdering(
        val svar: SvarJaNei?,
        val begrunnelse: String? = null,
        val resultat: ResultatDelvilkårperiode,
    ) {
        init {
            feilHvis(svar == SvarJaNei.JA_IMPLISITT && begrunnelse != null) {
                "Kan ikke ha begrunnelse når svar=$svar"
            }
            feilHvis(resultat == ResultatDelvilkårperiode.IKKE_AKTUELT && (svar != null || begrunnelse != null)) {
                "Ugyldig resultat=$resultat når svar=$svar begrunnelseErNull=${begrunnelse == null}"
            }
        }
    }
}

enum class ResultatDelvilkårperiode {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
    IKKE_AKTUELT,
}

data class DelvilkårMålgruppe(
    val medlemskap: Vurdering,
) : DelvilkårVilkårperiode()

data class DelvilkårAktivitet(
    val lønnet: Vurdering,
    val mottarSykepenger: Vurdering,
) : DelvilkårVilkårperiode()

enum class SvarJaNei {
    JA,
    JA_IMPLISITT,
    NEI,
}

sealed interface VilkårperiodeType {
    fun tilDbType(): String
}

enum class MålgruppeType(val gyldigeAktiviter: Set<AktivitetType>) : VilkårperiodeType {
    AAP(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    DAGPENGER(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    OMSTILLINGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    OVERGANGSSTØNAD(setOf(AktivitetType.REELL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    NEDSATT_ARBEIDSEVNE(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    UFØRETRYGD(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    ;

    override fun tilDbType(): String = this.name
}

enum class AktivitetType : VilkårperiodeType {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    ;

    override fun tilDbType(): String = this.name
}

val vilkårperiodetyper: Map<String, VilkårperiodeType> =
    listOf(MålgruppeType.entries, AktivitetType.entries).flatten().associateBy { it.name }

data class Vilkårperioder(
    val målgrupper: List<Vilkårperiode>,
    val aktiviteter: List<Vilkårperiode>,
)
