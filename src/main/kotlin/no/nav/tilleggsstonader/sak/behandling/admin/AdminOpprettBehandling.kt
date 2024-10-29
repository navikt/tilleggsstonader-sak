package no.nav.tilleggsstonader.sak.behandling.admin

data class PersoninfoDto(
    val barn: List<Barn>,
)

data class Barn(
    val ident: String,
    val navn: String,
)

data class AdminOpprettFørstegangsbehandlingDto(
    val ident: String,
    val valgteBarn: Set<String>,
    val medBrev: Boolean = true,
)
