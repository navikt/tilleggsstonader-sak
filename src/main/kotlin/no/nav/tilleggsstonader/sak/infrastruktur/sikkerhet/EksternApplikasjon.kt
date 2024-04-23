package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

enum class EksternApplikasjon(val namespaceAppNavn: String) {
    SOKNAD_API("gcp:tilleggsstonader:tilleggsstonader-soknad-api"),
    ARENA("fss:teamarenanais:arena"),
}
