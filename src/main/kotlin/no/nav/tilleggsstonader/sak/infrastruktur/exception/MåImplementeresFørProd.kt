package no.nav.tilleggsstonader.sak.infrastruktur.exception

import org.slf4j.LoggerFactory

class MåImplementeresFørProd(
    override val message: String,
) : RuntimeException()

inline fun måImlementeresFørProdsetting(lazyMessage: () -> String) {
    val property = System.getenv("NAIS_CLUSTER_NAME")
    if (property == null) {
        LoggerFactory
            .getLogger(MåImplementeresFørProd::class.java)
            .error("Har ikke satt property for NAIS_CLUSTER_NAME")
    }
    if (property == "prod-gcp") {
        throw MåImplementeresFørProd("NAIS_CLUSTER_NAME=$property message=${lazyMessage()}")
    }
}
