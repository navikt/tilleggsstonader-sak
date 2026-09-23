package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.libs.utils.fnr.Fødselsnummer
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GeneriskFaktaGrunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjon
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.DokumentasjonBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.AktivitetDagligReiseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.DokumentasjonDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.ReiseAdresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadPassAvBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.UtdanningAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.AktivitetReiseTilSamlingAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Avreiseadresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Reisemåte
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.SamlingPeriode
import no.nav.tilleggsstonader.sak.util.antallÅrSiden
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelUtil.harFullførtFjerdetrinn
import org.springframework.stereotype.Service
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.DekkesUtgiftenAvAndre as DekkesUtgiftenAvAndreKontrakt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reise as ReiseDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon as SøknadDokumentasjon

/**
 * Denne klassen håndterer henting av VilkårGrunnlagDto
 */
@Service
class BehandlingFaktaService(
    private val søknadService: SøknadService,
    private val barnService: BarnService,
    private val faktaArbeidOgOppholdMapper: FaktaArbeidOgOppholdMapper,
    private val fagsakService: FagsakService,
    private val faktaGrunnlagService: FaktaGrunnlagService,
) {
    fun hentFakta(behandlingId: BehandlingId): BehandlingFaktaDto {
        val stønadstype = fagsakService.hentFagsakForBehandling(behandlingId).stønadstype
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> hentFaktaDtoForPassAvBarn(behandlingId)
            Stønadstype.LÆREMIDLER -> hentFaktaDtoForLæremidler(behandlingId)
            Stønadstype.BOUTGIFTER -> hentFaktaDtoForBoutgifter(behandlingId)
            Stønadstype.DAGLIG_REISE_TSO -> hentFaktaDtoForDagligReise(behandlingId)
            Stønadstype.DAGLIG_REISE_TSR -> hentFaktaDtoForDagligReise(behandlingId)
            Stønadstype.REISE_TIL_SAMLING_TSO,
            Stønadstype.REISE_TIL_SAMLING_TSR,
            -> hentFaktaDtoForReiseTilSamling(behandlingId)

            Stønadstype.FLYTTING_TSO,
            Stønadstype.FLYTTING_TSR,
            -> error("Henting av fakta for $stønadstype er ikke implementert")

            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            -> hentFaktaDtoForReiseOppstartAvslutningHjemreise(behandlingId)
        }
    }

    fun hentFaktaDtoForPassAvBarn(behandlingId: BehandlingId): BehandlingFaktaPassAvBarnDto {
        val søknad = søknadService.hentSøknadPassAvBarn(behandlingId)
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaPassAvBarnDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            aktivitet = mapAktivitet(søknad?.data?.aktivitet),
            barn = mapBarn(grunnlagsdata, søknad, behandlingId),
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId, grunnlagsdata) },
            arena = arenaFakta(grunnlagsdata),
        )
    }

    fun hentFaktaDtoForLæremidler(behandlingId: BehandlingId): BehandlingFaktaLæremidlerDto {
        val søknad = søknadService.hentSøknadLæremidler(behandlingId)
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        val fødselsdato = grunnlagsdata.personopplysninger.fødsel?.fødselsdatoEller1JanForFødselsår()
        return BehandlingFaktaLæremidlerDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId, grunnlagsdata) },
            arena = arenaFakta(grunnlagsdata),
            utdanning = søknad?.data?.utdanning.let { mapUtdanning(it) },
            alder = antallÅrSiden(fødselsdato),
        )
    }

    private fun hentFaktaDtoForBoutgifter(behandlingId: BehandlingId): BehandlingFaktaBoutgifterDto {
        val søknad = søknadService.hentSøknadBoutgifter(behandlingId)
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaBoutgifterDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            aktiviteter = mapAktivitet(søknad?.data?.aktivitet),
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId) },
            arena = arenaFakta(grunnlagsdata),
            boligEllerOvernatting =
                FaktaBoligEllerOvernatting(
                    søknadsgrunnlag = søknad?.data?.boutgifter?.tilFakta(),
                ),
            personopplysninger = mapPersonopplysninger(søknad?.data?.personopplysninger),
        )
    }

    private fun hentFaktaDtoForDagligReise(behandlingId: BehandlingId): BehandlingFaktaDagligReiseDto {
        val søknad = søknadService.hentSøknadDagligReise(behandlingId)
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaDagligReiseDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            dokumentasjon = søknad?.let { mapDokumentasjonDagligReise(it.data.dokumentasjon, it.journalpostId) },
            arena = arenaFakta(grunnlagsdata),
            aktiviteter = mapAktivitetForDagligReise(søknad?.data?.aktivitet),
            reiser = mapReise(søknad?.data?.reiser),
            personopplysninger = mapPersonopplysninger(søknad?.data?.personopplysninger),
        )
    }

    private fun hentFaktaDtoForReiseTilSamling(behandlingId: BehandlingId): BehandlingFaktaReiseTilSamlingDto {
        val søknad = søknadService.hentSøknadReiseTilSamling(behandlingId)
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaReiseTilSamlingDto(
            søknadMottattTidspunkt = søknad?.mottattTidspunkt,
            aktiviteter = mapAktivitetForReiseTilSamling(søknad?.data?.aktivitet),
            hovedytelse = søknad?.data?.hovedytelse.let { mapHovedytelse(it) },
            dokumentasjon = søknad?.let { mapDokumentasjon(it.data.dokumentasjon, it.journalpostId, grunnlagsdata) },
            arena = arenaFakta(grunnlagsdata),
            samlinger = mapSamlinger(søknad?.data?.samlinger),
            avreiseadresse = mapAvreiseadresse(søknad?.data?.avreiseadresse),
            reisemåte = mapReisemåte(søknad?.data?.reisemåte),
        )
    }

    private fun hentFaktaDtoForReiseOppstartAvslutningHjemreise(
        behandlingId: BehandlingId,
    ): BehandlingFaktaReiseOppstartAvslutningHjemreiseDto {
        val grunnlagsdata = faktaGrunnlagService.hentGrunnlagsdata(behandlingId)
        return BehandlingFaktaReiseOppstartAvslutningHjemreiseDto(
            arena = arenaFakta(grunnlagsdata),
        )
    }

    private fun arenaFakta(grunnlagsdata: Grunnlag): ArenaFakta? =
        grunnlagsdata.arenaVedtak?.let {
            ArenaFakta(
                vedtakTom = it.vedtakTom,
            )
        }

    private fun mapAktivitet(aktivitet: AktivitetAvsnitt?) =
        FaktaAktivitet(
            søknadsgrunnlag =
                aktivitet?.let {
                    SøknadsgrunnlagAktivitet(
                        aktiviteter =
                            it.aktiviteter
                                ?.map { it.label },
                        annenAktivitet = it.annenAktivitet,
                        lønnetAktivitet = it.lønnetAktivitet,
                        dekkesUtgiftenAvAndre = null,
                    )
                },
        )

    private fun mapAktivitetForReiseTilSamling(aktivitet: AktivitetReiseTilSamlingAvsnitt?) =
        FaktaAktivitetReiseTilSamling(
            aktivitet =
                FaktaAktivitet(
                    søknadsgrunnlag =
                        aktivitet?.let { avsnitt ->
                            SøknadsgrunnlagAktivitet(
                                aktiviteter = avsnitt.aktiviteter?.map { it.label },
                                annenAktivitet = avsnitt.annenAktivitet,
                                lønnetAktivitet = avsnitt.lønnetAktivitet,
                                dekkesUtgiftenAvAndre = null,
                            )
                        },
                ),
        )

    private fun mapAktivitetForDagligReise(aktivitet: AktivitetDagligReiseAvsnitt?) =
        FaktaAktivitetDagligReise(
            aktivitet =
                FaktaAktivitet(
                    søknadsgrunnlag =
                        aktivitet?.let {
                            SøknadsgrunnlagAktivitet(
                                aktiviteter =
                                    it.aktiviteter
                                        ?.map { it.label },
                                annenAktivitet = it.annenAktivitet,
                                lønnetAktivitet = null,
                                dekkesUtgiftenAvAndre = mapDekkesUtgiftenAvAndre(it.dekkesUtgiftenAvAndre),
                            )
                        },
                ),
        )

    private fun mapDekkesUtgiftenAvAndre(dekkesUtgiftenAvAndre: DekkesUtgiftenAvAndreKontrakt?): DekkesUtgiftenAvAndre? {
        if (dekkesUtgiftenAvAndre === null) {
            return null
        }

        return DekkesUtgiftenAvAndre(
            typeUtdanning = dekkesUtgiftenAvAndre.typeUtdanning,
            lærling = dekkesUtgiftenAvAndre.lærling,
            arbeidsgiverDekkerUtgift = dekkesUtgiftenAvAndre.arbeidsgiverDekkerUtgift,
            erUnder25år = dekkesUtgiftenAvAndre.erUnder25år,
            betalerForReisenTilSkolenSelv = dekkesUtgiftenAvAndre.betalerForReisenTilSkolenSelv,
            lønnetAktivitet = dekkesUtgiftenAvAndre.lønnetAktivitet,
        )
    }

    private fun mapReise(reiser: List<ReiseDagligReise>?): List<FaktaReise>? =
        reiser?.map { reise ->
            FaktaReise(
                skalReiseFraFolkeregistrertAdresse = reise.skalReiseFraFolkeregistrertAdresse,
                adresseDetSkalReisesFra = reise.adresseDetSkalReisesFra,
                reiseAdresse = reise.adresse,
                periode = reise.periode,
                dagerPerUke = reise.dagerPerUke,
                harMerEnn6KmReisevei = reise.harMerEnn6KmReisevei,
                lengdeReisevei = reise.lengdeReisevei,
                harBehovForTransportUavhengigAvReisensLengde = reise.harBehovForTransportUavhengigAvReisensLengde,
                leveringOgHentingIBarnehage = reise.leveringOgHentingIBarnehage,
                kanReiseMedOffentligTransport = reise.kanReiseMedOffentligTransport,
                offentligTransport = reise.offentligTransport,
                privatTransport = reise.privatTransport,
            )
        }

    private fun mapSamlinger(samlinger: List<SamlingPeriode>?): List<FaktaSamling> =
        samlinger?.map {
            FaktaSamling(
                fom = it.fom,
                tom = it.tom,
                erObligatorisk = it.erObligatorisk,
                harBruktEkstraReiseDager = it.harBruktEkstraReiseDager,
                adresse =
                    it.adresse.let { adresse ->
                        ReiseAdresse(
                            gateadresse = adresse.adresse,
                            postnummer = adresse.postnummer,
                            poststed = adresse.poststed,
                        )
                    },
                antallKilometerEnVei = it.antallKilometerEnVei,
            )
        } ?: emptyList()

    private fun mapAvreiseadresse(avreiseadresse: Avreiseadresse?): FaktaAvreiseadresse? =
        avreiseadresse?.let {
            FaktaAvreiseadresse(
                skalReiseFraFolkeregistrertAdresse = it.skalReiseFraFolkeregistrertAdresse,
                adresseDetSkalReisesFra =
                    it.adresseDetSkalReisesFra?.let { adresse ->
                        ReiseAdresse(
                            gateadresse = adresse.adresse,
                            postnummer = adresse.postnummer,
                            poststed = adresse.poststed,
                        )
                    },
            )
        }

    private fun mapReisemåte(reisemåte: Reisemåte?): FaktaReisemåte? =
        reisemåte?.let {
            FaktaReisemåte(
                kanReiseMedOffentligTransport = it.kanReiseMedOffentligTransport,
                kanIkkeReiseMedOffentligTransportBegrunnelser = it.kanIkkeReiseMedOffentligTransportBegrunnelser,
                totalUtgifterOffentligTransport = it.totalUtgifterOffentligTransport,
                kanBenytteEgenBil = it.kanBenytteEgenBil,
                ønskerDekketUtgifterForDrosje = it.ønskerDekketUtgifterForDrosje,
                barnehageGateadresse = it.barnehageGateadresse,
                barnehagePostnummer = it.barnehagePostnummer,
                kanIkkeBenytteEgenBilBegrunnelser = it.kanIkkeBenytteEgenBilBegrunnelser,
                betalerForReiseSelv = it.betalerForReiseSelv,
                harTTKort = it.harTTKort,
                reiseMedBilUtgifter = it.reiseMedBilUtgifter,
            )
        }

    private fun mapHovedytelse(hovedytelseAvsnitt: HovedytelseAvsnitt?) =
        FaktaHovedytelse(
            søknadsgrunnlag =
                hovedytelseAvsnitt?.let {
                    SøknadsgrunnlagHovedytelse(
                        hovedytelse = it.hovedytelse,
                        arbeidOgOpphold = faktaArbeidOgOppholdMapper.mapArbeidOgOpphold(hovedytelseAvsnitt.arbeidOgOpphold),
                        harNedsattArbeidsevne = it.harNedsattArbeidsevne,
                    )
                },
        )

    private fun mapUtdanning(utdanningAvsnitt: UtdanningAvsnitt?) =
        FaktaUtdanning(
            søknadsgrunnlag =
                utdanningAvsnitt?.let { avsnitt ->
                    SøknadsgrunnlagUtdanning(
                        aktiviteter = avsnitt.aktiviteter?.map { it.label },
                        annenUtdanning = avsnitt.annenUtdanning,
                        harRettTilUtstyrsstipend =
                            avsnitt.harRettTilUtstyrsstipend?.let {
                                HarRettTilUtstyrsstipendDto(
                                    erLærlingEllerLiknende = it.erLærlingEllerLiknende,
                                    harTidligereFullførtVgs = it.harTidligereFullførtVgs,
                                    tarOpplæringVgsSamtidig = it.tarOpplæringVgsSamtidig,
                                )
                            },
                        harFunksjonsnedsettelse = avsnitt.harFunksjonsnedsettelse,
                    )
                },
        )

    private fun mapBarn(
        grunnlagsdata: Grunnlag,
        søknad: SøknadPassAvBarn?,
        behandlingId: BehandlingId,
    ): List<FaktaBarn> {
        val søknadBarnPåIdent = søknad?.barn?.associateBy { it.ident } ?: emptyMap()
        if (søknad != null) {
            validerFinnesGrunnlagsdataForAlleBarnISøknad(grunnlagsdata, søknadBarnPåIdent)
        }
        val grunnlagsdataBarn = grunnlagsdata.personopplysninger.barn.associateBy { it.ident }
        val faktaGrunnlagPerBarn = grunnlagsdata.saksinformasjonAndreForeldre.associateBy { it.data.identBarn }

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
                        saksinformasjonAndreForeldre =
                            mapSaksinformasjonAndreForeldre(
                                behandlingBarn,
                                faktaGrunnlagPerBarn,
                            ),
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
                    vedtaksperioderBarn =
                        fakta.data.andreForeldre
                            .flatMap { it.vedtaksperioderBarn }
                            .sorted(),
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
        dokumentasjonListe: List<SøknadDokumentasjon>,
        journalpostId: String,
        grunnlagsdata: Grunnlag,
    ): FaktaDokumentasjon {
        val navn = grunnlagsdata.personopplysninger.barn.associate { it.ident to it.navn.fornavn }
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

    private fun mapDokumentasjon(
        dokumentasjonListe: List<DokumentasjonBoutgifter>,
        journalpostId: String,
    ): FaktaDokumentasjon {
        val dokumentasjon =
            dokumentasjonListe.map {
                Dokumentasjon(type = it.tittel, dokumenter = listOf(Dokument(it.dokumentInfoId)))
            }
        return FaktaDokumentasjon(journalpostId, dokumentasjon)
    }

    private fun mapDokumentasjonDagligReise(
        dokumentasjonListe: List<DokumentasjonDagligReise>,
        journalpostId: String,
    ): FaktaDokumentasjon {
        val dokumentasjon =
            dokumentasjonListe.map {
                Dokumentasjon(type = it.tittel, dokumenter = listOf(Dokument(it.dokumentInfoId)))
            }
        return FaktaDokumentasjon(journalpostId, dokumentasjon)
    }

    private fun validerFinnesGrunnlagsdataForAlleBarnISøknad(
        grunnlagsdata: Grunnlag,
        søknadBarnPåIdent: Map<String, SøknadBarn>,
    ) {
        val identerIGrunnlagsdata =
            grunnlagsdata.personopplysninger.barn
                .map { it.ident }
                .toSet()
        val identerSomManglerGrunnlagsdata = søknadBarnPåIdent.keys.filterNot { identerIGrunnlagsdata.contains(it) }
        if (identerSomManglerGrunnlagsdata.isNotEmpty()) {
            val kommaseparerteIdenter = identerSomManglerGrunnlagsdata.joinToString(",")
            error("Mangler grunnlagsdata for barn i søknad ($kommaseparerteIdenter)")
        }
    }
}
