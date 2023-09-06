package no.nav.tilleggsstonader.sak.opplysninger.pdl

open class PdlRequestException(melding: String? = null) : Exception(melding)

class PdlNotFoundException : PdlRequestException()
