package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "eksterne-applikasjoner")
data class EksternApplikasjon(
    val soknadApi: String,
    val arena: String,
    val bidragGrunnlag: String,
    val bidragGrunnlagFeature: String,
)
