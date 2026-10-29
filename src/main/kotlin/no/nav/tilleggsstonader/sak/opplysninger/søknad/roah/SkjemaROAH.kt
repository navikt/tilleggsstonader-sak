package no.nav.tilleggsstonader.sak.opplysninger.søknad.roah

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHTypeReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Reisemåte
import java.time.LocalDate

data class SkjemaROAH(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetAvsnitt,
    val adresser: ROAHAdresseAvsnitt,
    val barnOgSærligeBehov: ROAHBarnOgSærligeBehovAvsnitt,
    val reiser: List<ROAHReise>,
    val dokumentasjon: List<Dokumentasjon>,
)

data class ROAHAdresseAvsnitt(
    val måBoBorteHjemmefra: JaNei?,
    val periodeMidlertidigBosted: PeriodeMidlertidigBostedAvsnitt?,
    val adresseOriginaltBosted: AdresseOriginaltBostedAvsnitt?,
    val adresseMidlertidigBosted: Adresse?,
)

data class PeriodeMidlertidigBostedAvsnitt(
    val fomFlyttedato: LocalDate?,
    val tomFlyttedato: LocalDate?,
)

data class AdresseOriginaltBostedAvsnitt(
    val skalReiseFraFolkeregistrertAdresse: JaNei?,
    val adresseOriginaltBosted: Adresse?,
)

data class ROAHBarnOgSærligeBehovAvsnitt(
    val harBarnUnder18SomHarFlyttetMed: JaNei?,
    val hvilkeBarnFlytterMed: List<String>?,
    val harBarnHjemmeUnder4Klasse: JaNei?,
    val harSærligeBehovForFlereHjemreiser: JaNei?,
)

data class ROAHReise(
    val typeReise: ROAHTypeReise?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val reisemåte: Reisemåte?,
)
