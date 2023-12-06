package no.nav.tilleggsstonader.sak.vilkår.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("stonadsperiode")
data class Stønadsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    @Column("malgruppe")
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
)
