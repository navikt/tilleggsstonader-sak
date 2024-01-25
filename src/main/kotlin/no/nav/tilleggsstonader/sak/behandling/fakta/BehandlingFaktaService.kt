package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.måImlementeresFørProdsetting
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class BehandlingFaktaService(
    private val grunnlagsdataService: GrunnlagsdataService,
    private val søknadService: SøknadService,
    private val barnService: BarnService,
) {

    fun hentGrunnlag(
        behandlingId: UUID,
    ): BehandlingFaktaDto {
        måImlementeresFørProdsetting {
            "Denne skal hente data fra databasen, og at grunnlagsdata lagres til databasen"
        }
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentFraRegister(behandlingId)
        return BehandlingFaktaDto(
            hovedytelse = mapHovedytelse(søknad),
            aktivitet = mapAktivitet(søknad),
            barn = mapBarn(grunnlagsdata, søknad, behandlingId),
        )
    }

    private fun mapAktivitet(søknad: SøknadBarnetilsyn?) =
        FaktaAktivtet(
            søknadsgrunnlag = søknad?.let {
                SøknadsgrunnlagAktivitet(
                    utdanning = it.data.aktivitet.utdanning,
                )
            },
        )

    private fun mapHovedytelse(søknad: SøknadBarnetilsyn?) =
        FaktaHovedytelse(
            søknadsgrunnlag = søknad?.let {
                SøknadsgrunnlagHovedytelse(
                    hovedytelse = it.data.hovedytelse.hovedytelse,
                )
            },
        )

    private fun mapBarn(grunnlagsdata: GrunnlagsdataMedMetadata, søknad: SøknadBarnetilsyn?, behandlingId: UUID): List<FaktaBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val barnPåBehandling = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.ident }

        return grunnlagsdata.grunnlagsdata.barn.map { barn ->
            val behandlingBarn = barnPåBehandling[barn.ident]
                ?: error("Finner ikke barn med ident=${barn.ident} på behandling=$behandlingId")

            FaktaBarn(
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
