package no.nav.tilleggsstonader.sak.behandling.fakta

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.AnnenUtdanningType
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.OffentligTransport
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.PrivatTransport
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.ReiseAdresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reiseperiode
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.TypeUtdanning
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Utgifter
import java.time.LocalDate
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(BehandlingFaktaTilsynBarnDto::class, name = "BARNETILSYN"),
    JsonSubTypes.Type(BehandlingFaktaLæremidlerDto::class, name = "LÆREMIDLER"),
    JsonSubTypes.Type(BehandlingFaktaBoutgifterDto::class, name = "BOUTGIFTER"),
    JsonSubTypes.Type(BehandlingFaktaDagligReiseDto::class, name = "DAGLIG_REISE_TSO"),
    JsonSubTypes.Type(BehandlingFaktaDagligReiseDto::class, name = "DAGLIG_REISE_TSR"),
    JsonSubTypes.Type(BehandlingFaktaReiseTilSamlingDto::class, name = "REISE_TIL_SAMLING_TSO"),
)
sealed interface BehandlingFaktaDto {
    val søknadMottattTidspunkt: LocalDateTime?
    val hovedytelse: FaktaHovedytelse?
    val dokumentasjon: FaktaDokumentasjon?
    val arena: ArenaFakta?
}

data class BehandlingFaktaTilsynBarnDto(
    override val søknadMottattTidspunkt: LocalDateTime?,
    override val hovedytelse: FaktaHovedytelse,
    override val dokumentasjon: FaktaDokumentasjon?,
    override val arena: ArenaFakta?,
    val aktivitet: FaktaAktivitet,
    val barn: List<FaktaBarn>,
) : BehandlingFaktaDto

data class BehandlingFaktaLæremidlerDto(
    override val søknadMottattTidspunkt: LocalDateTime?,
    override val hovedytelse: FaktaHovedytelse,
    override val dokumentasjon: FaktaDokumentasjon?,
    override val arena: ArenaFakta?,
    val utdanning: FaktaUtdanning,
    val alder: Int?,
) : BehandlingFaktaDto

data class BehandlingFaktaBoutgifterDto(
    override val søknadMottattTidspunkt: LocalDateTime?,
    override val hovedytelse: FaktaHovedytelse? = null,
    override val dokumentasjon: FaktaDokumentasjon? = null,
    override val arena: ArenaFakta?,
    val aktiviteter: FaktaAktivitet,
    val personopplysninger: FaktaPersonopplysninger,
    val boligEllerOvernatting: FaktaBoligEllerOvernatting?,
) : BehandlingFaktaDto

data class BehandlingFaktaDagligReiseDto(
    override val søknadMottattTidspunkt: LocalDateTime? = LocalDateTime.now(),
    override val hovedytelse: FaktaHovedytelse? = null,
    override val dokumentasjon: FaktaDokumentasjon? = null,
    override val arena: ArenaFakta? = null,
    val aktiviteter: FaktaAktivitetDagligReise,
    val reiser: List<FaktaReise>?,
    val personopplysninger: FaktaPersonopplysninger,
) : BehandlingFaktaDto

data class BehandlingFaktaReiseTilSamlingDto(
    override val søknadMottattTidspunkt: LocalDateTime? = LocalDateTime.now(),
    override val hovedytelse: FaktaHovedytelse? = null,
    override val dokumentasjon: FaktaDokumentasjon? = null,
    override val arena: ArenaFakta? = null,
    val aktiviteter: FaktaAktivitet,
    val samlinger: List<FaktaSamling>,
    val oppmøteadresse: FaktaOppmøteadresse?,
    val kanReiseKollektivt: JaNei?,
    val totalbeløpKollektivt: Int?,
    val årsakIkkeKollektivt: SøknadsskjemaReiseTilSamling.ÅrsakIkkeKollektivt?,
    val kanBenytteEgenBil: JaNei?,
    val årsakIkkeEgenBil: SøknadsskjemaReiseTilSamling.ÅrsakIkkeEgenBil?,
    val kanBenytteDrosje: JaNei?,
) : BehandlingFaktaDto

