package no.nav.tilleggsstonader.sak.vilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
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
    val detaljer: DetaljerVilkårperiode,
    val begrunnelse: String?,
    val resultat: ResultatVilkårperiode,

    val slettetKommentar: String? = null,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        val ugyldigTypeOgDetaljer = (type is MålgruppeType && detaljer !is DetaljerMålgruppe) ||
            (type is AktivitetType && detaljer !is DetaljerAktivitet)
        feilHvis(ugyldigTypeOgDetaljer) {
            "Ugyldig kombinasjon type=${type.javaClass.simpleName} detaljer=${detaljer.javaClass.simpleName}"
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
    IKKE_TATT_STILLING_TIL,
    SLETTET,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(DetaljerMålgruppe::class, name = "målgruppe"),
    JsonSubTypes.Type(DetaljerAktivitet::class, name = "aktivitet"),
)
sealed class DetaljerVilkårperiode

data class DetaljerMålgruppe(
    val medlemskap: SvarJaNei,
) : DetaljerVilkårperiode()

data class DetaljerAktivitet(
    val lønnet: SvarJaNei,
    val mottarSykepenger: SvarJaNei,
) : DetaljerVilkårperiode()

enum class SvarJaNei {
    JA,
    JA_IMPLISITT,
    NEI,
    IKKE_VURDERT,
}

sealed interface VilkårperiodeType {
    fun tilDbType(): String
}

enum class MålgruppeType(val gyldigeAktiviter: Set<AktivitetType>) : VilkårperiodeType {
    AAP(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    AAP_FERDIG_AVKLART(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    DAGPENGER(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),
    UFØRETRYGD(setOf(AktivitetType.TILTAK, AktivitetType.UTDANNING)),

    OMSTILLINGSSTØNAD(setOf(AktivitetType.REEL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    OVERGANGSSTØNAD(setOf(AktivitetType.REEL_ARBEIDSSØKER, AktivitetType.UTDANNING)),
    ;

    override fun tilDbType(): String = this.name
}

enum class AktivitetType : VilkårperiodeType {
    TILTAK,
    UTDANNING,
    REEL_ARBEIDSSØKER,
    ;

    override fun tilDbType(): String = this.name
}

val vilkårperiodetyper: Map<String, VilkårperiodeType> =
    listOf(MålgruppeType.entries, AktivitetType.entries).flatten().associateBy { it.name }
