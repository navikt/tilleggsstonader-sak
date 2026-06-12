package no.nav.tilleggsstonader.sak.integrasjonstest

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandling.opprettelse.ForenkletBehandlingstype
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.defaultJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.journalpostSøknadForStønadstype
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.SøknadUtil.barnMedBarnepass
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReisePrivatBilDto
import java.time.LocalDate

data class BehandlingContext(
    val behandlingId: BehandlingId,
    val fagsakId: FagsakId,
    val ident: String,
)

/**
 * Gjennomfører en behandling fra journalpost og helt til et gitt steg.
 *
 * Kan bruke [testdataProvider] til å modifisere aktiviteter, målgruppe, vilkår og vedtaksresultatet
 *
 * [stønadstype] Oppretter journalpost med brevkode tilhørende stønadstype som brukes til å opprette fagsak
 * [tilSteg] I hvilket steg behandlingen skal ende opp. Som default blir den ferdigstilt.
 * [ident] Bruker-ident på journalposten. Sak skal opprettes på denne brukeren
 *
 */
fun IntegrationTest.opprettBehandlingOgGjennomførBehandlingsløp(
    stønadstype: Stønadstype,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    ident: String = FnrGenerator.generer(),
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
): BehandlingContext {
    val journalpostSøknadForStønadstype = journalpostSøknadForStønadstype(stønadstype, ident)
    mockStrukturertSøknadForJournalpost(journalpostSøknadForStønadstype, stønadstype)
    val behandling = håndterSøknadService.håndterSøknad(journalpostSøknadForStønadstype)!!
    gjennomførBehandlingsløp(
        behandlingId = behandling.id,
        ident = ident,
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
    )

    return BehandlingContext(
        behandlingId = behandling.id,
        fagsakId = behandling.fagsakId,
        ident = ident,
    )
}

private fun IntegrationTest.mockStrukturertSøknadForJournalpost(
    journalpost: Journalpost,
    stønadstype: Stønadstype,
) {
    if (stønadstype == Stønadstype.DAGLIG_REISE_TSO) {
        // Samme søknad for TSO og TSR, rutes ved sjekk på ytelser i HåndterSøknadService
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()
    }
    every {
        journalpostClient.hentDokument(
            journalpost.journalpostId,
            journalpost.dokumenter?.single()!!.dokumentInfoId,
            Dokumentvariantformat.ORIGINAL,
        )
    } returns jsonMapper.writeValueAsBytes(søknadForStønadstype(stønadstype, ident = journalpost.bruker!!.id))
}

private fun søknadForStønadstype(
    stønadstype: Stønadstype,
    ident: String,
) = when (stønadstype) {
    Stønadstype.BARNETILSYN ->
        søknadskjemaBarnetilsyn(
            ident = ident,
            barnMedBarnepass = listOf(barnMedBarnepass(ident = PdlClientMockConfig.BARN_FNR)),
        )

    Stønadstype.LÆREMIDLER -> søknadskjemaLæremidler(ident = ident)
    Stønadstype.BOUTGIFTER -> søknadBoutgifter(ident = ident)
    Stønadstype.DAGLIG_REISE_TSO,
    Stønadstype.DAGLIG_REISE_TSR,
    -> søknadDagligReise(ident = ident)

    Stønadstype.REISE_TIL_SAMLING_TSO -> TODO("lage søgnadskjema for reise til samling")
}

