package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegFerdigstiltResponse
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilVedtaksperiodeDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsoRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseTsrRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreVilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import java.util.UUID

fun IntegrationTest.gjennomførInngangsvilkårSteg(
    testdataDsl: BehandlingTestdataDsl,
    behandlingId: BehandlingId,
): StegType {
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

    // Oppretter målgrupper
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

    val nesteSteg =
        kall.steg
            .ferdigstill(
                behandlingId,
                StegController.FerdigstillStegRequest(
                    steg = StegType.INNGANGSVILKÅR,
                ),
            ).nesteSteg
    kjørTasksKlareForProsessering()

    return nesteSteg
}

fun IntegrationTest.gjennomførVilkårSteg(
    testdata: BehandlingTestdataDsl,
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
): StegType {
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

    testdata.vilkår.opprettScope
        .build(
            behandlingId,
            barnIder,
            aktiviteter =
                kall.vilkårperiode
                    .hentForBehandling(behandlingId)
                    .vilkårperioder.aktiviteter,
        ).forEach {
            if (stønadstype.gjelderDagligReise()) {
                kall.vilkårDagligReise.opprettVilkår(behandlingId, it as LagreVilkårDagligReiseDto)
            } else {
                kall.vilkår.opprettVilkår(it as OpprettVilkårDto)
            }
        }

    if (stønadstype.gjelderDagligReise()) {
        val aktiviteter =
            kall.vilkårperiode
                .hentForBehandling(behandlingId)
                .vilkårperioder.aktiviteter
        testdata.vilkår.updateDagligReise
            .map { it(vilkårDagligReise, aktiviteter) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.oppdaterVilkår(dto, vilkårId, behandlingId) }

        testdata.vilkår.deleteDagligReise
            .map { it(vilkårDagligReise) }
            .forEach { (vilkårId, dto) -> kall.vilkårDagligReise.slettVilkår(behandlingId, vilkårId, dto) }
    } else {
        testdata.vilkår.update
            .map { it(vilkår) }
            .forEach { kall.vilkår.oppdaterVilkår(it) }

        testdata.vilkår.delete
            .map { it(vilkår) }
            .forEach { kall.vilkår.slettVilkår(it) }
    }

    val nesteSteg =
        kall.steg
            .ferdigstill(
                behandlingId,
                StegController.FerdigstillStegRequest(
                    steg = StegType.VILKÅR,
                ),
            ).nesteSteg

    kjørTasksKlareForProsessering()

    return nesteSteg
}

fun IntegrationTest.gjennomførBeregningSteg(
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse(),
): StegType =
    gjennomførBeregningStegKall(behandlingId, stønadstype, opprettVedtak)
        .expectStatus()
        .isOk
        .expectBody<StegFerdigstiltResponse>()
        .returnResult()
        .responseBody!!
        .nesteSteg

