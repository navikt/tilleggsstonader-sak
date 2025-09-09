package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import java.time.LocalDate

data class SkjemaDagligReise(
    val personopplysninger: Personopplysninger,
    val annenAdresseDetSkalReisesFra: ReiseAdresse?,
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetDagligReiseAvsnitt,
    val reiser: List<Reise>,
    val dokumentasjon: List<DokumentasjonDagligReise>,
)

data class AktivitetDagligReiseAvsnitt(
    val aktivitet: AktivitetAvsnitt,
    val reiseTilAktivitetsstedHelePerioden: JaNei?,
    val reiseperiode: Reiseperiode?,
)

data class Reiseperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Reise(
    val reiseAdresse: ReiseAdresse,
    val dagerPerUke: ValgtAktivitetDagligReise,
    val harMerEnn6KmReisevei: JaNei,
    val lengdeReisevei: Int?,
    val harBehovForTransportUavhengigAvReisensLengde: JaNei?,
    val kanReiseMedOffentligTransport: JaNei,
    val offentligTransport: OffentligTransport?,
    val privatTransport: PrivatTransport?,
)

data class ReiseAdresse(
    val gateadresse: String,
    val postnummer: String,
    val poststed: String,
)

data class ValgtAktivitetDagligReise(
    val id: String,
    val label: String,
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
    MÅNEDSKORT,
}

data class PrivatTransport(
    val årsakIkkeOffentligTransport: List<ÅrsakIkkeOffentligTransport>,
    val kanKjøreMedEgenBil: JaNei?,
    val utgifterBil: UtgifterBil?,
    val utgifterTaxi: UtgifterTaxi?,
)

enum class ÅrsakIkkeOffentligTransport {
    HELSEMESSIGE_ÅRSAKER,
    DÅRLIG_TRANSPORTTILBUD,
    LEVERING_HENTING_BARNEHAGE_SKOLE,
    ANNET,
}

data class UtgifterBil(
    val merEnn6kmReisevei: JaNei?,
    val bompenger: Int?,
    val ferge: Int?,
    val piggdekkavgift: Int?,
)

data class UtgifterTaxi(
    val årsakIkkeKjøreBil: List<ÅrsakIkkeKjøreBil>,
    val ønskerSøkeOmTaxi: JaNei,
)

enum class ÅrsakIkkeKjøreBil {
    HELSEMESSIGE_ÅRSAKER,
    DÅRLIG_TRANSPORTTILBUD,
    ANNET,
}

data class DokumentasjonDagligReise(
    val tittel: String,
    val dokumentInfoId: String,
)
