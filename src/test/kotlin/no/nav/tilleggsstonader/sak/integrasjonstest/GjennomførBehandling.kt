package no.nav.tilleggsstonader.sak.integrasjonstest

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientMockConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.defaultJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.journalpostSøknadForStønadstype
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilVedtaksperiodeDagligReiseDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.SøknadUtil.barnMedBarnepass
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaLæremidler
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import org.springframework.test.web.servlet.client.RestTestClient
import java.time.LocalDate
import java.util.UUID

/**
 * Gjennomfører en behandling fra journalpost og helt til et gitt steg.
 *
 * Kan bruke [testdataProvider] til å modifisere aktiviteter, målgruppe, vilkår og vedtaksresultatet
 *
 * [stønadstype] Oppretter journalpost med brevkode tilhørende stønadstype som brukes til å opprette fagsak
 * [tilSteg] I hvilket steg behandlingen skal ende opp. Som default blir den ferdigstilt.
 *
 */
fun IntegrationTest.opprettBehandlingOgGjennomførBehandlingsløp(
    stønadstype: Stønadstype,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val journalpostSøknadForStønadstype = journalpostSøknadForStønadstype(stønadstype)
    mockStrukturertSøknadForJournalpost(journalpostSøknadForStønadstype, stønadstype)
    val behandlingId = håndterSøknadService.håndterSøknad(journalpostSøknadForStønadstype)!!.id
    gjennomførBehandlingsløp(
        behandlingId = behandlingId,
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
    )
    return behandlingId
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
    } returns jsonMapper.writeValueAsBytes(søknadForStønadstype(stønadstype))
}

private fun søknadForStønadstype(stønadstype: Stønadstype) =
    when (stønadstype) {
        Stønadstype.BARNETILSYN ->
            søknadskjemaBarnetilsyn(
                barnMedBarnepass = listOf(barnMedBarnepass(ident = PdlClientMockConfig.BARN_FNR)),
            )
        Stønadstype.LÆREMIDLER -> søknadskjemaLæremidler()
        Stønadstype.BOUTGIFTER -> søknadBoutgifter()
        Stønadstype.DAGLIG_REISE_TSO,
        Stønadstype.DAGLIG_REISE_TSR,
        -> søknadDagligReise()
    }

fun IntegrationTest.gjennomførBehandlingsløp(
    behandlingId: BehandlingId,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
) {
    // Oppretter grunnlagsdata
    val behandling = kall.behandling.hent(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    val testdata = BehandlingTestdataDsl.build(testdataProvider)

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return
    }

    gjennomførInngangsvilkårSteg(testdata, behandlingId)

    if (tilSteg == StegType.VILKÅR) {
        return
    }

    if (behandling.stønadstype != Stønadstype.LÆREMIDLER) {
        gjennomførVilkårSteg(testdata, behandling.id, behandling.stønadstype)
    }

    if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) && behandling.stønadstype.gjelderDagligReise()) {
        if (tilSteg == StegType.VEDTAK) {
            return
        }

        gjennomførVedtakSteg(behandling.id, behandling.stønadstype, testdata.vedtak.vedtak)

        if (tilSteg == StegType.KJØRELISTE) {
            return
        }

        gjennomførKjørelisteSteg(behandlingId)

        if (tilSteg == StegType.BEREGNING) {
            return
        }

        gjennomførBeregningStegDagligReise(behandlingId)
    } else {
        if (tilSteg == StegType.BEREGNE_YTELSE) {
            return
        }

        gjennomførBeregningSteg(behandling.id, behandling.stønadstype, testdata.vedtak.vedtak)
    }

    if (tilSteg == StegType.SIMULERING) {
        return
    }

    gjennomførSimuleringSteg(behandlingId)

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return
    }

    gjennomførSendTilBeslutterSteg(behandlingId)

    if (tilSteg == StegType.BESLUTTE_VEDTAK) {
        return
    }

    gjennomførBeslutteVedtakSteg(behandlingId)

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) {
        return
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
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
            ),
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
    )
}

fun IntegrationTest.opprettRevurderingOgGjennomførBehandlingsløp(
    opprettBehandlingDto: OpprettBehandlingDto,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    testdataProvider: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val revurderingId = opprettRevurdering(opprettBehandlingDto)
    gjennomførBehandlingsløp(
        behandlingId = revurderingId,
        tilSteg = tilSteg,
        testdataProvider = testdataProvider,
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

fun IntegrationTest.gjennomførBeslutteVedtakSteg(behandlingId: BehandlingId) {
    medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
        tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)
        kall.totrinnskontroll.beslutteVedtak(behandlingId, BeslutteVedtakDto(godkjent = true))
    }
    kjørTasksKlareForProsessering()
}

