package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import java.util.UUID

object TilsynBarnTestUtil {

    fun innvilgelseDto(
        stønadsperioder: List<Stønadsperiode> = listOf(),
        utgifter: Map<UUID, List<Utgift>> = mapOf(),
    ) = InnvilgelseTilsynBarnDto(
        stønadsperioder = stønadsperioder,
        utgifter = utgifter,
    )

    fun barn(barnId: UUID, vararg utgifter: Utgift) = Pair(barnId, utgifter.toList())
}
