package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.felles.VedtakTilsynBarn
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakMapper : VedtakDtoMapper<VedtakTilsynBarn, VedtakTilsynBarnDto>{
    override fun map(dto: VedtakTilsynBarnDto): VedtakTilsynBarn {
        TODO("Not yet implemented")
    }

    override fun map(domene: VedtakTilsynBarn): VedtakTilsynBarnDto {
        TODO("Not yet implemented")
    }
}