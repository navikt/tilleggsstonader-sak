package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter
import java.time.LocalDate

data class FaktaGrunnlagArenaVedtak(
    val vedtakTom: LocalDate?,
) : FaktaGrunnlagData {
    override val type: TypeFaktaGrunnlag = TypeFaktaGrunnlag.ARENA_VEDTAK_TOM

    companion object {
        fun map(
            status: ArenaStatusDto,
            stønadstype: Stønadstype,
        ) = with(status.vedtak) {
            val vedtakTom =
                vedtakTom?.takeIf {
                    val datoFraOgMed =
                        LocalDate
                            .now()
                            .minusMonths(stønadstype.grunnlagAntallMånederBakITiden.toLong() + 2)
                    it.isEqualOrAfter(datoFraOgMed)
                }
            FaktaGrunnlagArenaVedtak(vedtakTom = vedtakTom)
        }
    }
}
