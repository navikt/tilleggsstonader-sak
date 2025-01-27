package no.nav.tilleggsstonader.sak.opplysninger.pdl.dto

data class PdlPersonRequestVariables(
    val ident: String,
)

data class PdlIdentRequestVariables(
    val ident: String,
    val gruppe: String,
    val historikk: Boolean = false,
)

data class PdlPersonBolkRequestVariables(
    val identer: List<String>,
)

data class PdlIdentBolkRequestVariables(
    val identer: List<String>,
    val gruppe: String,
)

data class PdlPersonSøkRequestVariables(
    val paging: Paging,
    val criteria: List<SøkeKriterier>,
)

data class SøkeKriterier(
    val fieldName: String,
    val searchRule: SearchRule,
    val searchHistorical: Boolean = false,
)

data class Paging(
    val pageNumber: Int,
    val resultsPerPage: Int,
)

sealed interface SearchRule

data class SearchRuleEquals(
    val equals: String,
) : SearchRule

data class SearchRuleExists(
    val exists: Boolean,
) : SearchRule
