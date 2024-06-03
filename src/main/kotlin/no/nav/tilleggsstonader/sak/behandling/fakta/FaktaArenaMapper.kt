package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import java.time.LocalDate

object FaktaArenaMapper {

    fun mapFaktaArena(status: ArenaStatusDto): ArenaFakta = with(status.vedtak) {
        ArenaFakta(
            finnesVedtak = harVedtak || harVedtakUtenUtfall,
            vedtakTom = vedtakTom?.takeIf { it.isEqualOrAfter(LocalDate.now().minusMonths(3)) },
        )
    }
}
