package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import java.util.UUID

object TilsynBarnTestUtil {

    fun innvilgelseDto(
        utgifter: Map<UUID, List<Utgift>> = mapOf(),
        beregningsresultat: BeregningsresultatTilsynBarnDto? = null,
    ) = InnvilgelseTilsynBarnDto(
        utgifter = utgifter,
        beregningsresultat = beregningsresultat,
    )

    fun barn(barnId: UUID, vararg utgifter: Utgift) = Pair(barnId, utgifter.toList())
}
