package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.infrastruktur.exception.måImlementeresFørProdsetting
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class VilkårGrunnlagService(
    private val grunnlagsdataService: GrunnlagsdataService,
    private val søknadService: SøknadService,
) {

    fun hentGrunnlag(
        behandlingId: UUID,
    ): VilkårGrunnlagDto {
        måImlementeresFørProdsetting {
            "Denne skal hente data fra databasen, og at grunnlagsdata lagres til databasen"
        }
        val søknadBarnetilsyn = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        return VilkårGrunnlagDto(
            barn = grunnlagBarn(grunnlagsdata),
        )
    }

    private fun grunnlagBarn(grunnlagsdata: GrunnlagsdataMedMetadata) =
        grunnlagsdata.grunnlagsdata.barn.map {
            GrunnlagBarn(
                ident = it.ident,
                registergrunnlag = RegistergrunnlagBarn(
                    navn = it.navn.visningsnavn(),
                    dødsdato = it.dødsdato,
                ),
                søknadgrunnlag = SøknadsgrunnlagBarn(true),
            )
        }
}
