package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaTilsynBarnDto
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaAktivtet
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaHovedytelse
import no.nav.tilleggsstonader.sak.behandling.fakta.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.VilkårFaktaBarn
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import java.time.LocalDate

object VilkårGrunnlagUtil {
    fun mockVilkårGrunnlagDto(
        barn: List<FaktaBarn> = emptyList(),
    ) =
        BehandlingFaktaTilsynBarnDto(
            hovedytelse = FaktaHovedytelse(
                søknadsgrunnlag = null,
            ),
            aktivitet = FaktaAktivtet(
                søknadsgrunnlag = null,
            ),
            barn = barn,
            dokumentasjon = null,
            søknadMottattTidspunkt = null,
            arena = null,
        )

    fun grunnlagBarn(
        ident: String = "123",
        barnId: BarnId = BarnId.random(),
        registergrunnlag: RegistergrunnlagBarn =
            RegistergrunnlagBarn(
                navn = "navn",
                fødselsdato = LocalDate.of(2024, 6, 4),
                alder = null,
                dødsdato = null,
            ),
        søknadgrunnlag: SøknadsgrunnlagBarn? = null,
    ) = FaktaBarn(
        ident = ident,
        barnId = barnId,
        registergrunnlag = registergrunnlag,
        søknadgrunnlag = søknadgrunnlag,
        vilkårFakta = VilkårFaktaBarn(
            harFullførtFjerdetrinn = null,
        ),
    )
}
