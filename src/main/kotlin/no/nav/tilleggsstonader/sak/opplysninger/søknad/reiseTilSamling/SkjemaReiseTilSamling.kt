package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AktivitetTypeUtdanning
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrivstoffType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanIkkeBenytteEgenBilBegrunnelser
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.KanIkkeReiseMedOffentligTransportBegrunnelser
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import java.time.LocalDate

data class SkjemaReiseTilSamling(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetReiseTilSamlingAvsnitt,
    val samlinger: List<SamlingPeriode>,
    val avreiseadresse: Avreiseadresse,
    val reisemåte: Reisemåte,
    val dokumentasjon: List<Dokumentasjon>,
)

data class AktivitetReiseTilSamlingAvsnitt(
    val aktiviteter: List<ValgtAktivitet>?,
    val annenAktivitet: AnnenAktivitetType?,
    val lønnetAktivitet: JaNei?,
    val tilleggsopplysningerAnnenAktivitet: TilleggsopplysningerAnnenAktivitet?,
    val annenAktivitetTypeUtdanning: AktivitetTypeUtdanning?,
)

data class TilleggsopplysningerAnnenAktivitet(
    val erLærlingEllerLiknende: JaNei?,
    val fårDekketReise: JaNei?,
    val erUnder25År: JaNei?,
    val måBetaleForReiseTilSkole: JaNei?,
)

data class SamlingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val erObligatorisk: JaNei,
    val harBruktEkstraReiseDager: JaNei,
    val adresse: Adresse,
    val antallKilometerEnVei: String,
)

data class Avreiseadresse(
    val skalReiseFraFolkeregistrertAdresse: JaNei,
    val adresseDetSkalReisesFra: Adresse?,
)

data class Reisemåte(
    val kanReiseMedOffentligTransport: JaNei,
    val kanIkkeReiseMedOffentligTransportBegrunnelser: List<KanIkkeReiseMedOffentligTransportBegrunnelser>?,
    val totalUtgifterOffentligTransport: String?,
    val kanBenytteEgenBil: KanBenytteEgenBil?,
    val ønskerDekketUtgifterForDrosje: JaNei?,
    val barnehageGateadresse: String?,
    val barnehagePostnummer: String?,
    val kanIkkeBenytteEgenBilBegrunnelser: List<KanIkkeBenytteEgenBilBegrunnelser>?,
    val betalerForReiseSelv: JaNei?,
    val harTTKort: JaNei?,
    val reiseMedBilUtgifter: ReiseMedBilUtgifter?,
)

data class ReiseMedBilUtgifter(
    val drivstoffType: DrivstoffType,
    val bompenger: String?,
    val ferge: String?,
    val piggdekkavgift: String?,
)
