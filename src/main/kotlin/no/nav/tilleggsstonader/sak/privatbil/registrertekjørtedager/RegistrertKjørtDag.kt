package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("registrert_kjort_dag")
data class RegistrertKjørtDag(
    @Id val id: UUID = UUID.randomUUID(),
    val dato: LocalDate,
    @Column("har_kjort") val harKjørt: Boolean,
    val parkeringsutgift: Int? = null,
)
