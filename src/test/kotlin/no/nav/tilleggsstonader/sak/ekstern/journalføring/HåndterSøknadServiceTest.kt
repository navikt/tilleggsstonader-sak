package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.AktivitetDagligReiseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.ReiseAdresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reiseperiode
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.SkjemaDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.HovedytelseAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadReiseTilSamling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.AktivitetReiseTilSamlingAvsnitt
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Avreiseadresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.Reisemåte
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.SamlingPeriode
import no.nav.tilleggsstonader.sak.opplysninger.søknad.reiseTilSamling.SkjemaReiseTilSamling
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.dokumentvariantOriginal
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HåndterSøknadServiceTest {
    private val journalpostService = mockk<JournalpostService>()
    private val søknadService = mockk<SøknadService>()
    private val unleashService = mockk<UnleashService>(relaxed = true)
    private val bestemTemaForJournalpostService = mockk<BestemTemaForJournalpostService>()

    private val service =
        HåndterSøknadService(
            journalpostService = journalpostService,
            taskService = mockk<TaskService>(),
            journalføringService = mockk(),
            søknadService = søknadService,
            unleashService = unleashService,
            bestemTemaForJournalpostService = bestemTemaForJournalpostService,
        )

    private val bruker = Bruker("12345678910", BrukerIdType.FNR)

    @Test
    fun `daglig reise - ustrukturert søknad skal rutes basert på tema uten å hente søknad`() {
        val journalpost = ustrukturertJournalpost(DokumentBrevkode.DAGLIG_REISE, tema = Tema.TSR)

        val resultat = service.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false)

        assertThat(resultat.defaultStønadstype).isEqualTo(Stønadstype.DAGLIG_REISE_TSR)
        verify(exactly = 0) { søknadService.mapSøknad(any(), any()) }
        verify(exactly = 0) { bestemTemaForJournalpostService.bestemStønadstype(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `daglig reise - strukturert søknad skal delegere til BestemTemaForJournalpostService med riktige parametre`() {
        val journalpost = strukturertJournalpost(DokumentBrevkode.DAGLIG_REISE)
        val fom = 1 januar 2026
        val tom = 10 januar 2026
        val søknad =
            søknadDagligReise(
                hovedytelse = listOf(Hovedytelse.AAP),
                reiser = listOf(reise(fom, tom)),
            )

        every { journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.DAGLIG_REISE_TSO) } returns mockk()
        every { søknadService.mapSøknad(any(), journalpost) } returns søknad
        every {
            bestemTemaForJournalpostService.bestemStønadstype(any(), any(), any(), any(), any(), any())
        } returns Stønadstype.DAGLIG_REISE_TSO

        val resultat = service.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false)

        assertThat(resultat.defaultStønadstype).isEqualTo(Stønadstype.DAGLIG_REISE_TSO)
        verify(exactly = 1) {
            bestemTemaForJournalpostService.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.DAGLIG_REISE_TSO,
                stønadstypeTsr = Stønadstype.DAGLIG_REISE_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.AAP),
            )
        }
    }

    @Test
    fun `reise til samling - ustrukturert søknad skal rutes basert på tema uten å hente søknad`() {
        val journalpost = ustrukturertJournalpost(DokumentBrevkode.REISE_TIL_SAMLING, tema = Tema.TSO)

        val resultat = service.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false)

        assertThat(resultat.defaultStønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSO)
        verify(exactly = 0) { søknadService.mapSøknad(any(), any()) }
        verify(exactly = 0) { bestemTemaForJournalpostService.bestemStønadstype(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `reise til samling - strukturert søknad skal delegere til BestemTemaForJournalpostService med riktige parametre`() {
        val journalpost = strukturertJournalpost(DokumentBrevkode.REISE_TIL_SAMLING)
        val fom = 12 januar 2026
        val tom = 20 januar 2026
        val søknad =
            søknadReiseTilSamling(
                hovedytelse = listOf(Hovedytelse.TILTAKSPENGER),
                samlinger = listOf(samlingPeriode(fom, tom)),
            )

        every { journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.REISE_TIL_SAMLING_TSO) } returns mockk()
        every { søknadService.mapSøknad(any(), journalpost) } returns søknad
        every {
            bestemTemaForJournalpostService.bestemStønadstype(any(), any(), any(), any(), any(), any())
        } returns Stønadstype.REISE_TIL_SAMLING_TSR

        val resultat = service.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false)

        assertThat(resultat.defaultStønadstype).isEqualTo(Stønadstype.REISE_TIL_SAMLING_TSR)
        verify(exactly = 1) {
            bestemTemaForJournalpostService.bestemStønadstype(
                journalpost = journalpost,
                stønadstypeTso = Stønadstype.REISE_TIL_SAMLING_TSO,
                stønadstypeTsr = Stønadstype.REISE_TIL_SAMLING_TSR,
                fom = fom,
                tom = tom,
                målgrupperFraSøknad = setOf(MålgruppeType.TILTAKSPENGER),
            )
        }
    }

    @Test
    fun `reise til samling - skal utlede fom og tom som min og maks av flere samlinger`() {
        val journalpost = strukturertJournalpost(DokumentBrevkode.REISE_TIL_SAMLING)
        val tidligsteFom = 1 januar 2026
        val senesteTom = 28 januar 2026
        val søknad =
            søknadReiseTilSamling(
                samlinger =
                    listOf(
                        samlingPeriode(10 januar 2026, 15 januar 2026),
                        samlingPeriode(tidligsteFom, 5 januar 2026),
                        samlingPeriode(20 januar 2026, senesteTom),
                    ),
            )

        every { journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.REISE_TIL_SAMLING_TSO) } returns mockk()
        every { søknadService.mapSøknad(any(), journalpost) } returns søknad
        val fomSlot = slot<LocalDate>()
        val tomSlot = slot<LocalDate>()
        every {
            bestemTemaForJournalpostService.bestemStønadstype(any(), any(), any(), capture(fomSlot), capture(tomSlot), any())
        } returns Stønadstype.REISE_TIL_SAMLING_TSO

        service.finnStønadstyperSomKanOpprettesFraJournalpost(journalpost, filtrerStønadstyperSomIkkeErAktivert = false)

        assertThat(fomSlot.captured).isEqualTo(tidligsteFom)
        assertThat(tomSlot.captured).isEqualTo(senesteTom)
    }

    private fun ustrukturertJournalpost(
        brevkode: DokumentBrevkode,
        tema: Tema,
    ) = journalpost(
        tema = tema.name,
        dokumenter = listOf(dokumentInfo(brevkode = brevkode.verdi, dokumentvarianter = listOf(dokumentvariant()))),
        bruker = bruker,
    )

    private fun strukturertJournalpost(brevkode: DokumentBrevkode) =
        journalpost(
            tema = Tema.TSO.name,
            dokumenter = listOf(dokumentInfo(brevkode = brevkode.verdi, dokumentvarianter = listOf(dokumentvariantOriginal()))),
            bruker = bruker,
        )

    private fun søknadDagligReise(
        hovedytelse: List<Hovedytelse>,
        reiser: List<Reise>,
    ) = SøknadDagligReise(
        journalpostId = "1",
        mottattTidspunkt = java.time.LocalDateTime.now(),
        språk = no.nav.tilleggsstonader.kontrakter.felles.Språkkode.NB,
        data =
            SkjemaDagligReise(
                personopplysninger = Personopplysninger(adresse = null),
                hovedytelse = HovedytelseAvsnitt(hovedytelse = hovedytelse, harNedsattArbeidsevne = null, arbeidOgOpphold = null),
                aktivitet =
                    AktivitetDagligReiseAvsnitt(
                        aktiviteter = null,
                        dekkesUtgiftenAvAndre = null,
                        annenAktivitet = null,
                    ),
                reiser = reiser,
                dokumentasjon = emptyList(),
            ),
    )

    private fun reise(
        fom: LocalDate,
        tom: LocalDate,
    ) = Reise(
        skalReiseFraFolkeregistrertAdresse = null,
        adresseDetSkalReisesFra = null,
        adresse = ReiseAdresse(gateadresse = null, postnummer = null, poststed = null),
        periode = Reiseperiode(fom = fom, tom = tom),
        dagerPerUke = "5",
        harMerEnn6KmReisevei = JaNei.JA,
        lengdeReisevei = 10.0,
        harBehovForTransportUavhengigAvReisensLengde = null,
        leveringOgHentingIBarnehage = null,
        kanReiseMedOffentligTransport = JaNei.JA,
        offentligTransport = null,
        privatTransport = null,
        skalDuBetaleForReisenSelv = null,
    )

    private fun søknadReiseTilSamling(
        hovedytelse: List<Hovedytelse> = listOf(Hovedytelse.AAP),
        samlinger: List<SamlingPeriode>,
    ) = SøknadReiseTilSamling(
        journalpostId = "1",
        mottattTidspunkt = java.time.LocalDateTime.now(),
        språk = no.nav.tilleggsstonader.kontrakter.felles.Språkkode.NB,
        data =
            SkjemaReiseTilSamling(
                hovedytelse = HovedytelseAvsnitt(hovedytelse = hovedytelse, harNedsattArbeidsevne = null, arbeidOgOpphold = null),
                aktivitet =
                    AktivitetReiseTilSamlingAvsnitt(
                        aktiviteter = null,
                        annenAktivitet = null,
                        lønnetAktivitet = null,
                        tilleggsopplysningerAnnenAktivitet = null,
                        annenAktivitetTypeUtdanning = null,
                    ),
                samlinger = samlinger,
                avreiseadresse = Avreiseadresse(skalReiseFraFolkeregistrertAdresse = JaNei.JA, adresseDetSkalReisesFra = null),
                reisemåte =
                    Reisemåte(
                        kanReiseMedOffentligTransport = JaNei.JA,
                        kanIkkeReiseMedOffentligTransportBegrunnelser = null,
                        totalUtgifterOffentligTransport = null,
                        kanBenytteEgenBil = null,
                        ønskerDekketUtgifterForDrosje = null,
                        barnehageGateadresse = null,
                        barnehagePostnummer = null,
                        kanIkkeBenytteEgenBilBegrunnelser = null,
                        betalerForReiseSelv = null,
                        harTTKort = null,
                        reiseMedBilUtgifter = null,
                    ),
                dokumentasjon = emptyList(),
            ),
    )

    private fun samlingPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ) = SamlingPeriode(
        fom = fom,
        tom = tom,
        erObligatorisk = JaNei.JA,
        harBruktEkstraReiseDager = JaNei.NEI,
        adresse = Adresse(gyldigFraOgMed = null, adresse = null, postnummer = null, poststed = null, landkode = null),
        antallKilometerEnVei = "10",
    )
}
