package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFelt
import no.nav.tilleggsstonader.kontrakter.søknad.EnumFlereValgFelt
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.SelectFelt
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ArbeidOgOpphold
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.felles.TypePengestøtte
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ÅrsakOppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.AnnenUtdanningType
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.HarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.kontrakter.søknad.læremidler.UtdanningAvsnitt
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OpprettDummySøknadLæremidler(
    private val søknadService: SøknadService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val skjemaLæremidler =
            SøknadsskjemaLæremidler(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                        arbeidOgOpphold = arbeidOgOpphold(),
                    ),
                utdanning =
                    UtdanningAvsnitt(
                        aktiviteter =
                            EnumFlereValgFelt(
                                "Hvilken utdanning eller opplæring søker du om støtte til læremidler for",
                                listOf(
                                    VerdiFelt("1", "Høyere utdanning: 25. februar 2024 - 25. juli 2024"),
                                ),
                                listOf("Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                            ),
                        annenUtdanning =
                            EnumFelt(
                                "Annen utdanning tekst",
                                AnnenUtdanningType.INGEN_UTDANNING,
                                "Ja",
                                emptyList(),
                            ),
                        harRettTilUtstyrsstipend =
                            HarRettTilUtstyrsstipend(
                                erLærlingEllerLiknende =
                                    EnumFelt(
                                        "Er lærling eller liknende?",
                                        JaNei.JA,
                                        "Ja",
                                        emptyList(),
                                    ),
                                harTidligereFullførtVgs =
                                    EnumFelt(
                                        "Har du tidligere fullført videregående skole?",
                                        JaNei.JA,
                                        "Ja",
                                        emptyList(),
                                    ),
                            ),
                        harFunksjonsnedsettelse = EnumFelt("Har funksjonsnedsettelse?", JaNei.JA, "Ja", emptyList()),
                    ),
                dokumentasjon = emptyList(),
            )
        val skjema =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.of(2020, 1, 1, 0, 0),
                språk = Språkkode.NB,
                skjema = skjemaLæremidler,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        søknadService.lagreSøknad(behandling.id, journalpost, skjema)
    }
}

private fun arbeidOgOpphold() =
    ArbeidOgOpphold(
        jobberIAnnetLand = EnumFelt("Jobber du i et annet land enn Norge?", JaNei.JA, "Ja", emptyList()),
        jobbAnnetLand = SelectFelt("Hvilket land jobber du i?", "SWE", "Sverige"),
        harPengestøtteAnnetLand =
            EnumFlereValgFelt(
                "Mottar du pengestøtte fra et annet land enn Norge?",
                listOf(
                    VerdiFelt(
                        TypePengestøtte.SYKEPENGER,
                        "Sykepenger",
                    ),
                ),
                emptyList(),
            ),
        pengestøtteAnnetLand = SelectFelt("Hvilket land mottar du pengestøtte fra?", "SWE", "Sverige"),
        harOppholdUtenforNorgeSiste12mnd =
            EnumFelt(
                "Jobber du i et annet land enn Norge?",
                JaNei.JA,
                "Ja",
                emptyList(),
            ),
        oppholdUtenforNorgeSiste12mnd = listOf(oppholdUtenforNorge()),
        harOppholdUtenforNorgeNeste12mnd =
            EnumFelt(
                "Jobber du i et annet land enn Norge?",
                JaNei.JA,
                "Ja",
                emptyList(),
            ),
        oppholdUtenforNorgeNeste12mnd = listOf(oppholdUtenforNorge()),
    )

private fun oppholdUtenforNorge() =
    OppholdUtenforNorge(
        land = SelectFelt("Hvilket land har du oppholdt deg i?", "SWE", "Sverige"),
        årsak =
            EnumFlereValgFelt(
                "Hva gjorde du i dette landet?",
                listOf(VerdiFelt(ÅrsakOppholdUtenforNorge.JOBB, "Jobb")),
                alternativer = emptyList(),
            ),
        fom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
        tom = DatoFelt("Fom", LocalDate.of(2024, 1, 1)),
    )
