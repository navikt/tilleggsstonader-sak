package no.nav.tilleggsstonader.sak.opplysninger.søknad.roah

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaROAH
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.AktivitetAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadROAH
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ArbeidOgOppholdMapper.mapArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.DokumentasjonMapper.mapDokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.ReisemåteMapper.mapReisemåte
import java.time.LocalDateTime

object SøknadsskjemaROAHMapper {
    fun map(
        mottattTidspunkt: LocalDateTime,
        språk: Språkkode,
        journalpost: Journalpost,
        skjema: SøknadsskjemaROAH,
    ): SøknadROAH =
        SøknadROAH(
            journalpostId = journalpost.journalpostId,
            mottattTidspunkt = mottattTidspunkt,
            språk = språk,
            data =
                SkjemaROAH(
                    hovedytelse =
                        HovedytelseAvsnitt(
                            hovedytelse =
                                skjema.hovedytelse.hovedytelse.verdier
                                    .map { it.verdi },
                            harNedsattArbeidsevne = null,
                            arbeidOgOpphold = mapArbeidOgOpphold(skjema.hovedytelse.arbeidOgOpphold),
                        ),
                    aktivitet =
                        AktivitetAvsnitt(
                            aktiviteter =
                                skjema.aktivitet.aktiviteter
                                    ?.verdier
                                    ?.map { ValgtAktivitet(it.verdi, it.label) },
                            annenAktivitet = skjema.aktivitet.annenAktivitet?.verdi,
                            lønnetAktivitet = skjema.aktivitet.lønnetAktivitet?.verdi,
                        ),
                    adresser =
                        ROAHAdresseAvsnitt(
                            måBoBorteHjemmefra = skjema.adresser.måBoBorteHjemmefra?.verdi,
                            periodeMidlertidigBosted =
                                skjema.adresser.periodeMidlertidigBosted?.let {
                                    PeriodeMidlertidigBostedAvsnitt(
                                        fomFlyttedato = it.fomFlyttedato?.verdi,
                                        tomFlyttedato = it.tomFlyttedato?.verdi,
                                    )
                                },
                            adresseOriginaltBosted =
                                skjema.adresser.adresseOriginaltBosted?.let { originaltBosted ->
                                    AdresseOriginaltBostedAvsnitt(
                                        skalReiseFraFolkeregistrertAdresse = originaltBosted.skalReiseFraFolkeregistrertAdresse?.verdi,
                                        adresseOriginaltBosted =
                                            originaltBosted.adresseOriginaltBosted?.let {
                                                Adresse(
                                                    gyldigFraOgMed = null,
                                                    adresse = it.gateadresse?.verdi,
                                                    postnummer = it.postnummer?.verdi,
                                                    poststed = it.poststed?.verdi,
                                                    landkode = it.land?.verdi,
                                                )
                                            },
                                    )
                                },
                            adresseMidlertidigBosted =
                                skjema.adresser.adresseMidlertidigBosted?.let {
                                    Adresse(
                                        gyldigFraOgMed = null,
                                        adresse = it.gateadresse?.verdi,
                                        postnummer = it.postnummer?.verdi,
                                        poststed = it.poststed?.verdi,
                                        landkode = it.land?.verdi,
                                    )
                                },
                        ),
                    barnOgSærligeBehov =
                        ROAHBarnOgSærligeBehovAvsnitt(
                            harBarnUnder18SomHarFlyttetMed = skjema.barnOgSærligeBehov.harBarnUnder18SomHarFlyttetMed?.verdi,
                            hvilkeBarnFlytterMed =
                                skjema.barnOgSærligeBehov.hvilkeBarnFlytterMed
                                    ?.verdier
                                    ?.map { it.verdi },
                            harBarnHjemmeUnder4Klasse = skjema.barnOgSærligeBehov.harBarnHjemmeUnder4Klasse?.verdi,
                            harSærligeBehovForFlereHjemreiser = skjema.barnOgSærligeBehov.harSærligeBehovForFlereHjemreiser?.verdi,
                        ),
                    reiser =
                        skjema.reiser.map {
                            ROAHReise(
                                typeReise = it.typeReise?.verdi,
                                fom = it.fom?.verdi,
                                tom = it.tom?.verdi,
                                reisemåte = it.reisemåte?.let(::mapReisemåte),
                            )
                        },
                    dokumentasjon = mapDokumentasjon(skjema, journalpost),
                ),
        )
}
