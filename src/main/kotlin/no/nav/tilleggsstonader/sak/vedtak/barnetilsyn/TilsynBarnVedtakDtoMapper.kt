package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.felles.VedtakTilsynBarnDomain
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakDtoMapper() : VedtakDtoMapper<VedtakTilsynBarnDomain, VedtakTilsynBarnDto>{
    override fun map(dto: VedtakTilsynBarnDto): VedtakTilsynBarnDomain {
        TODO("Not yet implemented")
    }

    override fun map(domene: VedtakTilsynBarnDomain): VedtakTilsynBarnDto {
        TODO("Not yet implemented")
    }
}