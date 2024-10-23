package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.util.isEqualOrAfter

object GrunnlagArenaMapper {

    fun mapFaktaArena(status: ArenaStatusDto, behandling: Saksbehandling): GrunnlagArena = with(status.vedtak) {
        GrunnlagArena(
            vedtakTom = vedtakTom?.takeIf {
                it.isEqualOrAfter(behandling.opprettetTid.toLocalDate().minusYears(1))
            },
        )
    }
}
