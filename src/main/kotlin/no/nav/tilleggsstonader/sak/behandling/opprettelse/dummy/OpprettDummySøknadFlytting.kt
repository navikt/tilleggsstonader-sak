package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaFlytting
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttebyråAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttebyråTilbud
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flytteårsak
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FlyttingDetaljerAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.FraAdresseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.TilAdresseAvsnitt
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OpprettDummySøknadFlytting(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val skjemaFlytting =
            SøknadsskjemaFlytting(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                        arbeidOgOpphold = null,
                    ),
                flyttingDetaljer =
                    FlyttingDetaljerAvsnitt(
                        årsak = EnumFelt("Hvorfor skal du flytte?", Flytteårsak.NY_JOBB, "Jeg har fått ny jobb", emptyList()),
                        flyttedato = VerdiFelt(LocalDate.now().plusWeeks(2).toString(), "Når skal du flytte?"),
                        adresse =
                            TilAdresseAvsnitt(
                                land = SelectFelt("Land", "NO", "Norge"),
                                gateadresse = VerdiFelt("Flyttegata 1", "Gateadresse"),
                                postnummer = VerdiFelt("0150", "Postnummer"),
                                poststed = VerdiFelt("Oslo", "Poststed"),
                            ),
                        skalFlytteFraFolkeregistrertAdresse =
                            EnumFelt("Flytter du fra din folkeregistrerte adresse?", JaNei.NEI, "Nei", emptyList()),
                        adresseDetSkalFlyttesFra =
                            FraAdresseAvsnitt(
                                land = SelectFelt("Land", "NO", "Norge"),
                                gateadresse = VerdiFelt("Gamlegata 2", "Gateadresse"),
                                postnummer = VerdiFelt("5003", "Postnummer"),
                                poststed = VerdiFelt("Bergen", "Poststed"),
                            ),
                        flyttemåte =
                            EnumFelt("Hvordan skal du flytte?", Flyttemåte.FLYTTEBYRÅ, "Med flyttebyrå", emptyList()),
                        utgifterDekketPåAnnenMåte =
                            EnumFelt("Får du dekket utgiftene på annen måte?", JaNei.NEI, "Nei", emptyList()),
                        utgifterEgenFlytting = null,
                        flyttebyrå =
                            FlyttebyråAvsnitt(
                                tilbud =
                                    listOf(
                                        FlyttebyråTilbud(
                                            navn = VerdiFelt("Flyttebyrå 1", "Navn på flyttebyrå"),
                                            beløp = VerdiFelt(BigDecimal("15000"), "Pris"),
                                        ),
                                        FlyttebyråTilbud(
                                            navn = VerdiFelt("Flyttebyrå 2", "Navn på flyttebyrå"),
                                            beløp = VerdiFelt(BigDecimal("12500"), "Pris"),
                                        ),
                                    ),
                            ),
                    ),
                dokumentasjon = emptyList(),
            )
        val skjema =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjemaFlytting,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjema)
    }
}
