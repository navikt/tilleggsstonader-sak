package no.nav.tilleggsstonader.sak.vilkår.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("stonadsperiode")
data class Stønadsperiode(
    @Id
    val id: UUID,
    val behandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val type: VilkårperiodeType,
)