fun IntegrationTest.gjennomførSendTilBeslutterSteg(behandlingId: BehandlingId) {
    kall.brev.genererPdf(behandlingId, GenererPdfRequest(MINIMALT_BREV))
    kall.totrinnskontroll.sendTilBeslutter(behandlingId)
    kjørTasksKlareForProsessering()
}

fun IntegrationTest.gjennomførSimuleringSteg(behandlingId: BehandlingId) {
    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.SIMULERING,
        ),
    )
    kjørTasksKlareForProsessering()
}

fun IntegrationTest.gjennomførVedtakSteg(
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
) {
    gjennomførBeregningSteg(behandlingId, stønadstype, opprettVedtak)
}

fun IntegrationTest.gjennomførKjørelisteSteg(behandlingId: BehandlingId) {
    kall.steg.ferdigstill(behandlingId, StegController.FerdigstillStegRequest(StegType.KJØRELISTE))
}

fun IntegrationTest.gjennomførBeregningStegDagligReise(behandlingId: BehandlingId) {
    kall.steg.ferdigstill(behandlingId, StegController.FerdigstillStegRequest(StegType.BEREGNING))
}

fun IntegrationTest.gjennomførBeregningSteg(
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
): RestTestClient.ResponseSpec {
    val foreslåtteVedtaksperioder = kall.vedtak.foreslåVedtaksperioder(behandlingId)

    val vedtaksperioder =
        foreslåtteVedtaksperioder.map {
            it.tilVedtaksperiodeDto()
        }
    return when (opprettVedtak) {
        is OpprettInnvilgelse ->
            kall.vedtak.apiRespons
                .lagreInnvilgelse(
                    stønadstype = stønadstype,
                    behandlingId = behandlingId,
                    innvilgelseDto =
                        when (stønadstype) {
                            Stønadstype.BARNETILSYN -> InnvilgelseTilsynBarnRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.LÆREMIDLER -> InnvilgelseLæremidlerRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.BOUTGIFTER -> InnvilgelseBoutgifterRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.DAGLIG_REISE_TSO ->
                                InnvilgelseDagligReiseRequest(
                                    vedtaksperioder = vedtaksperioder.tilVedtaksperiodeDagligReiseDto(),
                                )
                            Stønadstype.DAGLIG_REISE_TSR ->
                                InnvilgelseDagligReiseRequest(
                                    vedtaksperioder = vedtaksperioder.tilVedtaksperiodeDagligReiseDto(),
                                )
                        },
                )
        is OpprettAvslag -> TODO()
        is OpprettOpphør ->
            kall.vedtak.apiRespons
                .lagreOpphør(
                    stønadstype = stønadstype,
                    behandlingId = behandlingId,
                    opphørDto =
                        when (stønadstype) {
                            Stønadstype.BARNETILSYN ->
                                OpphørTilsynBarnRequest(
                                    årsakerOpphør = opprettVedtak.årsaker,
                                    begrunnelse = opprettVedtak.begrunnelse,
                                    opphørsdato = opprettVedtak.opphørsdato,
                                )
                            Stønadstype.LÆREMIDLER ->
                                OpphørLæremidlerRequest(
                                    årsakerOpphør = opprettVedtak.årsaker,
                                    begrunnelse = opprettVedtak.begrunnelse,
                                    opphørsdato = opprettVedtak.opphørsdato,
                                )
                            Stønadstype.BOUTGIFTER ->
                                OpphørBoutgifterRequest(
                                    årsakerOpphør = opprettVedtak.årsaker,
                                    begrunnelse = opprettVedtak.begrunnelse,
                                    opphørsdato = opprettVedtak.opphørsdato,
                                )
                            Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR ->
                                OpphørDagligReiseRequest(
                                    årsakerOpphør = opprettVedtak.årsaker,
                                    begrunnelse = opprettVedtak.begrunnelse,
                                    opphørsdato = opprettVedtak.opphørsdato,
                                )
                        },
                )
    }
}

sealed interface OpprettVedtak

data object OpprettInnvilgelse : OpprettVedtak

data object OpprettAvslag : OpprettVedtak

data class OpprettOpphør(
    val årsaker: List<ÅrsakOpphør> = listOf(ÅrsakOpphør.ANNET),
    val begrunnelse: String = "annet",
    val opphørsdato: LocalDate,
) : OpprettVedtak

