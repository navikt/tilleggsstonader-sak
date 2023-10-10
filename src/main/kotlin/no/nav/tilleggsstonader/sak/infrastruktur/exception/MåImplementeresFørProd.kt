package no.nav.tilleggsstonader.sak.infrastruktur.exception

class MåImplementeresFørProd(override val message: String) : RuntimeException()

inline fun måImlementeresFørProdsetting(
    lazyMessage: () -> String,
) {
    val property = System.getProperty("NAIS_CLUSTER_NAME")
    if (property != "dev-gcp") {
        throw MåImplementeresFørProd("NAIS_CLUSTER_NAME=$property message=${lazyMessage()}")
    }
}
