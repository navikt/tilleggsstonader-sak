package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import java.util.UUID

/**
 * Gjennomfører en behandling fra journalpost og helt til et gitt steg.
 *
 * Foreløpig funker bare innvilgelse.
 *
 * Du kan sende inn ønsket aktivitet, målgruppe og vilkår. Som default er disse satt til et happy-case for daglig reise TSO.
 *
 * [fraJournalpost] Journalposten som behandlingen skal opprettes på bakgrunn av. Bestemmer hvilken stønadstype det blir.
 * [tilSteg] I hvilket steg behandlingen skal ende opp. Som default blir den ferdigstilt.
 *
 */
fun IntegrationTest.opprettBehandlingOgGjennomførBehandlingsløp(
    fraJournalpost: Journalpost = defaultJournalpost,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    medAktivitet: (BehandlingId) -> LagreVilkårperiode = ::lagreVilkårperiodeAktivitet,
    medMålgruppe: (BehandlingId) -> LagreVilkårperiode = ::lagreVilkårperiodeMålgruppe,
    medVilkår: List<LagreVilkår> = listOf(lagreDagligReiseDto()),
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
): BehandlingId {
    val behandlingId = håndterSøknadService.håndterSøknad(fraJournalpost)!!.id
    gjennomførBehandlingsløp(
        behandlingId = behandlingId,
        tilSteg = tilSteg,
        testdataProvider = defaultBehandlingsløpEditor(medAktivitet, medMålgruppe, medVilkår),
        opprettVedtak = opprettVedtak,
    )
    return behandlingId
}

fun IntegrationTest.opprettBehandlingOgGjennomførBehandlingsløp(
    fraJournalpost: Journalpost = defaultJournalpost,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
    edit: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val behandlingId = håndterSøknadService.håndterSøknad(fraJournalpost)!!.id
    gjennomførBehandlingsløp(
        behandlingId = behandlingId,
        tilSteg = tilSteg,
        testdataProvider = edit,
        opprettVedtak = opprettVedtak,
    )
    return behandlingId
}

fun IntegrationTest.gjennomførBehandlingsløp(
    behandlingId: BehandlingId,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
    testdataProvider: BehandlingTestdataDsl.() -> Unit =
        defaultBehandlingsløpEditor(
            medAktivitet = ::lagreVilkårperiodeAktivitet,
            medMålgruppe = ::lagreVilkårperiodeMålgruppe,
            medVilkår = listOf(lagreDagligReiseDto()),
        ),
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

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return
    }

    gjennomførBeregningSteg(behandling.id, behandling.stønadstype, opprettVedtak)

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
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
    edit: BehandlingTestdataDsl.() -> Unit,
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
        opprettVedtak = opprettVedtak,
        edit = edit,
    )
}

