package no.nav.tilleggsstonader.sak.opplysninger.pdl

import org.apache.commons.lang3.StringUtils

object PdlConfig {

    const val PATH_GRAPHQL = "graphql"

    val personBolkKortQuery = graphqlQuery("/pdl/person_kort_bolk.graphql")

    val søkerQuery = graphqlQuery("/pdl/søker.graphql")

    val forelderBarnQuery = graphqlQuery("/pdl/barn.graphql")

    val annenForelderQuery = graphqlQuery("/pdl/andreForeldre.graphql")

    val hentIdentQuery = graphqlQuery("/pdl/hent_ident.graphql")

    val hentIdenterBolkQuery = graphqlQuery("/pdl/hent_ident_bolk.graphql")

    val søkPersonQuery = graphqlQuery("/pdl/søk_person.graphql")

    val hentGeografiskTilknytningQuery = graphqlQuery("/pdl/geografisk_tilknytning.graphql")

    private fun graphqlQuery(path: String) = PdlConfig::class.java.getResource(path)!!
        .readText()
        .graphqlCompatible()

    private fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }
}
