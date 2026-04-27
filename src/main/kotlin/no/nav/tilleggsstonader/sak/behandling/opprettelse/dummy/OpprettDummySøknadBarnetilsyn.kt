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
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.TekstFelt
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AktivitetAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.AnnenAktivitetType
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnAvsnitt
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.felles.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpprettDummySøknadBarnetilsyn(
    private val personService: PersonService,
    private val søknadService: SøknadService,
    private val barnService: BarnService,
) {
    fun opprettDummy(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        val pdlBarn = personService.hentPersonMedBarn(fagsak.hentAktivIdent()).barn
        val barnMedBarnepass =
            pdlBarn.entries.map { (ident, _) ->
                BarnMedBarnepass(
                    ident = TekstFelt("", ident),
                    navn = TekstFelt("", "navn"),
                    type = EnumFelt("", TypeBarnepass.BARNEHAGE_SFO_AKS, "", emptyList()),
                    startetIFemte = null,
                    årsak = null,
                )
            }
        val skjemaBarnetilsyn =
            SøknadsskjemaBarnetilsyn(
                hovedytelse =
                    HovedytelseAvsnitt(
                        hovedytelse = EnumFlereValgFelt("", listOf(VerdiFelt(Hovedytelse.AAP, "AAP")), emptyList()),
                        arbeidOgOpphold = null,
                    ),
                aktivitet =
                    AktivitetAvsnitt(
                        aktiviteter =
                            EnumFlereValgFelt(
                                "Hvilken aktivitet søker du om støtte i forbindelse med?",
                                listOf(
                                    VerdiFelt("ANNET", "Annet"),
                                    VerdiFelt("1", "Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                                ),
                                listOf("Arbeidstrening: 25. februar 2024 - 25. juli 2024"),
                            ),
                        annenAktivitet =
                            EnumFelt(
                                "Hvilken arbeidsrettet aktivitet har du? ",
                                AnnenAktivitetType.TILTAK,
                                "Tiltak / arbeidsrettet aktivitet",
                                listOf(),
                            ),
                        lønnetAktivitet = EnumFelt("Mottar du lønn gjennom ett tiltak?", JaNei.NEI, "Nei", listOf()),
                    ),
                barn =
                    BarnAvsnitt(
                        barnMedBarnepass = barnMedBarnepass,
                    ),
                dokumentasjon = emptyList(),
            )
        val skjema =
            InnsendtSkjema(
                ident = fagsak.hentAktivIdent(),
                mottattTidspunkt = LocalDateTime.now(),
                språk = Språkkode.NB,
                skjema = skjemaBarnetilsyn,
            )
        val journalpost = Journalpost("TESTJPID", Journalposttype.I, Journalstatus.FERDIGSTILT)
        val søknad = søknadService.lagreSøknad(behandling.id, journalpost, skjema)
        opprettBarn(behandling, søknad as SøknadBarnetilsyn)
    }

    // Oppretter BehandlingBarn for alle barn fra PDL for å få et vilkår per barn
    private fun opprettBarn(
        behandling: Behandling,
        søknad: SøknadBarnetilsyn,
    ) {
        val behandlingBarn =
            søknad.barn.map { barn ->
                BehandlingBarn(
                    behandlingId = behandling.id,
                    ident = barn.ident,
                )
            }
        barnService.opprettBarn(behandlingBarn)
    }
}
