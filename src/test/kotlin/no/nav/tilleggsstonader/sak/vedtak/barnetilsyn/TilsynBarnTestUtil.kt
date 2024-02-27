package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import java.util.UUID

object TilsynBarnTestUtil {

    fun innvilgelseDto(
        utgifter: Map<UUID, List<Utgift>> = mapOf(),
    ) = InnvilgelseTilsynBarnDto(
        utgifter = utgifter,
    )

    fun barn(barnId: UUID, vararg utgifter: Utgift) = Pair(barnId, utgifter.toList())
}
