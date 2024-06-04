package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import java.time.LocalDate

object GrunnlagArenaMapper {

    fun mapFaktaArena(status: ArenaStatusDto): GrunnlagArena = with(status.vedtak) {
        GrunnlagArena(
            vedtakTom = vedtakTom?.takeIf { it.isEqualOrAfter(LocalDate.now().minusMonths(3)) },
        )
    }
}
