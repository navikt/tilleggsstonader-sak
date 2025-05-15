package no.nav.tilleggsstonader.sak.behandling.admin

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import java.time.LocalDate

data class PersoninfoDto(
    val navn: String,
    val barn: List<Barn>,
)

data class Barn(
    val ident: String,
    val navn: String,
)

data class AdminOpprettFørstegangsbehandlingHentPersonDto(
    val stønadstype: Stønadstype,
    val ident: String,
)

data class AdminOpprettFørstegangsbehandlingDto(
    val stønadstype: Stønadstype,
    val ident: String,
    val valgteBarn: Set<String>,
    val medBrev: Boolean = true,
    val kravMottatt: LocalDate,
)
