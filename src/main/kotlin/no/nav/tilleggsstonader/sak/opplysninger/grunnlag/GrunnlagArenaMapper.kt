package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import java.time.LocalDate

object GrunnlagArenaMapper {
    fun mapFaktaArena(
        status: ArenaStatusDto,
        stønadstype: Stønadstype,
    ): GrunnlagArena =
        with(status.vedtak) {
            val vedtakTom =
                vedtakTom?.takeIf {
                    val datoFraOgMed =
                        LocalDate
                            .now()
                            .minusMonths(stønadstype.grunnlagAntallMånederBakITiden.toLong())
                            .minusMonths(2)
                    it.isEqualOrAfter(datoFraOgMed)
                }
            GrunnlagArena(vedtakTom = vedtakTom)
        }
}
