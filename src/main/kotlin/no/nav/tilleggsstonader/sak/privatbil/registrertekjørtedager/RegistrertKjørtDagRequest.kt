package no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

data class RegistrertKjørtDagRequest(
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int? = null,
)

data class RegistrertKjørtUkePostRequest(
    val reiseId: ReiseId,
    val begrunnelse: String?,
    val dager: List<RegistrertKjørtDagRequest>,
)

data class RegistrertKjørtUkePutRequest(
    val begrunnelse: String?,
    val dager: List<RegistrertKjørtDagRequest>,
)
