package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "eksterne-applikasjoner")
data class EksterneApplikasjonerProperties(
    val soknadApi: String,
    val arena: String,
    val bidragGrunnlag: String,
    val bidragGrunnlagFeature: String,
)

enum class EksternApplikasjon {
    SOKNAD_API,
    ARENA,
    BIDRAG_GRUNNLAG,
    BIDRAG_GRUNNLAG_FEATURE,
}

@Configuration
@EnableConfigurationProperties(EksterneApplikasjonerProperties::class)
class EksternApplikasjonConfig(
    private val properties: EksterneApplikasjonerProperties
) {
    companion object {
        lateinit var namespaceAppNavn: Map<EksternApplikasjon, String>
    }

    @PostConstruct
    fun init() {
        namespaceAppNavn = mapOf(
            EksternApplikasjon.SOKNAD_API to properties.soknadApi,
            EksternApplikasjon.ARENA to properties.arena,
            EksternApplikasjon.BIDRAG_GRUNNLAG to properties.bidragGrunnlag,
            EksternApplikasjon.BIDRAG_GRUNNLAG_FEATURE to properties.bidragGrunnlagFeature,
        )
    }
}


/*enum class EksternApplikasjon(
    val namespaceAppNavn: String,
) {
    SOKNAD_API("gcp:tilleggsstonader:tilleggsstonader-soknad-api"),

    // SOKNAD_API_LOKAL("gcp:tilleggsstonader:tilleggsstonader-soknad-api-lokal"),
    ARENA("fss:teamarenanais:arena"),

    BIDRAG_GRUNNLAG("gcp:bidrag:bidrag-grunnlag"),
    BIDRAG_GRUNNLAG_FEATURE("gcp:bidrag:bidrag-grunnlag-feature"),
}*/
