package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import java.time.LocalDate

data class SkjemaBoutgifter(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val boutgifter: BoligEllerOvernattingAvsnitt,
)

data class BoligEllerOvernattingAvsnitt(
    val typeUtgifter: TypeUtgifter,
    val fasteUtgifter: FasteUtgifter?,
    val samling: UtgifterIForbindelseMedSamling?,
    val harSærligStoreUtgifterPgaFunksjonsnedsettelse: JaNei,
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
    val harUtgifterTilBoligToSteder: TypeFasteUtgifter,
    val harLeieinntekterSomDekkerUtgifteneTilBoligenPaHjemstedet: JaNei?,
    val harHoyereUtgifterPaNyttBosted: JaNei?,
    val mottarBostotte: JaNei?,
)

data class UtgifterIForbindelseMedSamling(
    val periodeForSamling: List<PeriodeForSamling>,
)

data class PeriodeForSamling(
    val fom: LocalDate,
    val tom: LocalDate,
    val trengteEkstraOvernatting: JaNei,
    val utgifterTilOvernatting: Int,
)
