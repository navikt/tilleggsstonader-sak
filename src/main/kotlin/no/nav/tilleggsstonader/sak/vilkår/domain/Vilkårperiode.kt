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

enum class VilkårperiodeType {
    // MÅLGRUPPE
    AAP,
    AAP_FERDIG_AVKLART,

    // AKTIVITET
    TILTAK,
    UTDANNING,
}
