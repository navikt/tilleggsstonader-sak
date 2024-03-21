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

    fun hentFakta(
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
            dokumentasjon = søknad?.let { mapDokumentasjon(it, grunnlagsdata) },
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
                    boddSammenhengende = it.data.hovedytelse.boddSammenhengende,
                    planleggerBoINorgeNeste12mnd = it.data.hovedytelse.planleggerBoINorgeNeste12mnd,
                )
            },
        )

    private fun mapBarn(
        grunnlagsdata: GrunnlagsdataMedMetadata,
        søknad: SøknadBarnetilsyn?,
        behandlingId: UUID,
    ): List<FaktaBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn.associateBy { it.ident }

        return barnService.finnBarnPåBehandling(behandlingId).map { behandlingBarn ->
            val barnGrunnlagsdata = grunnlagsdataBarn[behandlingBarn.ident]
                ?: error("Finner ikke barn med ident=${behandlingBarn.ident} på behandling=$behandlingId")

            FaktaBarn(
                ident = behandlingBarn.ident,
                barnId = behandlingBarn.id,
                registergrunnlag = RegistergrunnlagBarn(
                    navn = barnGrunnlagsdata.navn.visningsnavn(),
                    alder = barnGrunnlagsdata.alder,
                    dødsdato = barnGrunnlagsdata.dødsdato,
                ),
                søknadgrunnlag = søknadBarnPåIdent[behandlingBarn.ident]?.let { søknadBarn ->
                    SøknadsgrunnlagBarn(
                        type = søknadBarn.data.type,
                        startetIFemte = søknadBarn.data.startetIFemte,
                        årsak = søknadBarn.data.årsak,
                    )
                },
            )
        }
    }

    private fun mapDokumentasjon(søknad: SøknadBarnetilsyn, grunnlagsdata: GrunnlagsdataMedMetadata): FaktaDokumentasjon {
        val navn = grunnlagsdata.grunnlagsdata.barn.associate { it.ident to it.navn.fornavn }
        val dokumentasjon = søknad.data.dokumentasjon.map { dokumentasjon ->
            val navnBarn = dokumentasjon.identBarn?.let { navn[it] }?.let { " - $it" } ?: ""
            Dokumentasjon(
                type = dokumentasjon.type.tittel + navnBarn,
                dokumenter = dokumentasjon.dokumenter.map { Dokument(it.dokumentInfoId) },
                identBarn = dokumentasjon.identBarn,
            )
        }
        return FaktaDokumentasjon(søknad.journalpostId, dokumentasjon)
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