private const val MINIMALT_BREV = """SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR"""

private fun IntegrationTest.gjennomførInngangsvilkårSteg(
    testdataDsl: BehandlingTestdataDsl,
    behandlingId: BehandlingId,
) {
    // Hentes ikke ut om man ikke trenger
    val vilkårperioder: VilkårperioderDto by lazy {
        kall.vilkårperiode.hentForBehandling(behandlingId).vilkårperioder
    }

    // Opprett aktiviteter
    testdataDsl.aktivitet.opprettScope
        .build(behandlingId)
        .forEach { lagreVilkårperiode ->
            kall.vilkårperiode
                .opprett(lagreVilkårperiode)
                .periode!!
                .id
        }

    // Oppretter ålgrupper
    testdataDsl.målgruppe.opprettScope
        .build(behandlingId)
        .forEach { lagreVilkårperiode ->
            kall.vilkårperiode
                .opprett(lagreVilkårperiode)
                .periode!!
                .id
        }

    // Oppdater aktiviteter
    testdataDsl.aktivitet.update.forEach { upd ->
        val (vilkårperiodeId, lagreVilkårperiode) = upd(vilkårperioder.aktiviteter, behandlingId)
        kall.vilkårperiode.oppdater(
            vilkårperiodeId = vilkårperiodeId,
            lagreVilkårperiode = lagreVilkårperiode,
        )
    }

    // Slett aktiviteter
    testdataDsl.aktivitet.delete.forEach { del ->
        val idOgRequest: Pair<UUID, SlettVikårperiode> = del(vilkårperioder.aktiviteter)
        kall.vilkårperiode.apiRespons.slett(
            vilkårperiodeId = idOgRequest.first,
            slettVikårperiode = idOgRequest.second,
        )
    }

    // Oppdater målgruppeer
    testdataDsl.målgruppe.update.forEach { upd ->
        val (vilkårperiodeId, lagreVilkårperiode) = upd(vilkårperioder.målgrupper, behandlingId)
        kall.vilkårperiode.oppdater(
            vilkårperiodeId = vilkårperiodeId,
            lagreVilkårperiode = lagreVilkårperiode,
        )
    }

    // Slett målgruppeer
    testdataDsl.målgruppe.delete.forEach { del ->
        val idOgRequest: Pair<UUID, SlettVikårperiode> = del(vilkårperioder.målgrupper)
        kall.vilkårperiode.apiRespons.slett(
            vilkårperiodeId = idOgRequest.first,
            slettVikårperiode = idOgRequest.second,
        )
    }

    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )
    kjørTasksKlareForProsessering()
}

private fun IntegrationTest.gjennomførVilkårSteg(
    testdataProvider: BehandlingTestdataDsl,
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
) {
    val vilkårDagligReise: List<VilkårDagligReiseDto> by lazy {
        kall.vilkårDagligReise.hentVilkår(behandlingId)
    }

    val vilkår: VilkårsvurderingDto by lazy {
        kall.vilkår.hentVilkår(behandlingId)
    }

    // Trenger kun hente ut om tilsyn-barn
    val barnIder: List<BarnId> =
        stønadstype
            .takeIf { it == Stønadstype.BARNETILSYN }
            ?.let { _ -> barnRepository.findByBehandlingId(behandlingId).map { it.id } }
            ?: emptyList()

    testdataProvider.vilkår.opprettScope.build(behandlingId, barnIder).forEach {
        if (stønadstype.gjelderDagligReise()) {
            kall.vilkårDagligReise.opprettVilkår(behandlingId, it as LagreDagligReiseDto)
        } else {
            kall.vilkår.opprettVilkår(it as OpprettVilkårDto)
        }
    }

    if (stønadstype.gjelderDagligReise()) {
        testdataProvider.vilkår.updateDagligReise
            .map { it(vilkårDagligReise) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.oppdaterVilkår(dto, vilkårId, behandlingId) }

        testdataProvider.vilkår.deleteDagligReise
            .map { it(vilkårDagligReise) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.slettVilkår(behandlingId, vilkårId, dto) }
    } else {
        testdataProvider.vilkår.update
            .map { it(vilkår) }
            .forEach { kall.vilkår.oppdaterVilkår(it) }

        testdataProvider.vilkår.delete
            .map { it(vilkår) }
            .forEach { kall.vilkår.slettVilkår(it) }
    }

    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.VILKÅR,
        ),
    )
    kjørTasksKlareForProsessering()
}
