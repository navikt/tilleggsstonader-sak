package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import java.time.LocalDate

data class SkjemaReiseTilSamling(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val samlinger: List<SamlingPeriode>,
    val reiseavstand: Reiseavstand,
    val reisemåte: Reisemåte,
    val dokumentasjon: List<Dokumentasjon>,
)

data class SamlingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Reiseavstand(
    val antallKilometerEnVei: String?,
    val land: String?,
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
)

data class Reisemåte(
    val kanReiseKollektivt: JaNei?,
    val totalutgifterKollektivt: String?,
    val kanBenytteEgenBil: JaNei?,
    val kanBenytteDrosje: JaNei?,
)
