package no.nav.tilleggsstonader.sak.vedtak.beregning

import no.nav.tilleggsstonader.sak.vedtak.beregning.dto.BeløpsperioderTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.beregning.dto.InnvilgelseTilsynBarnDto
import org.springframework.stereotype.Service


@Service
class BeregningTilsynBarnService {

    fun beregn(dto: InnvilgelseTilsynBarnDto): List<BeløpsperioderTilsynBarnDto> {
        return emptyList()
    }
}