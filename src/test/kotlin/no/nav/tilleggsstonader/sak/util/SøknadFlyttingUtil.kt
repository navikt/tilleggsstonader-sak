package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.Dokument
import no.nav.tilleggsstonader.kontrakter.søknad.DokumentasjonFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.kontrakter.søknad.Vedleggstype
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttebyrå
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttebyråTilbud
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flytteårsak
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttingDetaljer
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.UtgifterEgenFlytting
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Adresse
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

object SøknadFlyttingUtil {
    fun søknadFlytting(
        ident: String = "11111122222",
        mottattTidspunkt: LocalDateTime = LocalDateTime.of(2026, 2, 1, 12, 0),
        flyttemåte: Flyttemåte = Flyttemåte.FLYTTEBYRÅ,
        medDokumentasjon: Boolean = false,
    ): InnsendtSkjema<SøknadsskjemaFlytting> =
        InnsendtSkjema(
            ident = ident,
            mottattTidspunkt = mottattTidspunkt,
            språk = Språkkode.NB,
            skjema =
                SøknadsskjemaFlytting(
                    hovedytelse =
                        HovedytelseAvsnitt(
                            hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                            arbeidOgOpphold = null,
                        ),
                    flyttingDetaljer =
                        FlyttingDetaljer(
                            årsak = EnumFelt("Hvorfor flytter du?", Flytteårsak.NY_JOBB, "Ny jobb", emptyList()),
                            flyttedato = VerdiFelt("2026-03-01", "Flyttedato"),
                            adresse =
                                Adresse(
                                    land = SelectFelt("Land", "NO", "Norge"),
                                    gateadresse = VerdiFelt(verdi = "Mimes vei 1", label = "Gateadresse"),
                                    postnummer = VerdiFelt(verdi = "5132", label = "Postnummer"),
                                    poststed = VerdiFelt(verdi = "Nyborg", label = "Poststed"),
                                ),
                            flyttemåte = EnumFelt("Hvordan flytter du?", flyttemåte, flyttemåte.name, emptyList()),
                            utgifterDekketPåAnnenMåte =
                                EnumFelt("Får du dekket utgiftene på annen måte?", JaNei.NEI, "Nei", emptyList()),
                            utgifterEgenFlytting =
                                if (flyttemåte == Flyttemåte.FLYTTER_SELV) {
                                    UtgifterEgenFlytting(
                                        avstand = VerdiFelt(BigDecimal("120.25"), "Avstand"),
                                        hengerleie = VerdiFelt(BigDecimal("500.00"), "Hengerleie"),
                                        bom = VerdiFelt(BigDecimal("0.00"), "Bom"),
                                        parkering = VerdiFelt(BigDecimal("25.50"), "Parkering"),
                                        ferje = VerdiFelt(BigDecimal("100.25"), "Ferje"),
                                        annet = null,
                                    )
                                } else {
                                    null
                                },
                            flyttebyrå =
                                if (flyttemåte == Flyttemåte.FLYTTEBYRÅ) {
                                    Flyttebyrå(
                                        tilbud =
                                            listOf(
                                                FlyttebyråTilbud(
                                                    navn = VerdiFelt("Flyttebyrå 1", "Navn"),
                                                    beløp = VerdiFelt(BigDecimal("12000"), "Beløp"),
                                                ),
                                                FlyttebyråTilbud(
                                                    navn = VerdiFelt("Flyttebyrå 2", "Navn"),
                                                    beløp = VerdiFelt(BigDecimal("10500.50"), "Beløp"),
                                                ),
                                            ),
                                    )
                                } else {
                                    null
                                },
                        ),
                    dokumentasjon =
                        if (medDokumentasjon) {
                            (
                                if (flyttemåte == Flyttemåte.FLYTTER_SELV) {
                                    listOf(Vedleggstype.UTGIFTER_EGEN_FLYTTING)
                                } else {
                                    listOf(Vedleggstype.TILBUD_FLYTTEBYRÅ_1, Vedleggstype.TILBUD_FLYTTEBYRÅ_2)
                                }
                            ).mapIndexed { index, type ->
                                DokumentasjonFelt(
                                    type = type,
                                    label = type.tittel,
                                    opplastedeVedlegg =
                                        listOf(Dokument(UUID.fromString("00000000-0000-0000-0000-00000000000${index + 1}"), "vedlegg.pdf")),
                                )
                            }
                        } else {
                            emptyList()
                        },
                ),
        )
}
