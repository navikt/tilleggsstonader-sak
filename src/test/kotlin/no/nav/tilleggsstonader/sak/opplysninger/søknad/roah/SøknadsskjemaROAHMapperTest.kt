package no.nav.tilleggsstonader.sak.opplysninger.søknad.roah

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaROAH
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ReisemåteAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrivstoffType
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.DrosjeInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.InfoBilKunDelerAvStrekning
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.LeveringOgHentingIBarnehage
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.OffentligTransportInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.PrivatBilInfo
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Transportmiddel
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.UnntakFraOffentligTransport
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.UtgifterPrivatBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteEgenBil
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.ÅrsakKanIkkeBenytteOffentligTransport
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseMidlertidigBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseOriginaltBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.AdresseOriginaltBostedDetaljerAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.PeriodeMidlertidigBostedAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAdresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHAktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHBarnOgSærligeBehovAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHReiseInfo
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHTypeReise
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ValgtAktivitet
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadsskjemaROAHMapperTest {
    @Test
    fun `skal koble vedlegg til journalpostens dokument og avvise manglende vedlegg`() {
        val vedleggId = UUID.randomUUID()
        val skjema =
            skjema().copy(
                dokumentasjon =
                    listOf(
                        DokumentasjonFelt(
                            type = Vedleggstype.UTGIFTER_ROAH,
                            label = "Reiseutgifter",
                            opplastedeVedlegg = listOf(Dokument(vedleggId, "kvittering.pdf")),
                        ),
                    ),
            )
        val journalpost =
            journalpost(
                dokumenter =
                    listOf(
                        dokumentInfo(
                            dokumentInfoId = "dokument-1",
                            dokumentvarianter = listOf(dokumentvariant(filnavn = vedleggId.toString())),
                        ),
                    ),
            )

        val dokumentasjon =
            SøknadsskjemaROAHMapper
                .map((2 januar 2026).atStartOfDay(), Språkkode.NB, journalpost, skjema)
                .data.dokumentasjon
                .single()

        assertThat(dokumentasjon.type).isEqualTo(Vedleggstype.UTGIFTER_ROAH)
        assertThat(dokumentasjon.dokumenter.single().dokumentInfoId).isEqualTo("dokument-1")
        assertThatThrownBy { map(skjema) }.hasMessageContaining("Finner ikke vedlegg")
    }

    @Test
    fun `skal bevare adresser barn og aktivitet som domeneverdier`() {
        val skjema =
            skjema().copy(
                aktivitet =
                    ROAHAktivitetAvsnitt(
                        aktiviteter = flereValg("aktivitet-1"),
                        annenAktivitet = felt(AnnenAktivitetType.entries.first()),
                        lønnetAktivitet = felt(JaNei.NEI),
                    ),
                adresser =
                    ROAHAdresseAvsnitt(
                        måBoBorteHjemmefra = felt(JaNei.JA),
                        periodeMidlertidigBosted =
                            PeriodeMidlertidigBostedAvsnitt(
                                fomFlyttedato = DatoFelt("Fra", 1 januar 2026),
                                tomFlyttedato = null,
                            ),
                        adresseOriginaltBosted =
                            AdresseOriginaltBostedAvsnitt(
                                skalReiseFraFolkeregistrertAdresse = felt(JaNei.NEI),
                                adresseOriginaltBosted =
                                    AdresseOriginaltBostedDetaljerAvsnitt(
                                        land = SelectFelt("Land", "NOR", "Norge"),
                                        gateadresse = VerdiFelt("Testgata 1", "Adresse"),
                                        postnummer = VerdiFelt("0001", "Postnummer"),
                                        poststed = VerdiFelt("Teststed", "Poststed"),
                                    ),
                            ),
                        adresseMidlertidigBosted =
                            AdresseMidlertidigBostedAvsnitt(
                                land = SelectFelt("Land", "SWE", "Sverige"),
                                gateadresse = VerdiFelt("Testveien 2", "Adresse"),
                                postnummer = null,
                                poststed = null,
                            ),
                    ),
                barnOgSærligeBehov =
                    ROAHBarnOgSærligeBehovAvsnitt(
                        harBarnUnder18SomHarFlyttetMed = felt(JaNei.JA),
                        hvilkeBarnFlytterMed = flereValg("barn-1", "barn-2"),
                        harBarnHjemmeUnder4Klasse = felt(JaNei.NEI),
                        harSærligeBehovForFlereHjemreiser = felt(JaNei.JA),
                    ),
            )

        val søknad = map(skjema)
        val data = søknad.data

        assertThat(søknad.journalpostId).isEqualTo("12345")
        assertThat(søknad.språk).isEqualTo(Språkkode.NB)
        assertThat(søknad.mottattTidspunkt).isEqualTo((2 januar 2026).atTime(12, 0))
        assertThat(data.hovedytelse.hovedytelse).containsExactly(Hovedytelse.AAP)
        assertThat(data.aktivitet.aktiviteter).containsExactly(ValgtAktivitet("aktivitet-1", "aktivitet-1"))
        assertThat(data.aktivitet.annenAktivitet).isEqualTo(skjema.aktivitet.annenAktivitet?.verdi)
        assertThat(data.aktivitet.lønnetAktivitet).isEqualTo(JaNei.NEI)
        assertThat(data.adresser.måBoBorteHjemmefra).isEqualTo(JaNei.JA)
        assertThat(data.adresser.periodeMidlertidigBosted?.fomFlyttedato).isEqualTo(1 januar 2026)
        assertThat(data.adresser.periodeMidlertidigBosted?.tomFlyttedato).isNull()
        assertThat(data.adresser.adresseOriginaltBosted?.skalReiseFraFolkeregistrertAdresse).isEqualTo(JaNei.NEI)
        assertThat(data.adresser.adresseOriginaltBosted?.adresseOriginaltBosted)
            .isEqualTo(Adresse(null, "Testgata 1", "0001", "Teststed", "NOR"))
        assertThat(data.adresser.adresseMidlertidigBosted)
            .isEqualTo(Adresse(null, "Testveien 2", null, null, "SWE"))
        assertThat(data.barnOgSærligeBehov.harBarnUnder18SomHarFlyttetMed).isEqualTo(JaNei.JA)
        assertThat(data.barnOgSærligeBehov.hvilkeBarnFlytterMed).containsExactly("barn-1", "barn-2")
        assertThat(data.barnOgSærligeBehov.harBarnHjemmeUnder4Klasse).isEqualTo(JaNei.NEI)
        assertThat(data.barnOgSærligeBehov.harSærligeBehovForFlereHjemreiser).isEqualTo(JaNei.JA)
    }

    @Test
    fun `skal beholde alle reisetyper og reiser uten sluttdato`() {
        val skjema =
            skjema().copy(
                reiser =
                    ROAHTypeReise.entries.map { type ->
                        ROAHReiseInfo(
                            typeReise = felt(type),
                            fom = DatoFelt("Fra", 3 januar 2026),
                            tom = if (type == ROAHTypeReise.HJEMREISE) DatoFelt("Til", 5 januar 2026) else null,
                            reisemåte = reisemåte(),
                        )
                    },
            )

        val reiser = map(skjema).data.reiser

        assertThat(reiser.map { it.typeReise }).containsExactlyElementsOf(ROAHTypeReise.entries)
        assertThat(reiser.map { it.fom }).containsOnly(3 januar 2026)
        assertThat(reiser.map { it.tom }).containsExactly(null, null, 5 januar 2026)
        val reisemåte = reiser.first().reisemåte
        assertThat(reisemåte?.hvilkeTransportmidlerBleBenyttet).containsExactlyElementsOf(Transportmiddel.entries)
        assertThat(reisemåte?.offentligTransport?.totalUtgifterOffentligTransport).isEqualTo("1500")
        assertThat(reisemåte?.privatBil?.benyttetEgenBil).isEqualTo(JaNei.JA)
        assertThat(reisemåte?.privatBil?.betalteForReisen).isEqualTo(JaNei.NEI)
        assertThat(reisemåte?.privatBil?.infoBilKunDelerAvStrekning?.strekningHvorBilBleBenyttet).isEqualTo("A til B")
        assertThat(reisemåte?.privatBil?.infoBilKunDelerAvStrekning?.antallKilometerKjørt).isEqualTo("42")
        assertThat(reisemåte?.privatBil?.utgifterPrivatBil?.bompenger).isEqualTo("100")
        assertThat(reisemåte?.privatBil?.utgifterPrivatBil?.ferge).isEqualTo("200")
        assertThat(reisemåte?.privatBil?.utgifterPrivatBil?.piggdekkavgift).isEqualTo("30")
        assertThat(reisemåte?.privatBil?.utgifterPrivatBil?.parkering).isEqualTo("40")
        assertThat(reisemåte?.privatBil?.utgifterPrivatBil?.drivstoffType).isEqualTo(DrivstoffType.ELBIL)
        assertThat(reisemåte?.drosje?.harTTKort).isEqualTo(JaNei.NEI)
        assertThat(reisemåte?.unntakFraPrivatBil).containsExactly(ÅrsakKanIkkeBenytteEgenBil.ANNET)
        assertThat(reisemåte?.unntakFraOffentligTransport?.årsaker)
            .containsExactly(ÅrsakKanIkkeBenytteOffentligTransport.LEVERING_HENTING_I_BARNEHAGE)
        assertThat(reisemåte?.unntakFraOffentligTransport?.leveringOgHentingIBarnehage?.gateadresse).isEqualTo("Testgata 3")
        assertThat(reisemåte?.unntakFraOffentligTransport?.leveringOgHentingIBarnehage?.postnummer).isEqualTo("0003")
    }

    @Test
    fun `skal bevare ubesvarte felt uten å filtrere bort reisen`() {
        val data = map(skjema()).data

        assertThat(data.aktivitet.aktiviteter).isNull()
        assertThat(data.aktivitet.annenAktivitet).isNull()
        assertThat(data.aktivitet.lønnetAktivitet).isNull()
        assertThat(data.adresser.måBoBorteHjemmefra).isNull()
        assertThat(data.adresser.periodeMidlertidigBosted).isNull()
        assertThat(data.adresser.adresseOriginaltBosted).isNull()
        assertThat(data.adresser.adresseMidlertidigBosted).isNull()
        assertThat(data.barnOgSærligeBehov.harBarnUnder18SomHarFlyttetMed).isNull()
        assertThat(data.barnOgSærligeBehov.hvilkeBarnFlytterMed).isNull()
        assertThat(data.barnOgSærligeBehov.harBarnHjemmeUnder4Klasse).isNull()
        assertThat(data.barnOgSærligeBehov.harSærligeBehovForFlereHjemreiser).isNull()
        assertThat(data.reiser).hasSize(1)
        assertThat(data.reiser.single().typeReise).isNull()
        assertThat(data.reiser.single().fom).isNull()
        assertThat(data.reiser.single().tom).isNull()
        assertThat(data.reiser.single().reisemåte).isNull()
    }

    private fun map(skjema: SøknadsskjemaROAH) =
        SøknadsskjemaROAHMapper.map(
            mottattTidspunkt = (2 januar 2026).atTime(12, 0),
            språk = Språkkode.NB,
            journalpost = journalpost(journalpostId = "12345"),
            skjema = skjema,
        )

    private fun skjema() =
        SøknadsskjemaROAH(
            hovedytelse = HovedytelseAvsnitt(flereValg(Hovedytelse.AAP), null),
            aktivitet = ROAHAktivitetAvsnitt(null, null, null),
            adresser = ROAHAdresseAvsnitt(null, null, null, null),
            barnOgSærligeBehov = ROAHBarnOgSærligeBehovAvsnitt(null, null, null, null),
            reiser = listOf(ROAHReiseInfo(null, null, null, null)),
            dokumentasjon = emptyList(),
        )

    private fun reisemåte() =
        ReisemåteAvsnitt(
            hvilkeTransportmidlerBleBenyttet = flereValg(*Transportmiddel.entries.toTypedArray()),
            unntakFraOffentligTransport =
                UnntakFraOffentligTransport(
                    årsaker = flereValg(ÅrsakKanIkkeBenytteOffentligTransport.LEVERING_HENTING_I_BARNEHAGE),
                    leveringOgHentingIBarnehage =
                        LeveringOgHentingIBarnehage(
                            gateadresse = VerdiFelt("Testgata 3", "Adresse"),
                            postnummer = VerdiFelt("0003", "Postnummer"),
                        ),
                ),
            unntakFraPrivatBil = flereValg(ÅrsakKanIkkeBenytteEgenBil.ANNET),
            offentligTransport = OffentligTransportInfo(VerdiFelt("1500", "Utgift")),
            privatBil =
                PrivatBilInfo(
                    benyttetEgenBil = felt(JaNei.JA),
                    betalteForReisen = felt(JaNei.NEI),
                    infoBilKunDelerAvStrekning =
                        InfoBilKunDelerAvStrekning(
                            strekningHvorBilBleBenyttet = VerdiFelt("A til B", "Strekning"),
                            antallKilometerKjørt = VerdiFelt("42", "Kilometer"),
                        ),
                    utgifterPrivatBil =
                        UtgifterPrivatBil(
                            bompenger = VerdiFelt("100", "Bom"),
                            ferge = VerdiFelt("200", "Ferge"),
                            piggdekkavgift = VerdiFelt("30", "Piggdekk"),
                            parkering = VerdiFelt("40", "Parkering"),
                            drivstoffType = felt(DrivstoffType.ELBIL),
                        ),
                ),
            drosje = DrosjeInfo(felt(JaNei.NEI)),
        )

    private fun <T> felt(verdi: T) = EnumFelt("Valg", verdi, verdi.toString(), emptyList())

    private fun <T> flereValg(vararg verdier: T) = EnumFlereValgFelt("Valg", verdier.map { VerdiFelt(it, it.toString()) }, emptyList())
}
