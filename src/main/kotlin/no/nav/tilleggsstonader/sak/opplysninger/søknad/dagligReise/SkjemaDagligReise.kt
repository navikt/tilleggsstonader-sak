package no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import java.time.LocalDate

data class SkjemaDagligReise(
    val personopplysninger: Personopplysninger,
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val dokumentasjon: List<DokumentasjonDagligReise>,
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

data class DokumentasjonDagligReise(
    val tittel: String,
    val dokumentInfoId: String,
)
