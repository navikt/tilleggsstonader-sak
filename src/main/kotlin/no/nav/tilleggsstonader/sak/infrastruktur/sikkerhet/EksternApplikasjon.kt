package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

enum class EksternApplikasjon(
    val namespaceAppNavn: String,
) {
    SOKNAD_API("gcp:tilleggsstonader:tilleggsstonader-soknad-api-lokal"),

    // SOKNAD_API_LOKAL("gcp:tilleggsstonader:tilleggsstonader-soknad-api-lokal"),
    ARENA("fss:teamarenanais:arena"),

    BIDRAG_GRUNNLAG("gcp:bidrag:bidrag-grunnlag"),
    BIDRAG_GRUNNLAG_FEATURE("gcp:bidrag:bidrag-grunnlag-feature"),
}
