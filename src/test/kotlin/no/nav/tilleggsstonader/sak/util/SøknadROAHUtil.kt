package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaROAH
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.OffentligTransportInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseMidlertidigBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAdresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHBarnOgSærligeBehovAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHReiseInfo
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHTypeReise
import no.nav.tilleggsstonader.libs.utils.dato.mars
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument as KontraktDokument
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt as KontraktHovedytelseAvsnitt

object SøknadROAHUtil {
    fun søknadROAH(ident: String = "11111122222"): InnsendtSkjema<SøknadsskjemaROAH> {
        val vedleggsId = UUID.randomUUID()
        val skjema =
            SøknadsskjemaROAH(
                hovedytelse =
                    KontraktHovedytelseAvsnitt(
                        hovedytelse =
                            EnumFlereValgFelt(
                                label = "Hovedytelse",
                                verdier = listOf(VerdiFelt(Hovedytelse.AAP, "Arbeidsavklaringspenger")),
                                alternativer = emptyList(),
                            ),
                        arbeidOgOpphold = null,
                    ),
                aktivitet = ROAHAktivitetAvsnitt(null, null, null),
                adresser =
                    ROAHAdresseAvsnitt(
                        måBoBorteHjemmefra = EnumFelt("Må bo borte", JaNei.JA, "Ja", emptyList()),
                        periodeMidlertidigBosted = null,
                        adresseOriginaltBosted = null,
                        adresseMidlertidigBosted =
                            AdresseMidlertidigBostedAvsnitt(
                                land = SelectFelt("Land", "NOR", "Norge"),
                                gateadresse = null,
                                postnummer = null,
                                poststed = null,
                            ),
                    ),
                barnOgSærligeBehov = ROAHBarnOgSærligeBehovAvsnitt(null, null, null, null),
                reiser =
                    listOf(
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.OPPSTART, "Oppstart", emptyList()),
                            fom = DatoFelt("Fra", 3 mars 2026),
                            tom = null,
                            reisemåte =
                                ReisemåteAvsnitt(
                                    hvilkeTransportmidlerBleBenyttet =
                                        EnumFlereValgFelt(
                                            "Transportmidler",
                                            listOf(VerdiFelt(Transportmiddel.OFFENTLIG_TRANSPORT, "Offentlig transport")),
                                            emptyList(),
                                        ),
                                    offentligTransport = OffentligTransportInfo(VerdiFelt("1200", "Reiseutgifter")),
                                    unntakFraOffentligTransport = null,
                                    unntakFraPrivatBil = null,
                                    privatBil = null,
                                    drosje = null,
                                ),
                        ),
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.AVSLUTNING, "Avslutning", emptyList()),
                            fom = DatoFelt("Fra", 4 mars 2026),
                            tom = DatoFelt("Til", 6 mars 2026),
                            reisemåte = null,
                        ),
                        ROAHReiseInfo(
                            typeReise = EnumFelt("Type reise", ROAHTypeReise.HJEMREISE, "Hjemreise", emptyList()),
                            fom = DatoFelt("Fra", 7 mars 2026),
                            tom = null,
                            reisemåte = null,
                        ),
                    ),
                dokumentasjon =
                    listOf(
                        DokumentasjonFelt(
                            type = Vedleggstype.UTGIFTER_ROAH,
                            label = "Utgifter",
                            opplastedeVedlegg = listOf(KontraktDokument(vedleggsId, "reiseutgift.pdf")),
                        ),
                    ),
            )
        return InnsendtSkjema(
            ident = ident,
            mottattTidspunkt = (2 mars 2026).atTime(12, 0),
            språk = Språkkode.NB,
            skjema = skjema,
        )
    }
}