fun IntegrationTest.opprettRevurderingOgGjennomførBehandlingsløp(
    opprettBehandlingDto: OpprettBehandlingDto,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
    edit: BehandlingTestdataDsl.() -> Unit,
): BehandlingId {
    val revurderingId = opprettRevurdering(opprettBehandlingDto)
    gjennomførBehandlingsløp(
        behandlingId = revurderingId,
        tilSteg = tilSteg,
        opprettVedtak = opprettVedtak,
        testdataProvider = edit,
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

fun IntegrationTest.gjennomførBeregningSteg(
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse,
): WebTestClient.ResponseSpec {
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
                            Stønadstype.DAGLIG_REISE_TSO -> InnvilgelseDagligReiseRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.DAGLIG_REISE_TSR -> InnvilgelseDagligReiseRequest(vedtaksperioder = vedtaksperioder)
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

@Suppress("unused")
@Deprecated(
    message = "Use gjennomførBehandlingsløp(edit = { ... }) instead",
    replaceWith =
        ReplaceWith(
            "gjennomførBehandlingsløp(behandlingId = behandlingId, tilSteg = StegType.INNGANGSVILKÅR, opprettVedtak = OpprettInnvilgelse)",
        ),
    level = DeprecationLevel.WARNING,
)
fun IntegrationTest.gjennomførInngangsvilkårSteg(
    medAktivitet: ((BehandlingId) -> LagreVilkårperiode)? = null,
    medMålgruppe: ((BehandlingId) -> LagreVilkårperiode)? = null,
    behandlingId: BehandlingId,
) {
    val testdataDsl =
        BehandlingTestdataDsl.build {
            medAktivitet?.let { a -> aktivitet { opprett { add(a) } } }
            medMålgruppe?.let { m -> målgruppe { opprett { add(m) } } }
        }
    gjennomførInngangsvilkårSteg(testdataDsl, behandlingId)
}

@Suppress("unused")
@Deprecated(
    message = "Use gjennomførBehandlingsløp(edit = { ... }) instead",
    replaceWith =
        ReplaceWith(
            "gjennomførBehandlingsløp(behandlingId = behandlingId, tilSteg = StegType.VILKÅR, opprettVedtak = OpprettInnvilgelse)",
        ),
    level = DeprecationLevel.WARNING,
)
fun IntegrationTest.gjennomførVilkårSteg(
    medVilkår: List<LagreVilkår>,
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
) {
    val editor =
        BehandlingTestdataDsl.build {
            vilkår {
                opprett {
                    medVilkår.forEach { add(it) }
                }
            }
        }
    gjennomførVilkårSteg(editor, behandlingId, stønadstype)
}

val defaultJournalpost =
    journalpost(
        journalpostId = "1",
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
        bruker = Bruker("12345678910", BrukerIdType.FNR),
    )

private const val MINIMALT_BREV = """SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR"""

/**
 * Plan for mutating inngangsvilkår-data (aktivitet/målgruppe) and vilkår in integration tests.
 *
 * This is intentionally "test-DSL" level and uses the existing endpoints via `kall.*`.
 */

private fun defaultBehandlingsløpEditor(
    medAktivitet: (BehandlingId) -> LagreVilkårperiode,
    medMålgruppe: (BehandlingId) -> LagreVilkårperiode,
    medVilkår: List<LagreVilkår>,
): BehandlingTestdataDsl.() -> Unit =
    {
        aktivitet {
            opprett {
                add(medAktivitet)
            }
        }
        målgruppe {
            opprett {
                add(medMålgruppe)
            }
        }
        vilkår {
            opprett {
                medVilkår.forEach { add(it) }
            }
        }
    }

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
        val (vilkårperiodeId, lagreVilkårperiode) = upd(vilkårperioder.aktiviteter)
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
        val (vilkårperiodeId, lagreVilkårperiode) = upd(vilkårperioder.målgrupper)
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
    editor: BehandlingTestdataDsl,
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
) {
    val vilkårDagligReise: List<VilkårDagligReiseDto> by lazy {
        kall.vilkårDagligReise.hentVilkår(behandlingId)
    }

    val vilkår: VilkårsvurderingDto by lazy {
        kall.vilkår.hentVilkår(behandlingId)
    }

    editor.vilkår.opprettScope.build().forEach {
        if (stønadstype.gjelderDagligReise()) {
            kall.vilkårDagligReise.opprettVilkår(behandlingId, it as LagreDagligReiseDto)
        } else {
            kall.vilkår.opprettVilkår(it as OpprettVilkårDto)
        }
    }

    if (stønadstype.gjelderDagligReise()) {
        editor.vilkår.updateDagligReise
            .map { it(vilkårDagligReise) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.oppdaterVilkår(dto, vilkårId, behandlingId) }

        editor.vilkår.deleteDagligReise
            .map { it(vilkårDagligReise) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.slettVilkår(behandlingId, vilkårId, dto) }
    } else {
        editor.vilkår.update
            .map { it(vilkår) }
            .forEach { kall.vilkår.oppdaterVilkår(it) }

        editor.vilkår.delete
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
