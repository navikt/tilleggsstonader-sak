package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.AktivitetTypeUtdanning
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrivstoffType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteOffentligTransport
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import java.time.LocalDate

data class SkjemaReiseTilSamling(
    val hovedytelse: HovedytelseAvsnitt,
    val aktivitet: AktivitetReiseTilSamlingAvsnitt,
    val samlinger: List<Samling>,
    val avreiseadresse: Avreiseadresse,
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

data class Samling(
    val fom: LocalDate,
    val tom: LocalDate,
    val erObligatorisk: JaNei,
    val adresse: Adresse,
    val antallKilometerEnVei: String,
    val reisemåte: Reisemåte? = null,
)

data class Avreiseadresse(
    val skalReiseFraFolkeregistrertAdresse: JaNei,
    val adresseDetSkalReisesFra: Adresse?,
)

data class Reisemåte(
    val hvilkeTransportmidlerBleBenyttet: List<Transportmiddel>?,
    val unntakFraOffentligTransport: UnntakFraOffentligTransport?,
    val unntakFraPrivatBil: List<ÅrsakKanIkkeBenytteEgenBil>?,
    val offentligTransport: OffentligTransportInfo?,
    val privatBil: PrivatBilInfo?,
    val drosje: DrosjeInfo?,
)

data class OffentligTransportInfo(
    val totalUtgifterOffentligTransport: String?,
)

data class PrivatBilInfo(
    val benyttetEgenBil: JaNei?,
    val betalteForReisen: JaNei?,
    val infoBilKunDelerAvStrekning: InfoBilKunDelerAvStrekning?,
    val utgifterPrivatBil: UtgifterPrivatBil?,
)

data class DrosjeInfo(
    val harTTKort: JaNei?,
)

data class UnntakFraOffentligTransport(
    val årsaker: List<ÅrsakKanIkkeBenytteOffentligTransport>?,
    val leveringOgHentingIBarnehage: LeveringOgHentingIBarnehage?,
)

data class LeveringOgHentingIBarnehage(
    val gateadresse: String?,
    val postnummer: String?,
)

data class UtgifterPrivatBil(
    val bompenger: String?,
    val ferge: String?,
    val piggdekkavgift: String?,
    val parkering: String?,
    val drivstoffType: DrivstoffType?,
)

data class InfoBilKunDelerAvStrekning(
    val strekningHvorBilBleBenyttet: String?,
    val antallKilometerKjørt: String?,
)
