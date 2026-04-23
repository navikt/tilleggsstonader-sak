package no.nav.tilleggsstonader.sak.brev

sealed interface ResultatDistribusjon {
    data object Distribuert : ResultatDistribusjon

    data class FeiletFordiMottakerErDødOgManglerAdresse(
        val feilmelding: String?,
    ) : ResultatDistribusjon
}
