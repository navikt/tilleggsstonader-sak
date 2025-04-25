package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import java.time.LocalDate

data class FaktaGrunnlagArenaVedtak(
    val vedtakTom: LocalDate?,
) : FaktaGrunnlagData {
    override val type: TypeFaktaGrunnlag = TypeFaktaGrunnlag.ARENA_VEDTAK_TOM
}
