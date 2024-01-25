package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.behandling.fakta.GrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.GrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.behandling.fakta.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.SøknadsgrunnlagBarn
import java.util.UUID

object VilkårGrunnlagUtil {
    fun mockVilkårGrunnlagDto(
        barn: List<GrunnlagBarn> = emptyList(),
    ) =
        BehandlingFaktaDto(
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
