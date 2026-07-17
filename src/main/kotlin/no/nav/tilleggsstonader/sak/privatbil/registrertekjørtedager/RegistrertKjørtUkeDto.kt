package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate
import java.util.UUID

data class RegistrertKjørtUkeDto(
    val id: UUID,
    val reiseId: ReiseId,
    val begrunnelse: String?,
    val dager: List<RegistrertKjørtDagDto>,
)

data class RegistrertKjørtDagDto(
    val id: UUID,
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)

fun RegistrertKjørtUke.tilDto(): RegistrertKjørtUkeDto =
    RegistrertKjørtUkeDto(
        id = id,
        reiseId = reiseId,
        begrunnelse = begrunnelse,
        dager = dager.map { it.tilDto() },
    )

fun RegistrertKjørtDag.tilDto(): RegistrertKjørtDagDto =
    RegistrertKjørtDagDto(
        id = id,
        dato = dato,
        harKjørt = harKjørt,
        parkeringsutgift = parkeringsutgift,
    )
