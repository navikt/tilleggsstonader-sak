package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.måImlementeresFørProdsetting
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.GrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.RegistergrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SøknadsgrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SøknadsgrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SøknadsgrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårGrunnlagDto
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class VilkårGrunnlagService(
    private val grunnlagsdataService: GrunnlagsdataService,
    private val søknadService: SøknadService,
    private val barnService: BarnService,
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
            barn = mapBarn(grunnlagsdata, søknad, behandlingId),
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

    private fun mapBarn(grunnlagsdata: GrunnlagsdataMedMetadata, søknad: SøknadBarnetilsyn?, behandlingId: UUID): List<GrunnlagBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val barnPåBehandling = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.ident }

        return grunnlagsdata.grunnlagsdata.barn.map { barn ->
            val behandlingBarn = barnPåBehandling[barn.ident]
                ?: error("Finner ikke barn med ident=${barn.ident} på behandling=$behandlingId")

            GrunnlagBarn(
                ident = barn.ident,
                barnId = behandlingBarn.id,
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
