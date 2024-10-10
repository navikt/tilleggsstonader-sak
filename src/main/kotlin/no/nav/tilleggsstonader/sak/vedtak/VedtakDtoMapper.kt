package no.nav.tilleggsstonader.sak.vedtak

interface VedtakDtoMapper<DOMENE, DTO> {

    fun map(dto: DTO): DOMENE
    fun map(domene: DOMENE): DTO

}