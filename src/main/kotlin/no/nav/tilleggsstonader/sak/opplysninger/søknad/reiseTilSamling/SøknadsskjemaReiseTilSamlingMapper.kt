package no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
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
            AktivitetAvsnitt(
                aktiviteter =
                    skjema.aktivitet.aktiviteter
                        ?.verdier
                        ?.map { ValgtAktivitet(id = it.verdi, label = it.label) },
                annenAktivitet = skjema.aktivitet.annenAktivitet?.verdi,
                lønnetAktivitet = skjema.aktivitet.lønnetAktivitet?.verdi,
            ),
        samlinger =
            skjema.samlinger.mapNotNull { samling ->
                val fom = samling.fom?.verdi ?: return@mapNotNull null
                val tom = samling.tom?.verdi ?: return@mapNotNull null
                val erObligatorisk = samling.erObligatorisk?.verdi ?: return@mapNotNull null
                val harBruktEkstraReiseDager = samling.harBruktEkstraReiseDager?.verdi ?: return@mapNotNull null
                val antallKilometerEnVei = samling.antallKilometerEnVei?.verdi ?: return@mapNotNull null
                val adresse = samling.adresse ?: return@mapNotNull null

                SamlingPeriode(
                    fom = fom,
                    tom = tom,
                    erObligatorisk = erObligatorisk,
                    harBruktEkstraReiseDager = harBruktEkstraReiseDager,
                    antallKilometerEnVei = antallKilometerEnVei,
                    adresse =
                        Adresse(
                            gyldigFraOgMed = null,
                            adresse = adresse.gateadresse?.verdi,
                            postnummer = adresse.postnummer?.verdi,
                            poststed = adresse.poststed?.verdi,
                            landkode = adresse.land?.verdi,
                        ),
                )
            },
        avreiseadresse =
            Avreiseadresse(
                skalReiseFraFolkeregistrertAdresse = skjema.avreiseadresse.skalReiseFraFolkeregistrertAdresse.verdi,
                adresseDetSkalReisesFra =
                    Adresse(
                        gyldigFraOgMed = null,
                        adresse =
                            skjema.avreiseadresse.adresseDetSkalReisesFra
                                ?.gateadresse
                                ?.verdi,
                        postnummer =
                            skjema.avreiseadresse.adresseDetSkalReisesFra
                                ?.postnummer
                                ?.verdi,
                        poststed =
                            skjema.avreiseadresse.adresseDetSkalReisesFra
                                ?.poststed
                                ?.verdi,
                        landkode =
                            skjema.avreiseadresse.adresseDetSkalReisesFra
                                ?.land
                                ?.verdi,
                    ),
            ),
        reisemåte =
            Reisemåte(
                kanReiseMedOffentligTransport = skjema.reisemåte.kanReiseMedOffentligTransport.verdi,
                totalUtgifterOffentligTransport = skjema.reisemåte.totalUtgifterOffentligTransport?.verdi,
                kanBenytteEgenBil = skjema.reisemåte.kanBenytteEgenBil?.verdi,
                kanIkkeReiseMedOffentligTransportBegrunnelser =
                    skjema.reisemåte.kanIkkeReiseMedOffentligTransportBegrunnelser
                        ?.verdier
                        ?.map { it.verdi },
                ønskerDekketUtgifterForDrosje = skjema.reisemåte.ønskerDekketUtgifterForDrosje?.verdi,
                barnehageGateadresse = skjema.reisemåte.barnehageGateadresse?.verdi,
                barnehagePostnummer = skjema.reisemåte.barnehagePostnummer?.verdi,
                kanIkkeBenytteEgenBilBegrunnelser =
                    skjema.reisemåte.kanIkkeBenytteEgenBilBegrunnelser
                        ?.verdier
                        ?.map { it.verdi },
                betalerForReiseSelv = skjema.reisemåte.betalerForReiseSelv?.verdi,
                harTTKort = skjema.reisemåte.harTTKort?.verdi,
                reiseMedBilUtgifter =
                    skjema.reisemåte.reiseMedBilUtgifter?.let { reiseMedBilUtgifter ->
                        ReiseMedBilUtgifter(
                            drivstoffType = reiseMedBilUtgifter.drivstoffType.verdi,
                            bompenger = reiseMedBilUtgifter.bompenger?.verdi,
                            ferge = reiseMedBilUtgifter.ferge?.verdi,
                            piggdekkavgift = reiseMedBilUtgifter.piggdekkavgift?.verdi,
                        )
                    },
            ),
        dokumentasjon = mapDokumentasjon(skjema, journalpost),
    )
}
