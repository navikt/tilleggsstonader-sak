package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttebyrå
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttebyråTilbud
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flytteårsak
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttingDetaljer
import no.nav.tilleggsstonader.kontrakter.søknad.reisetilsamling.Adresse
import java.math.BigDecimal
import java.time.LocalDateTime

object SøknadFlyttingUtil {
    fun søknadFlytting(
        ident: String = "11111122222",
        mottattTidspunkt: LocalDateTime = LocalDateTime.of(2026, 2, 1, 12, 0),
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
                            flyttemåte = EnumFelt("Hvordan flytter du?", Flyttemåte.FLYTTEBYRÅ, "Flyttebyrå", emptyList()),
                            utgifterDekketPåAnnenMåte =
                                EnumFelt("Får du dekket utgiftene på annen måte?", JaNei.NEI, "Nei", emptyList()),
                            utgifterEgenFlytting = null,
                            flyttebyrå =
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
                                ),
                        ),
                    dokumentasjon = emptyList(),
                ),
        )
}
