package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagArenaVedtak
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjon
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlag

data class Grunnlag(
    val personopplysninger: FaktaGrunnlagPersonopplysninger,
    val arenaVedtak: FaktaGrunnlagArenaVedtak?,
    val saksinformasjonAndreForeldre: List<GeneriskFaktaGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>>,
)
