package no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import java.time.LocalDate

// data class SkjemaBoutgifter()

data class HovedytelseAvsnitt(
    val hovedytelse: List<Hovedytelse>,
)

data class UtgifterIForbindelseMedSamling(
    val periodeForSamling: List<PeriodeForSamling>,
)

data class PeriodeForSamling(
    val fom: LocalDate,
    val tom: LocalDate,
    val trengteDuEnEkstraOvernattingPaGrunnAvTidligOppstartEllerSenAvslutning: String,
    val hvorMyeHaddeDuIUtgifterTilOvernattingTotaltForDenneSamlingen: Int,
)
