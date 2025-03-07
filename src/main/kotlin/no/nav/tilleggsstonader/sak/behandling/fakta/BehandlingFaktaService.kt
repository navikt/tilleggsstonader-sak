package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.libs.utils.fnr.Fødselsnummer
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjon
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.UtdanningAvsnitt
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelUtil.harFullførtFjerdetrinn
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class BehandlingFaktaService(
    private val grunnlagsdataService: GrunnlagsdataService,
    private val søknadService: SøknadService,
    private val barnService: BarnService,
    private val faktaArbeidOgOppholdMapper: FaktaArbeidOgOppholdMapper,
    private val fagsakService: FagsakService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
) {
    fun hentFakta(behandlingId: BehandlingId): BehandlingFaktaDto {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> hentFaktaDTOForBarneTilsyn(behandlingId)
            Stønadstype.LÆREMIDLER -> hentFaktaDTOForLæremidler(behandlingId)
            Stønadstype.BOUTGIFTER -> hentFaktaDTOForBoutgifter(behandlingId)
        }
    }

    fun hentFaktaDTOForBarneTilsyn(behandlingId: BehandlingId): BehandlingFaktaTilsynBarnDto {
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val faktaGrunnlagAnnenForelder =
            faktaGrunnlagService.hentGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>(behandlingId)
        return BehandlingFaktaTilsynBarnDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            aktivitet = mapAktivitet(søknad),
            barn = mapBarn(grunnlagsdata, søknad, behandlingId, faktaGrunnlagAnnenForelder),
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId, grunnlagsdata) },
            arena = arenaFakta(grunnlagsdata),
        )
    }

    fun hentFaktaDTOForLæremidler(behandlingId: BehandlingId): BehandlingFaktaLæremidlerDto {
        val søknad = søknadService.hentSøknadLæremidler(behandlingId)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val fødselsdato = grunnlagsdata.grunnlag.fødsel?.fødselsdatoEller1JanForFødselsår()
        return BehandlingFaktaLæremidlerDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId, grunnlagsdata) },
            arena = arenaFakta(grunnlagsdata),
            utdanning = søknad?.data?.utdanning.let { mapUtdanning(it) },
            alder = antallÅrSiden(fødselsdato),
        )
    }

    private fun hentFaktaDTOForBoutgifter(behandlingId: BehandlingId): BehandlingFaktaBoutgifterDto {
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaBoutgifterDto(
            arena = arenaFakta(grunnlagsdata),
        )
    }

    private fun arenaFakta(grunnlagsdata: Grunnlagsdata): ArenaFakta? =
        grunnlagsdata.grunnlag.arena?.let {
            ArenaFakta(
                vedtakTom = it.vedtakTom,
            )
        }

    private fun mapAktivitet(søknad: SøknadBarnetilsyn?) =
        FaktaAktivtet(
            søknadsgrunnlag =
                søknad?.let {
                    SøknadsgrunnlagAktivitet(
                        aktiviteter =
                            it.data.aktivitet.aktiviteter
                                ?.map { it.label },
                        annenAktivitet = it.data.aktivitet.annenAktivitet,
                        lønnetAktivitet = it.data.aktivitet.lønnetAktivitet,
                    )
                },
        )

    private fun mapHovedytelse(hovedytelseAvsnitt: HovedytelseAvsnitt?) =
        FaktaHovedytelse(
            søknadsgrunnlag =
                hovedytelseAvsnitt?.let {
                    SøknadsgrunnlagHovedytelse(
                        hovedytelse = it.hovedytelse,
                        arbeidOgOpphold = faktaArbeidOgOppholdMapper.mapArbeidOgOpphold(hovedytelseAvsnitt.arbeidOgOpphold),
                    )
                },
        )

    private fun mapUtdanning(utdanningAvsnitt: UtdanningAvsnitt?) =
        FaktaUtdanning(
            søknadsgrunnlag =
                utdanningAvsnitt?.let {
                    SøknadsgrunnlagUtdanning(
                        aktiviteter = it.aktiviteter?.map { it.label },
                        annenUtdanning = it.annenUtdanning,
                        harRettTilUtstyrsstipend =
                            it.harRettTilUtstyrsstipend?.let {
                                HarRettTilUtstyrsstipendDto(
                                    erLærlingEllerLiknende = it.erLærlingEllerLiknende,
                                    harTidligereFullførtVgs = it.harTidligereFullførtVgs,
                                )
                            },
                        harFunksjonsnedsettelse = it.harFunksjonsnedsettelse,
                    )
                },
        )

    private fun mapBarn(
        grunnlagsdata: Grunnlagsdata,
        søknad: SøknadBarnetilsyn?,
        behandlingId: BehandlingId,
        faktaGrunnlagAnnenForelder: List<GeneriskFaktaGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>>,
    ): List<FaktaBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val grunnlagsdataBarn = grunnlagsdata.grunnlag.barn.associateBy { it.ident }
        val faktaGrunnlagPerBarn = faktaGrunnlagAnnenForelder.associateBy { it.data.identBarn }

        return barnService.finnBarnPåBehandling(behandlingId).map { behandlingBarn ->
            val barnGrunnlagsdata =
                grunnlagsdataBarn[behandlingBarn.ident]
                    ?: error("Finner ikke barn med ident=${behandlingBarn.ident} på behandling=$behandlingId")

            val søknadgrunnlag =
                søknadBarnPåIdent[behandlingBarn.ident]?.let { søknadBarn ->
                    SøknadsgrunnlagBarn(
                        type = søknadBarn.data.type,
                        utgifter = søknadBarn.data.utgifter,
                        startetIFemte = søknadBarn.data.startetIFemte,
                        årsak = søknadBarn.data.årsak,
                    )
                }
            FaktaBarn(
                ident = behandlingBarn.ident,
                barnId = behandlingBarn.id,
                registergrunnlag =
                    RegistergrunnlagBarn(
                        navn = barnGrunnlagsdata.navn.visningsnavn(),
                        fødselsdato = barnGrunnlagsdata.fødselsdato,
                        alder = barnGrunnlagsdata.alder,
                        dødsdato = barnGrunnlagsdata.dødsdato,
                        saksinformasjonAndreForeldre = mapSaksinformasjonAndreForeldre(behandlingBarn, faktaGrunnlagPerBarn),
                    ),
                søknadgrunnlag = søknadgrunnlag,
                vilkårFakta =
                    VilkårFaktaBarn(
                        harFullførtFjerdetrinn = utledHarFullførtFjerdetrinn(barnGrunnlagsdata, søknadgrunnlag),
                    ),
            )
        }
    }

    private fun mapSaksinformasjonAndreForeldre(
        behandlingBarn: BehandlingBarn,
        faktaGrunnlagPerBarn: Map<String, GeneriskFaktaGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>>,
    ): SaksinformasjonAndreForeldre? =
        faktaGrunnlagPerBarn[behandlingBarn.ident]
            ?.let { fakta ->
                SaksinformasjonAndreForeldre(
                    hentetTidspunkt = fakta.sporbar.opprettetTid,
                    harBehandlingUnderArbeid = fakta.data.andreForeldre.any { it.harBehandlingUnderArbeid },
                    vedtaksperioderBarn = fakta.data.andreForeldre.flatMap { it.vedtaksperioderBarn }.sorted(),
                )
            }

    private fun utledHarFullførtFjerdetrinn(
        barnGrunnlagsdata: GrunnlagBarn,
        søknadgrunnlag: SøknadsgrunnlagBarn?,
    ): JaNei? {
        val fødselsdato = barnGrunnlagsdata.fødselsdato ?: Fødselsnummer(barnGrunnlagsdata.ident).fødselsdato
        if (søknadgrunnlag?.startetIFemte == JaNei.JA) {
            return null
        }
        if (!harFullførtFjerdetrinn(fødselsdato, LocalDate.now())) {
            return JaNei.NEI
        }
        return null
    }

    private fun mapDokumentasjon(
        dokumentasjonListe: List<no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon>,
        journalpostId: String,
        grunnlagsdata: Grunnlagsdata,
    ): FaktaDokumentasjon {
        val navn = grunnlagsdata.grunnlag.barn.associate { it.ident to it.navn.fornavn }
        val dokumentasjon =
            dokumentasjonListe.map { dokumentasjon ->
                val navnBarn = dokumentasjon.identBarn?.let { navn[it] }?.let { " - $it" } ?: ""
                Dokumentasjon(
                    type = dokumentasjon.type.tittel + navnBarn,
                    dokumenter = dokumentasjon.dokumenter.map { Dokument(it.dokumentInfoId) },
                    identBarn = dokumentasjon.identBarn,
                )
            }
        return FaktaDokumentasjon(journalpostId, dokumentasjon)
    }

    private fun validerFinnesGrunnlagsdataForAlleBarnISøknad(
        grunnlagsdata: Grunnlagsdata,
        søknadBarnPåIdent: Map<String, SøknadBarn>,
    ) {
        val identerIGrunnlagsdata =
            grunnlagsdata.grunnlag.barn
                .map { it.ident }
                .toSet()
        val identerSomManglerGrunnlagsdata = søknadBarnPåIdent.keys.filterNot { identerIGrunnlagsdata.contains(it) }
        if (identerSomManglerGrunnlagsdata.isNotEmpty()) {
            val kommaseparerteIdenter = identerSomManglerGrunnlagsdata.joinToString(",")
            error("Mangler grunnlagsdata for barn i søknad ($kommaseparerteIdenter)")
        }
    }
}