fun IntegrationTest.gjennomførBeregningStegKall(
    behandlingId: BehandlingId,
    stønadstype: Stønadstype,
    opprettVedtak: OpprettVedtak = OpprettInnvilgelse(),
): RestTestClient.ResponseSpec =
    when (opprettVedtak) {
        is OpprettInnvilgelse -> {
            val vedtaksperioder =
                opprettVedtak.vedtaksperioder ?: kall.vedtak
                    .foreslåVedtaksperioder(behandlingId)
                    .map { it.tilVedtaksperiodeDto() }
            kall.vedtak.apiRespons
                .lagreInnvilgelse(
                    stønadstype = stønadstype,
                    behandlingId = behandlingId,
                    innvilgelseDto =
                        when (stønadstype) {
                            Stønadstype.BARNETILSYN -> InnvilgelseTilsynBarnRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.LÆREMIDLER -> InnvilgelseLæremidlerRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.BOUTGIFTER -> InnvilgelseBoutgifterRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.DAGLIG_REISE_TSO -> InnvilgelseDagligReiseTsoRequest(vedtaksperioder = vedtaksperioder)
                            Stønadstype.DAGLIG_REISE_TSR ->
                                InnvilgelseDagligReiseTsrRequest(
                                    vedtaksperioder = vedtaksperioder.tilVedtaksperiodeDagligReiseDto(),
                                )

                            Stønadstype.REISE_TIL_SAMLING_TSO -> TODO("InnvilgelseReiseTilSamlingTsoRequest")
                        },
                )
        }

        is OpprettAvslag ->
            kall.vedtak.apiRespons
                .lagreVedtak(
                    stønadstype = stønadstype,
                    behandlingId = behandlingId,
                    typeVedtakPath = "avslag",
                    vedtakDto =
                        when (stønadstype) {
                            Stønadstype.BARNETILSYN ->
                                AvslagTilsynBarnDto(
                                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                                    begrunnelse = "begrunnelse",
                                )

                            Stønadstype.LÆREMIDLER ->
                                AvslagLæremidlerDto(
                                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                                    begrunnelse = "begrunnelse",
                                )

                            Stønadstype.BOUTGIFTER ->
                                AvslagBoutgifterDto(
                                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                                    begrunnelse = "begrunnelse",
                                )

                            Stønadstype.DAGLIG_REISE_TSO,
                            Stønadstype.DAGLIG_REISE_TSR,
                            ->
                                AvslagDagligReiseDto(
                                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                                    begrunnelse = "begrunnelse",
                                )

                            Stønadstype.REISE_TIL_SAMLING_TSO -> TODO("AvslagReiseTilSamlingTsoRequest")
                        },
                )
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

                            Stønadstype.REISE_TIL_SAMLING_TSO -> TODO("OpphørReiseTilSamlingTsoRequest")
                        },
                )
    }

fun IntegrationTest.gjennomførRegistrerKjørelisteSteg(behandlingId: BehandlingId) =
    kall.steg.ferdigstill(behandlingId, StegController.FerdigstillStegRequest(StegType.REGISTRER_KJØRELISTE)).nesteSteg

fun IntegrationTest.gjennomførKjørelisteSteg(behandlingId: BehandlingId) =
    kall.steg.ferdigstill(behandlingId, StegController.FerdigstillStegRequest(StegType.KJØRELISTE)).nesteSteg

fun IntegrationTest.gjennomførBeregningStegDagligReise(behandlingId: BehandlingId) =
    kall.steg.ferdigstill(behandlingId, StegController.FerdigstillStegRequest(StegType.BEREGNING)).nesteSteg

fun IntegrationTest.gjennomførSimuleringSteg(behandlingId: BehandlingId): StegType {
    val nesteSteg =
        kall.steg
            .ferdigstill(
                behandlingId,
                StegController.FerdigstillStegRequest(
                    steg = StegType.SIMULERING,
                ),
            ).nesteSteg

    kjørTasksKlareForProsessering()

    return nesteSteg
}

const val MINIMALT_BREV = """SAKSBEHANDLER_SIGNATUR - BREVDATO_PLACEHOLDER - BESLUTTER_SIGNATUR"""

fun IntegrationTest.gjennomførSendTilBeslutterSteg(behandlingId: BehandlingId): StegType {
    kall.brev.genererPdf(behandlingId, GenererPdfRequest(MINIMALT_BREV))
    kall.brevmottakere.hent(behandlingId)
    kall.totrinnskontroll.sendTilBeslutter(behandlingId)
    kjørTasksKlareForProsessering()

    // Send-til-beslutter kall returnerer egen body men steg skal alltid være BESLUTTE_VEDTAK om kallet er OK
    return StegType.BESLUTTE_VEDTAK
}

fun IntegrationTest.gjennomførBeslutteVedtakSteg(behandlingId: BehandlingId): StegType {
    medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
        tilordneÅpenBehandlingOppgaveForBehandling(behandlingId)
        kall.totrinnskontroll.beslutteVedtak(behandlingId, BeslutteVedtakDto(godkjent = true))
    }
    kjørTasksKlareForProsessering()

    // Beslutte-vedtak steg returnerer egen body men steg skal alltid være BEHANDLING_FERDIGSTILT om kallet er OK,
    // slik at gjennomførBehandlingsløp()-funksjon kan avslutte steg-gjennomføring
    return StegType.BEHANDLING_FERDIGSTILT
}
