package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import java.time.LocalDate

data class SkjemaDagligReise(
    val personopplysninger: Personopplysninger,
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetDagligReiseAvsnitt,
    val reiser: List<Reise>,
    val dokumentasjon: List<DokumentasjonDagligReise>,
)

data class AktivitetDagligReiseAvsnitt(
    val aktiviteter: List<AktivitetDagligReise>?,
    val dekkesUtgiftenAvAndre: DekkesUtgiftenAvAndre,
    val annenAktivitet: AnnenAktivitetType?,
)

data class AktivitetDagligReise(
    val id: String,
    val label: String,
    val type: String?,
)

data class DekkesUtgiftenAvAndre(
    val typeUtdanning: TypeUtdanning,
    val lærling: JaNei?,
    val arbeidsgiverDekkerUtgift: JaNei?,
    val erUnder25år: JaNei?,
    val betalerForReisenTilSkolenSelv: JaNei?,
    val lønnetAktivitet: JaNei?,
)

enum class TypeUtdanning {
    VIDEREGÅENDE,
    OPPLÆRING_FOR_VOKSNE,
    ANNET_TILTAK,
}

data class Reiseperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Reise(
    val skalReiseFraFolkeregistrertAdresse: JaNei,
    val adresseDetSkalReisesFra: ReiseAdresse?,
    val adresse: ReiseAdresse,
    val periode: Reiseperiode,
    val dagerPerUke: String,
    val harMerEnn6KmReisevei: JaNei,
    val lengdeReisevei: Int,
    val harBehovForTransportUavhengigAvReisensLengde: JaNei?,
    val kanReiseMedOffentligTransport: KanDuReiseMedOffentligTransport,
    val offentligTransport: OffentligTransport?,
    val privatTransport: PrivatTransport?,
)

data class ReiseAdresse(
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class OffentligTransport(
    val billettTyperValgt: List<BillettType>,
    val enkeltbillettPris: Int?,
    val syvdagersbillettPris: Int?,
    val månedskortPris: Int?,
)

enum class BillettType {
    ENKELTBILLETT,
    SYVDAGERSBILLETT,
    TRETTIDAGERSBILLETT,
}

data class PrivatTransport(
    val årsakIkkeOffentligTransport: List<ÅrsakIkkeOffentligTransport>,
    val kanKjøreMedEgenBil: JaNei?,
    val utgifterBil: UtgifterBil?,
    val taxi: Taxi?,
)

enum class KanDuReiseMedOffentligTransport {
    JA,
    NEI,
    KOMBINERT_BIL_OFFENTLIG_TRANSPORT, ;

    fun kanReiseMedOffentligTransport(): Boolean = this == JA || this == KOMBINERT_BIL_OFFENTLIG_TRANSPORT
}

enum class ÅrsakIkkeOffentligTransport {
    HELSEMESSIGE_ÅRSAKER,
    DÅRLIG_TRANSPORTTILBUD,
    LEVERING_HENTING_BARNEHAGE_SKOLE,
    ANNET,
}

data class UtgifterBil(
    val mottarGrunnstønad: JaNei?,
    val reisedistanseEgenBil: Int,
    val destinasjonEgenBil: List<DestinasjonEgenBil>?,
    val parkering: JaNei,
    val bompenger: Int?,
    val ferge: Int?,
    val piggdekkavgift: Int?,
)

data class Taxi(
    val årsakIkkeKjøreBil: List<ÅrsakIkkeKjøreBil>,
    val ønskerSøkeOmTaxi: JaNei,
    val ttkort: JaNei?,
)

enum class ÅrsakIkkeKjøreBil {
    HELSEMESSIGE_ÅRSAKER,
    HAR_IKKE_BIL_FØRERKORT,
    ANNET,
}

data class DokumentasjonDagligReise(
    val tittel: String,
    val dokumentInfoId: String,
)

enum class DestinasjonEgenBil {
    TOGSTASJON,
    BUSSSTOPP,
    FERGE_BAT_KAI,
}
