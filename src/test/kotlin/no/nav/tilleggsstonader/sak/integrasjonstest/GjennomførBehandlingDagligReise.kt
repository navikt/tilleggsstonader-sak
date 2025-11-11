package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

val defaultIdent = "12345678910"
val defaultJournalpostId = "1"
val defaultJournalpost =
    journalpost(
        journalpostId = defaultJournalpostId,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
        bruker = Bruker(defaultIdent, BrukerIdType.FNR),
    )

val minimaltBrev = """SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR"""

fun IntegrationTest.gjennomførInnvilgelseDagligReise(
    fraJournalpost: Journalpost = defaultJournalpost,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
): BehandlingId {
    val behandling = håndterSøknadService.håndterSøknad(fraJournalpost)!!

    kall.behandling.hentBehandling(behandling.id)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return behandling.id
    }

    // Gjennomfører steg: Inngangsvilkår
    kall.vilkårperiode.opprett(lagreVilkårperiodeMålgruppe(behandling.id))
    kall.vilkårperiode.opprett(lagreVilkårperiodeAktivitet(behandling.id))

    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )

    if (tilSteg == StegType.VILKÅR) {
        return behandling.id
    }

    // Gjennomfører steg: Vilkår
    kall.vilkår.opprettDagligReise(lagreDagligReiseDto(), behandling.id)

    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.VILKÅR,
        ),
    )

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return behandling.id
    }

    // Gjennomfører steg: Beregn ytelse
    kall.vedtak.dagligReise.lagreInnvilgelseResponse(
        behandlingId = behandling.id,
        innvilgelseDto =
            InnvilgelseDagligReiseRequest(
                vedtaksperioder = listOf(vedtaksperiode().tilDto()),
            ),
    )

    if (tilSteg == StegType.SIMULERING) {
        return behandling.id
    }

    // Gjennomfører steg: Simulering
    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.SIMULERING,
        ),
    )

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return behandling.id
    }

    // Gjennomfører steg: Send til beslutter
    kall.brev.brev(behandling.id, GenererPdfRequest(minimaltBrev))
    kall.totrinnskontroll.sendTilBeslutter(behandling.id)

    if (tilSteg == StegType.BESLUTTE_VEDTAK) {
        return behandling.id
    }

    // Gjennomfører steg: Beslutte vedtak
    medBrukercontext(bruker = "nissemor", rolle = rolleConfig.beslutterRolle) {
        kall.totrinnskontroll.beslutteVedtak(behandling.id, BeslutteVedtakDto(godkjent = true))
    }

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) {
        return behandling.id
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandling.id
}

fun IntegrationTest.gjennomførAvslagDagligReise(
    fraJournalpost: Journalpost = defaultJournalpost,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
): BehandlingId {
    val behandling = håndterSøknadService.håndterSøknad(fraJournalpost)!!

    kall.behandling.hentBehandling(behandling.id)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return behandling.id
    }

    // Gjennomfører steg: Inngangsvilkår
    kall.vilkårperiode.opprett(lagreVilkårperiodeMålgruppe(behandling.id, målgruppeType = MålgruppeType.INGEN_MÅLGRUPPE))
    kall.vilkårperiode.opprett(lagreVilkårperiodeAktivitet(behandling.id, aktivitetType = AktivitetType.INGEN_AKTIVITET))

    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )

    if (tilSteg == StegType.VILKÅR) {
        return behandling.id
    }

    // Gjennomfører steg: Vilkår
    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.VILKÅR,
        ),
    )

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return behandling.id
    }

    // Gjennomfører steg: Beregn ytelse
    kall.vedtak.dagligReise.lagreAvslagResponse(
        behandlingId = behandling.id,
        avslagDto =
            AvslagDagligReiseDto(
                årsakerAvslag = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                begrunnelse = "begrunnelse",
            ),
    )

    if (tilSteg == StegType.SIMULERING) {
        return behandling.id
    }

    // Gjennomfører steg: Simulering
    kall.steg.ferdigstill(
        behandling.id,
        StegController.FerdigstillStegRequest(
            steg = StegType.SIMULERING,
        ),
    )

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return behandling.id
    }

    // Gjennomfører steg: Send til beslutter
    kall.brev.brev(behandling.id, GenererPdfRequest(minimaltBrev))
    kall.totrinnskontroll.sendTilBeslutter(behandling.id)

    // Gjennomfører steg: Beslutte vedtak
    medBrukercontext(bruker = "nissemor", rolle = rolleConfig.beslutterRolle) {
        kall.totrinnskontroll.beslutteVedtak(behandling.id, BeslutteVedtakDto(godkjent = true))
    }

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) {
        return behandling.id
    }

    // Ferdigstiller behandling
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandling.id
}

fun IntegrationTest.gjennomførHenleggelse(fraJournalpost: Journalpost = defaultJournalpost): BehandlingId {
    val behandling = håndterSøknadService.håndterSøknad(fraJournalpost)!!

    kall.behandling.hentBehandling(behandling.id)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    kall.behandling.henlegg(
        behandlingId = behandling.id,
        henlagtDto =
            HenlagtDto(
                årsak = HenlagtÅrsak.FEILREGISTRERT,
                begrunnelse = "begrunnelse for henleggelse",
            ),
    )
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandling.id
}
