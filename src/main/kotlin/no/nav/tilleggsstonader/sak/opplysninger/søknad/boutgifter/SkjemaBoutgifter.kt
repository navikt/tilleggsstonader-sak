package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import java.time.LocalDate

data class SkjemaBoutgifter(
    val personopplysninger: Personopplysninger,
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val boutgifter: BoligEllerOvernattingAvsnitt,
    val dokumentasjon: List<DokumentasjonBoutgifter>,
    val harNedsattArbeidsevne: JaNei?,
)

data class Personopplysninger(
    val adresse: Adresse?,
)

data class Adresse(
    val gyldigFraOgMed: LocalDate,
    val adresse: String,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)

data class BoligEllerOvernattingAvsnitt(
    val typeUtgifter: TypeUtgifter,
    val fasteUtgifter: FasteUtgifter?,
    val samling: UtgifterIForbindelseMedSamling?,
    val harSærligStoreUtgifterPgaFunksjonsnedsettelse: JaNei,
)

data class DokumentasjonBoutgifter(
    val tittel: String,
    val dokumentInfoId: String,
)

enum class TypeUtgifter {
    FASTE,
    SAMLING,
}

enum class TypeFasteUtgifter {
    EKSTRA_BOLIG,
    NY_BOLIG,
}

data class FasteUtgifter(
    val typeFasteUtgifter: TypeFasteUtgifter,
    val utgifterFlereSteder: UtgifterFlereSteder?,
    val utgifterNyBolig: UtgifterNyBolig?,
)

data class UtgifterNyBolig(
    val delerBoutgifter: JaNei,
    val andelUtgifterBolig: Int?,
    val harHoyereUtgifterPaNyttBosted: JaNei,
    val mottarBostotte: JaNei,
)

data class UtgifterFlereSteder(
    val delerBoutgifter: List<DelerUtgifterFlereStederType>,
    val andelUtgifterBoligHjemsted: Int,
    val andelUtgifterBoligAktivitetssted: Int,
)

enum class DelerUtgifterFlereStederType {
    HJEMSTED,
    AKTIVITETSSTED,
    NEI,
}

data class UtgifterIForbindelseMedSamling(
    val periodeForSamling: List<PeriodeForSamling>,
)

data class PeriodeForSamling(
    val fom: LocalDate,
    val tom: LocalDate,
    val trengteEkstraOvernatting: JaNei,
    val utgifterTilOvernatting: Int?,
)
