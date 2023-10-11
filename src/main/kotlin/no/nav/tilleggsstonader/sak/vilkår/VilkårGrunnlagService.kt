package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.infrastruktur.exception.måImlementeresFørProdsetting
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.vilkår.dto.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.SøknadsgrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.dto.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.dto.SøknadsgrunnlagHovedytelse
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
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        return VilkårGrunnlagDto(
            hovedytelse = mapHovedytelse(søknad),
            aktivitet = mapAktivitet(søknad),
            barn = mapBarn(grunnlagsdata, søknad),
        )
    }

    private fun mapAktivitet(søknad: SøknadBarnetilsyn?) =
        GrunnlagAktivitet(
            søknadsgrunnlag = søknad?.let {
                SøknadsgrunnlagAktivitet(
                    utdanning = it.data.aktivitet.utdanning,
                )
            },
        )

    private fun mapHovedytelse(søknad: SøknadBarnetilsyn?) =
        GrunnlagHovedytelse(
            søknadsgrunnlag = søknad?.let {
                SøknadsgrunnlagHovedytelse(
                    hovedytelse = it.data.hovedytelse.hovedytelse,
                )
            },
        )

    private fun mapBarn(grunnlagsdata: GrunnlagsdataMedMetadata, søknad: SøknadBarnetilsyn?): List<GrunnlagBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        return grunnlagsdata.grunnlagsdata.barn.map { barn ->
            GrunnlagBarn(
                ident = barn.ident,
                registergrunnlag = RegistergrunnlagBarn(
                    navn = barn.navn.visningsnavn(),
                    dødsdato = barn.dødsdato,
                ),
                søknadgrunnlag = søknadBarnPåIdent[barn.ident]?.let { søknadBarn ->
                    SøknadsgrunnlagBarn(
                        type = søknadBarn.data.type,
                        startetIFemte = søknadBarn.data.startetIFemte,
                        årsak = søknadBarn.data.årsak,
                    )
                },
            )
        }
    }

    private fun validerFinnesGrunnlagsdataForAlleBarnISøknad(
        grunnlagsdata: GrunnlagsdataMedMetadata,
        søknadBarnPåIdent: Map<String, SøknadBarn>,
    ) {
        val identerIGrunnlagsdata = grunnlagsdata.grunnlagsdata.barn.map { it.ident }.toSet()
        val identerSomManglerGrunnlagsdata = søknadBarnPåIdent.keys.filterNot { identerIGrunnlagsdata.contains(it) }
        if (identerSomManglerGrunnlagsdata.isNotEmpty()) {
            val kommaseparerteIdenter = identerSomManglerGrunnlagsdata.joinToString(",")
            error("Mangler grunnlagsdata for barn i søknad ($kommaseparerteIdenter)")
        }
    }
}