fun IntegrationTest.gjennomførBehandlingsløp(
    behandlingId: BehandlingId,
    ident: String,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    erRevurderingDagligReiseMedPrivatBil: Boolean = false,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
) {
    // Oppretter grunnlagsdata
    val behandling = kall.behandling.hent(behandlingId)

    // Oppretter vilkårperiode grunnlag
    kall.vilkårperiode.hentForBehandling(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    val testdata = BehandlingTestdataDsl.build(testdataProvider)

    var nåværendeSteg = behandling.steg
    while (nåværendeSteg != tilSteg) {
        nåværendeSteg = utførStegOgReturnerNesteSteg(nåværendeSteg, behandling, testdata)
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    val reiserMedPrivatBil by lazy {
        kall.vilkårDagligReise.hentVilkår(behandlingId).filter { it.fakta is FaktaDagligReisePrivatBilDto }
    }

    testdata.kjørelisterTilInnsending.forEach {
        val kjøreliste = it.build(reiserMedPrivatBil)
        sendInnKjøreliste(kjøreliste, ident)
    }
}

fun IntegrationTest.utførStegOgReturnerNesteSteg(
    steg: StegType,
    behandling: BehandlingDto,
    testdata: BehandlingTestdataDsl,
): StegType =
    when (steg) {
        StegType.INNGANGSVILKÅR -> gjennomførInngangsvilkårSteg(testdata, behandling.id)
        StegType.VILKÅR -> gjennomførVilkårSteg(testdata, behandling.id, behandling.stønadstype)
        StegType.BEREGNE_YTELSE -> gjennomførBeregningSteg(behandling.id, behandling.stønadstype, testdata.vedtak.vedtak)
        StegType.KJØRELISTE -> gjennomførKjørelisteSteg(behandling.id)
        StegType.BEREGNING -> gjennomførBeregningStegDagligReise(behandling.id)
        StegType.SIMULERING -> gjennomførSimuleringSteg(behandling.id)
        StegType.SEND_TIL_BESLUTTER -> gjennomførSendTilBeslutterSteg(behandling.id)
        StegType.BESLUTTE_VEDTAK -> gjennomførBeslutteVedtakSteg(behandling.id)
        StegType.FULLFØR_KJØRELISTE,
        StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV,
        StegType.FERDIGSTILLE_BEHANDLING,
        StegType.BEHANDLING_FERDIGSTILT,
        -> error("$steg kan ikke gjennomføres gjennom api-kall")
    }

fun IntegrationTest.opprettRevurdering(opprettBehandlingDto: OpprettBehandlingDto): BehandlingId {
    val behandlingId = kall.behandling.opprettRevurdering(opprettBehandlingDto)

    // Oppretter grunnlagsdata
    kall.behandling.hent(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    return behandlingId
}

fun IntegrationTest.opprettRevurderingOgGjennomførBehandlingsløp(
    fraBehandlingId: BehandlingId,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    erRevurderingDagligReiseMedPrivatBil: Boolean = false,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val behandling = kall.behandling.hent(fraBehandlingId)
    return opprettRevurderingOgGjennomførBehandlingsløp(
        opprettBehandlingDto =
            OpprettBehandlingDto(
                fagsakId = behandling.fagsakId,
                årsak = BehandlingÅrsak.SØKNAD,
                kravMottatt = LocalDate.now(),
                nyeOpplysningerMetadata = null,
                forenkletBehandlingstype = ForenkletBehandlingstype.ORDINAER_BEHANDLING,
            ),
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
        erRevurderingDagligReiseMedPrivatBil = erRevurderingDagligReiseMedPrivatBil,
    )
}

fun IntegrationTest.opprettRevurderingOgGjennomførBehandlingsløp(
    opprettBehandlingDto: OpprettBehandlingDto,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    erRevurderingDagligReiseMedPrivatBil: Boolean = false,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val revurderingId = opprettRevurdering(opprettBehandlingDto)
    gjennomførBehandlingsløp(
        behandlingId = revurderingId,
        ident = testoppsettService.hentPersonidentForBehandlingId(revurderingId),
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
        erRevurderingDagligReiseMedPrivatBil = erRevurderingDagligReiseMedPrivatBil,
    )
    return revurderingId
}

fun IntegrationTest.gjennomførHenleggelse(fraJournalpost: Journalpost = defaultJournalpost): BehandlingId {
    val behandlingId = håndterSøknadService.håndterSøknad(fraJournalpost)!!.id

    // Oppretter grunnlagsdata
    kall.behandling.hent(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    kall.behandling.henlegg(
        behandlingId,
        HenlagtDto(
            årsak = HenlagtÅrsak.FEILREGISTRERT,
            begrunnelse = "begrunnelse for henleggelse",
        ),
    )
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandlingId
}

sealed interface OpprettVedtak

data class OpprettInnvilgelse(
    val vedtaksperioder: List<VedtaksperiodeDto>? = null,
) : OpprettVedtak

data object OpprettAvslag : OpprettVedtak

data class OpprettOpphør(
    val årsaker: List<ÅrsakOpphør> = listOf(ÅrsakOpphør.ANNET),
    val begrunnelse: String = "annet",
    val opphørsdato: LocalDate,
) : OpprettVedtak
