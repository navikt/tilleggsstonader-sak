package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårGrunnlagDto
import java.util.UUID

object VilkårGrunnlagUtil {
    fun mockVilkårGrunnlagDto(
        barn: List<GrunnlagBarn> = emptyList(),
    ) =
        VilkårGrunnlagDto(
            hovedytelse = GrunnlagHovedytelse(
                søknadsgrunnlag = null,
            ),
            aktivitet = GrunnlagAktivitet(
                søknadsgrunnlag = null,
            ),
            barn = barn,
        )

    fun grunnlagBarn(
        ident: String = "123",
        barnId: UUID = UUID.randomUUID(),
        registergrunnlag: RegistergrunnlagBarn = RegistergrunnlagBarn("navn", null),
        søknadgrunnlag: SøknadsgrunnlagBarn? = null,
    ) = GrunnlagBarn(
        ident = ident,
        barnId = barnId,
        registergrunnlag = registergrunnlag,
        søknadgrunnlag = søknadgrunnlag,
    )
}
