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
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode

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
    val behandling = kall.behandling.hentBehandling(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    if (tilSteg == StegType.INNGANGSVILKÅR) {
        return behandlingId
    }

    // Gjennomfører steg: Inngangsvilkår
    kall.vilkårperiode.opprett(medAktivitet(behandlingId))
    kall.vilkårperiode.opprett(medMålgruppe(behandlingId))

    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.INNGANGSVILKÅR,
        ),
    )

    if (tilSteg == StegType.VILKÅR) {
        return behandlingId
    }

    // Gjennomfører steg: Vilkår
    medVilkår.forEach {
        if (behandling.stønadstype.gjelderDagligReise()) {
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

    if (tilSteg == StegType.BEREGNE_YTELSE) {
        return behandlingId
    }

    // Gjennomfører steg: Beregn ytelse
    val foreslåtteVedtaksperioder = kall.vedtak.foreslåVedtaksperioder(behandlingId)

    kall.vedtak.lagreInnvilgelse(
        stønadstype = behandling.stønadstype,
        behandlingId = behandlingId,
        innvilgelseDto =
            mapInnvilgelseRequest(
                behandling.stønadstype,
                vedtaksperioder =
                    foreslåtteVedtaksperioder.map {
                        it.tilVedtaksperiodeDto()
                    },
            ),
    )

    if (tilSteg == StegType.SIMULERING) {
        return behandlingId
    }

    // Gjennomfører steg: Simulering
    kall.steg.ferdigstill(
        behandlingId,
        StegController.FerdigstillStegRequest(
            steg = StegType.SIMULERING,
        ),
    )

    if (tilSteg == StegType.SEND_TIL_BESLUTTER) {
        return behandlingId
    }

    // Gjennomfører steg: Send til beslutter
    kall.brev.genererPdf(behandlingId, GenererPdfRequest(minimaltBrev))
    kall.totrinnskontroll.sendTilBeslutter(behandlingId)

    if (tilSteg == StegType.BESLUTTE_VEDTAK) {
        return behandlingId
    }

    // Gjennomfører steg: Beslutte vedtak
    medBrukercontext(bruker = "nissemor", rolle = rolleConfig.beslutterRolle) {
        kall.totrinnskontroll.beslutteVedtak(behandlingId, BeslutteVedtakDto(godkjent = true))
    }

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
    kall.behandling.hentBehandling(behandlingId)

    // Oppretter oppgave
    kjørTasksKlareForProsessering()

    kall.behandling.henlegg(
        behandlingId = behandlingId,
        henlagtDto =
            HenlagtDto(
                årsak = HenlagtÅrsak.FEILREGISTRERT,
                begrunnelse = "begrunnelse for henleggelse",
            ),
    )
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return behandlingId
}

private fun mapInnvilgelseRequest(
    stønadstype: Stønadstype,
    vedtaksperioder: List<VedtaksperiodeDto>,
): VedtakRequest =
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> InnvilgelseTilsynBarnRequest(vedtaksperioder)
        Stønadstype.LÆREMIDLER -> InnvilgelseLæremidlerRequest(vedtaksperioder)
        Stønadstype.BOUTGIFTER -> InnvilgelseBoutgifterRequest(vedtaksperioder)
        Stønadstype.DAGLIG_REISE_TSO -> InnvilgelseDagligReiseRequest(vedtaksperioder)
        Stønadstype.DAGLIG_REISE_TSR -> InnvilgelseDagligReiseRequest(vedtaksperioder)
    }
