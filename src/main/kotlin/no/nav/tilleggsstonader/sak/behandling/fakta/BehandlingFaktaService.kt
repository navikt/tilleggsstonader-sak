package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
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
    private val faktaArbeidOgOppholdMapper: FaktaArbeidOgOppholdMapper,
) {

    fun hentFakta(
        behandlingId: UUID,
    ): BehandlingFaktaDto {
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
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
                    aktiviteter = it.data.aktivitet.aktiviteter?.map { it.label },
                    annenAktivitet = it.data.aktivitet.annenAktivitet,
                    lønnetAktivitet = it.data.aktivitet.lønnetAktivitet,
                )
            },
        )

    private fun mapHovedytelse(søknad: SøknadBarnetilsyn?) =
        FaktaHovedytelse(
            søknadsgrunnlag = søknad?.let {
                SøknadsgrunnlagHovedytelse(
                    hovedytelse = it.data.hovedytelse.hovedytelse,
                    arbeidOgOpphold = faktaArbeidOgOppholdMapper.mapArbeidOgOpphold(it.data.hovedytelse.arbeidOgOpphold),
                )
            },
        )

    private fun mapBarn(
        grunnlagsdata: Grunnlagsdata,
        søknad: SøknadBarnetilsyn?,
        behandlingId: UUID,
    ): List<FaktaBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val grunnlagsdataBarn = grunnlagsdata.grunnlag.barn.associateBy { it.ident }

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

    private fun mapDokumentasjon(søknad: SøknadBarnetilsyn, grunnlagsdata: Grunnlagsdata): FaktaDokumentasjon {
        val navn = grunnlagsdata.grunnlag.barn.associate { it.ident to it.navn.fornavn }
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
        grunnlagsdata: Grunnlagsdata,
        søknadBarnPåIdent: Map<String, SøknadBarn>,
    ) {
        val identerIGrunnlagsdata = grunnlagsdata.grunnlag.barn.map { it.ident }.toSet()
        val identerSomManglerGrunnlagsdata = søknadBarnPåIdent.keys.filterNot { identerIGrunnlagsdata.contains(it) }
        if (identerSomManglerGrunnlagsdata.isNotEmpty()) {
            val kommaseparerteIdenter = identerSomManglerGrunnlagsdata.joinToString(",")
            error("Mangler grunnlagsdata for barn i søknad ($kommaseparerteIdenter)")
        }
    }
}
