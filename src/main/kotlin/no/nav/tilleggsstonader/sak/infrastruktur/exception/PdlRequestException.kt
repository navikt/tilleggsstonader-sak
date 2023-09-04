package no.nav.tilleggsstonader.sak.infrastruktur.exception

open class PdlRequestException(melding: String? = null) : Exception(melding)

class PdlNotFoundException : PdlRequestException()
