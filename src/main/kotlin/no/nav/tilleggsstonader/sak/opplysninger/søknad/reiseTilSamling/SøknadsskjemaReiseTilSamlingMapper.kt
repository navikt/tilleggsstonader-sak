package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import java.time.LocalDateTime

object SøknadsskjemaReiseTilSamlingMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaReiseTilSamling,
    ): SøknadReiseTilSamling =
        SøknadReiseTilSamling(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data = mapSkjemaReiseTilSamling(skjema, journalpost),
        )

    private fun mapSkjemaReiseTilSamling(
        skjema: SøknadsskjemaReiseTilSamling,
        journalpost: Journalpost,
    ) = SkjemaReiseTilSamling(
        hovedytelse =
            HovedytelseAvsnitt(
                hovedytelse =
                    skjema.hovedytelse.hovedytelse.verdier
                        .map { it.verdi },
                harNedsattArbeidsevne = null, // Finnes ikke i søknad ennå
                arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
            ),
        aktivitet =
            AktivitetReiseTilSamlingAvsnitt(
                aktiviteter =
                    skjema.aktivitet.aktiviteter
                        ?.verdier
                        ?.map { ValgtAktivitet(id = it.verdi, label = it.label) },
                annenAktivitet = skjema.aktivitet.annenAktivitet?.verdi,
                lønnetAktivitet = skjema.aktivitet.lønnetAktivitet?.verdi,
                tilleggsopplysningerAnnenAktivitet =
                    skjema.aktivitet.tilleggsopplysningerAnnenAktivitet?.let {
                        TilleggsopplysningerAnnenAktivitet(
                            erLærlingEllerLiknende = it.erLærlingEllerLiknende?.verdi,
                            fårDekketReise = it.fårDekketReise?.verdi,
                            erUnder25År = it.erUnder25År?.verdi,
                            måBetaleForReiseTilSkole = it.måBetaleForReiseTilSkole?.verdi,
                        )
                    },
                annenAktivitetTypeUtdanning = skjema.aktivitet.annenAktivitetTypeUtdanning?.verdi,
            ),
        samlinger =
            skjema.samlinger.mapNotNull { samling ->
                val fom = samling.fom?.verdi ?: return@mapNotNull null
                val tom = samling.tom?.verdi ?: return@mapNotNull null
                val erObligatorisk = samling.erObligatorisk?.verdi ?: return@mapNotNull null
                val antallKilometerEnVei = samling.antallKilometerEnVei?.verdi ?: return@mapNotNull null
                val adresse = samling.adresse ?: return@mapNotNull null

                Samling(
                    fom = fom,
                    tom = tom,
                    erObligatorisk = erObligatorisk,
                    antallKilometerEnVei = antallKilometerEnVei,
                    adresse =
                        Adresse(
                            gyldigFraOgMed = null,
                            adresse = adresse.gateadresse?.verdi,
                            postnummer = adresse.postnummer?.verdi,
                            poststed = adresse.poststed?.verdi,
                            landkode = adresse.land?.verdi,
                        ),
                    reisemåte =
                        samling.reisemåte?.let { reisemåte ->
                            Reisemåte(
                                hvilkeTransportmidlerBleBenyttet =
                                    reisemåte.hvilkeTransportmidlerBleBenyttet
                                        ?.verdier
                                        ?.map { it.verdi },
                                unntakFraOffentligTransport =
                                    reisemåte.unntakFraOffentligTransport?.let { unntak ->
                                        UnntakFraOffentligTransport(
                                            årsaker = unntak.årsaker?.verdier?.map { it.verdi },
                                            leveringOgHentingIBarnehage =
                                                unntak.leveringOgHentingIBarnehage?.let {
                                                    LeveringOgHentingIBarnehage(
                                                        gateadresse = it.gateadresse?.verdi,
                                                        postnummer = it.postnummer?.verdi,
                                                    )
                                                },
                                        )
                                    },
                                unntakFraPrivatBil =
                                    reisemåte.unntakFraPrivatBil
                                        ?.verdier
                                        ?.map { it.verdi },
                                offentligTransport =
                                    reisemåte.offentligTransport?.let {
                                        OffentligTransportInfo(
                                            totalUtgifterOffentligTransport = it.totalUtgifterOffentligTransport?.verdi,
                                        )
                                    },
                                privatBil =
                                    reisemåte.privatBil?.let { privatBil ->
                                        PrivatBilInfo(
                                            benyttetEgenBil = privatBil.benyttetEgenBil?.verdi,
                                            betalteForReisen = privatBil.betalteForReisen?.verdi,
                                            infoBilKunDelerAvStrekning =
                                                privatBil.infoBilKunDelerAvStrekning?.let {
                                                    InfoBilKunDelerAvStrekning(
                                                        strekningHvorBilBleBenyttet = it.strekningHvorBilBleBenyttet?.verdi,
                                                        antallKilometerKjørt = it.antallKilometerKjørt?.verdi,
                                                    )
                                                },
                                            utgifterPrivatBil =
                                                privatBil.utgifterPrivatBil?.let { utgifterPrivatBil ->
                                                    UtgifterPrivatBil(
                                                        bompenger = utgifterPrivatBil.bompenger?.verdi,
                                                        ferge = utgifterPrivatBil.ferge?.verdi,
                                                        piggdekkavgift = utgifterPrivatBil.piggdekkavgift?.verdi,
                                                        parkering = utgifterPrivatBil.parkering?.verdi,
                                                        drivstoffType = utgifterPrivatBil.drivstoffType?.verdi,
                                                    )
                                                },
                                        )
                                    },
                                drosje =
                                    reisemåte.drosje?.let {
                                        DrosjeInfo(
                                            harTTKort = it.harTTKort?.verdi,
                                        )
                                    },
                            )
                        },
                )
            },
        avreiseadresse =
            Avreiseadresse(
                skalReiseFraFolkeregistrertAdresse = skjema.avreiseadresse.skalReiseFraFolkeregistrertAdresse.verdi,
                adresseDetSkalReisesFra =
                    skjema.avreiseadresse.adresseDetSkalReisesFra?.let {
                        Adresse(
                            adresse = it.gateadresse?.verdi,
                            postnummer = it.postnummer?.verdi,
                            poststed = it.poststed?.verdi,
                            landkode = it.land?.verdi,
                            gyldigFraOgMed = null,
                        )
                    },
            ),
        dokumentasjon = mapDokumentasjon(skjema, journalpost),
    )
}
