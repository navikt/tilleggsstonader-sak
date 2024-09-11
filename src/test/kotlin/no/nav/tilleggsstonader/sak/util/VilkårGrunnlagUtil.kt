package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaAktivtet
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.FaktaHovedytelse
import no.nav.tilleggsstonader.sak.behandling.fakta.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.behandling.fakta.VilkårFaktaBarn
import java.time.LocalDate
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
            dokumentasjon = null,
            søknadMottattTidspunkt = null,
            arena = null,
        )

    fun grunnlagBarn(
        ident: String = "123",
        barnId: UUID = UUID.randomUUID(),
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
