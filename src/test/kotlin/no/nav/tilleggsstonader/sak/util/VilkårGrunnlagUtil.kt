package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaAktivtet
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaHovedytelse
import no.nav.tilleggsstonader.sak.behandling.fakta.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.SøknadsgrunnlagBarn
import java.util.UUID

object VilkårGrunnlagUtil {
    fun mockVilkårGrunnlagDto(
        barn: List<FaktaBarn> = emptyList(),
    ) =
        BehandlingFaktaDto(
            hovedytelse = FaktaHovedytelse(
                søknadsgrunnlag = null,
            ),
            aktivitet = FaktaAktivtet(
                søknadsgrunnlag = null,
            ),
            barn = barn,
        )

    fun grunnlagBarn(
        ident: String = "123",
        barnId: UUID = UUID.randomUUID(),
        registergrunnlag: RegistergrunnlagBarn = RegistergrunnlagBarn("navn", null),
        søknadgrunnlag: SøknadsgrunnlagBarn? = null,
    ) = FaktaBarn(
        ident = ident,
        barnId = barnId,
        registergrunnlag = registergrunnlag,
        søknadgrunnlag = søknadgrunnlag,
    )
}
