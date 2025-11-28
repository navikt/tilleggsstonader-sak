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
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode

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
fun IntegrationTest.gjennomførBehandlingsløp(
    fraJournalpost: Journalpost = defaultJournalpost,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    medAktivitet: (BehandlingId) -> LagreVilkårperiode = ::lagreVilkårperiodeAktivitet,
    medMålgruppe: (BehandlingId) -> LagreVilkårperiode = ::lagreVilkårperiodeMålgruppe,
    medVilkår: List<LagreVilkår> = listOf(lagreDagligReiseDto()),
): BehandlingId {
    val behandlingId = håndterSøknadService.håndterSøknad(fraJournalpost)!!.id

    // Oppretter grunnlagsdata
    val behandling = kall.behandling.hent(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return behandlingId
    }

    gjennomførIngangsvilkårSteg(medAktivitet, medMålgruppe, behandlingId)

    if (tilSteg == StegType.VILKÅR) {
        return behandlingId
    }

    gjennomførVilkårSteg(medVilkår, behandling.id, behandling.stønadstype)

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return behandlingId
    }

    gjennomførBeregningSteg(behandling.id, behandling.stønadstype)

    if (tilSteg == StegType.SIMULERING) {
        return behandlingId
    }

    gjennomførSimuleringSteg(behandlingId)

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return behandlingId
    }

    gjennomførSendTilBeslutterSteg(behandlingId)

    if (tilSteg == StegType.BESLUTTE_VEDTAK) {
        return behandlingId
    }

    gjennomførBeslutteVedtakSteg(behandlingId)

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) {
        return behandlingId
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandlingId
}

fun IntegrationTest.gjennomførRevurderingsløp(
    opprettBehandlingDto: OpprettBehandlingDto,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
    medVilkår: List<LagreVilkår> = emptyList(),
): BehandlingId {
    val behandlingId = kall.behandling.opprettRevurdering(opprettBehandlingDto)

    // Oppretter grunnlagsdata
    kall.behandling.hent(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return behandlingId
    }

    gjennomførIngangsvilkårStegRevurdering(behandlingId)

    if (tilSteg == StegType.VILKÅR) {
        return behandlingId
    }

    val behandling = kall.behandling.hent(behandlingId)

    gjennomførVilkårSteg(medVilkår, behandling.id, behandling.stønadstype)

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return behandlingId
    }

    gjennomførBeregningSteg(behandling.id, behandling.stønadstype)

    if (tilSteg == StegType.SIMULERING) {
        return behandlingId
    }

    gjennomførSimuleringSteg(behandlingId)

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return behandlingId
    }

    gjennomførSendTilBeslutterSteg(behandlingId)

    if (tilSteg == StegType.BESLUTTE_VEDTAK) {
        return behandlingId
    }

    gjennomførBeslutteVedtakSteg(behandlingId)

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) {
        return behandlingId
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandlingId
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

private fun IntegrationTest.gjennomførBeslutteVedtakSteg(behandlingId: BehandlingId) {
    medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
        tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)
        kall.totrinnskontroll.beslutteVedtak(behandlingId, BeslutteVedtakDto(godkjent = true))
    }
    kjørTasksKlareForProsessering()
}

private fun IntegrationTest.gjennomførSendTilBeslutterSteg(behandlingId: BehandlingId) {
    kall.brev.genererPdf(behandlingId, GenererPdfRequest(MINIMALT_BREV))
    kall.totrinnskontroll.sendTilBeslutter(behandlingId)
    kjørTasksKlareForProsessering()
}

private fun IntegrationTest.gjennomførSimuleringSteg(behandlingId: BehandlingId) {
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
) {
    val foreslåtteVedtaksperioder = kall.vedtak.foreslåVedtaksperioder(behandlingId)

    val vedtaksperioder =
        foreslåtteVedtaksperioder.map {
            it.tilVedtaksperiodeDto()
        }
    kall.vedtak.lagreInnvilgelse(
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
    kjørTasksKlareForProsessering()
}

private fun IntegrationTest.gjennomførIngangsvilkårSteg(
    medAktivitet: (BehandlingId) -> LagreVilkårperiode,
    medMålgruppe: (BehandlingId) -> LagreVilkårperiode,
    behandlingId: BehandlingId,
) {
    kall.vilkårperiode.opprett(medAktivitet(behandlingId))
    kall.vilkårperiode.opprett(medMålgruppe(behandlingId))

    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )
    kjørTasksKlareForProsessering()
}

private fun IntegrationTest.gjennomførIngangsvilkårStegRevurdering(behandlingId: BehandlingId) {
    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )
    kjørTasksKlareForProsessering()
}

fun IntegrationTest.gjennomførVilkårSteg(
    medVilkår: List<LagreVilkår>,
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
) {
    medVilkår.forEach {
        if (stønadstype.gjelderDagligReise()) {
            kall.vilkårDagligReise.opprettVilkår(behandlingId, it as LagreDagligReiseDto)
        } else {
            kall.vilkår.opprettVilkår(it as OpprettVilkårDto)
        }
    }

    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.VILKÅR,
        ),
    )
    kjørTasksKlareForProsessering()
}

val defaultJournalpost =
    journalpost(
        journalpostId = "1",
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
        bruker = Bruker("12345678910", BrukerIdType.FNR),
    )

private const val MINIMALT_BREV = """SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR"""
