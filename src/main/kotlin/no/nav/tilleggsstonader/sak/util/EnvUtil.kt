package no.nav.tilleggsstonader.sak.util

object EnvUtil {

    fun erIDev() = System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp"
    fun erIProd() = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"
}
