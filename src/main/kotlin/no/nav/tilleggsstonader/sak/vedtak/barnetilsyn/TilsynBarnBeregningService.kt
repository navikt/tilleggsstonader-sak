package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import org.springframework.stereotype.Service

@Service
class TilsynBarnBeregningService {

    fun beregn(dto: InnvilgelseTilsynBarnDto): BeregningsresultatTilsynBarnDto {
        return BeregningsresultatTilsynBarnDto(emptyList())
    }
}
