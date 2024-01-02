package no.nav.tilleggsstonader.sak.vilkår.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("vilkar_periode")
data class Vilkårperiode(
    @Id
    @Column("vilkar_id")
    val vilkårId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val type: VilkårperiodeType,
)

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