data class FaktaOppmøteadresse(
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class FaktaSamling(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class FaktaHovedytelse(
    val søknadsgrunnlag: SøknadsgrunnlagHovedytelse?,
)

data class FaktaUtdanning(
    val søknadsgrunnlag: SøknadsgrunnlagUtdanning?,
)

data class FaktaPersonopplysninger(
    val søknadsgrunnlag: FaktaPersonopplysningerSøknadsgrunnlag?,
)

data class FaktaPersonopplysningerSøknadsgrunnlag(
    val adresse: String?,
)

data class SøknadsgrunnlagHovedytelse(
    val hovedytelse: List<Hovedytelse>,
    val arbeidOgOpphold: FaktaArbeidOgOpphold?,
    val harNedsattArbeidsevne: JaNei?,
)

data class SøknadsgrunnlagUtdanning(
    val aktiviteter: List<String>?,
    val annenUtdanning: AnnenUtdanningType?,
    val harRettTilUtstyrsstipend: HarRettTilUtstyrsstipendDto?,
    val harFunksjonsnedsettelse: JaNei,
)

data class HarRettTilUtstyrsstipendDto(
    val erLærlingEllerLiknende: JaNei?,
    val harTidligereFullførtVgs: JaNei?,
)

data class FaktaArbeidOgOpphold(
    val jobberIAnnetLand: JaNei?,
    val jobbAnnetLand: String?,
    val harPengestøtteAnnetLand: List<TypePengestøtte>?,
    val pengestøtteAnnetLand: String?,
    val harOppholdUtenforNorgeSiste12mnd: JaNei?,
    val oppholdUtenforNorgeSiste12mnd: List<FaktaOppholdUtenforNorge>,
    val harOppholdUtenforNorgeNeste12mnd: JaNei?,
    val oppholdUtenforNorgeNeste12mnd: List<FaktaOppholdUtenforNorge>,
)

data class FaktaOppholdUtenforNorge(
    val land: String,
    val årsak: List<ÅrsakOppholdUtenforNorge>,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class FaktaAktivitet(
    val søknadsgrunnlag: SøknadsgrunnlagAktivitet?,
)

data class FaktaAktivitetDagligReise(
    val aktivitet: FaktaAktivitet,
)

data class SøknadsgrunnlagAktivitet(
    val aktiviteter: List<String>?,
    val annenAktivitet: AnnenAktivitetType?,
    val lønnetAktivitet: JaNei?,
    val dekkesUtgiftenAvAndre: DekkesUtgiftenAvAndre?,
)

data class DekkesUtgiftenAvAndre(
    val typeUtdanning: TypeUtdanning?,
    val lærling: JaNei?,
    val arbeidsgiverDekkerUtgift: JaNei?,
    val erUnder25år: JaNei?,
    val betalerForReisenTilSkolenSelv: JaNei?,
    val lønnetAktivitet: JaNei?,
)

data class FaktaBarn(
    val ident: String,
    val barnId: BarnId,
    val registergrunnlag: RegistergrunnlagBarn,
    val søknadgrunnlag: SøknadsgrunnlagBarn?,
    val vilkårFakta: VilkårFaktaBarn,
)

data class FaktaReise(
    val skalReiseFraFolkeregistrertAdresse: JaNei?,
    val adresseDetSkalReisesFra: ReiseAdresse?,
    val reiseAdresse: ReiseAdresse?,
    val periode: Reiseperiode,
    val dagerPerUke: String,
    val harMerEnn6KmReisevei: JaNei,
    val lengdeReisevei: Double?,
    val harBehovForTransportUavhengigAvReisensLengde: JaNei?,
    val kanReiseMedOffentligTransport: JaNei,
    val offentligTransport: OffentligTransport?,
    val privatTransport: PrivatTransport?,
)

/**
 * Kan brukes for å automatisk sette info på vilkår
 */
data class VilkårFaktaBarn(
    val harFullførtFjerdetrinn: JaNei?,
)

data class FaktaDokumentasjon(
    val journalpostId: String,
    val dokumentasjon: List<Dokumentasjon>,
)

data class Dokumentasjon(
    val type: String,
    val dokumenter: List<Dokument>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val identBarn: String? = null,
)

data class Dokument(
    val dokumentInfoId: String,
)

data class RegistergrunnlagBarn(
    val navn: String,
    val fødselsdato: LocalDate?,
    val alder: Int?,
    val dødsdato: LocalDate?,
    val saksinformasjonAndreForeldre: SaksinformasjonAndreForeldre?,
)

data class SaksinformasjonAndreForeldre(
    val hentetTidspunkt: LocalDateTime,
    val harBehandlingUnderArbeid: Boolean,
    val vedtaksperioderBarn: List<Datoperiode>,
)

data class SøknadsgrunnlagBarn(
    val type: TypeBarnepass,
    val startetIFemte: JaNei?,
    val utgifter: Utgifter?,
    val årsak: ÅrsakBarnepass?,
)

data class ArenaFakta(
    val vedtakTom: LocalDate?,
)
