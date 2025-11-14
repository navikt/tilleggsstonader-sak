package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.behandlingIdTilTestId
import no.nav.tilleggsstonader.sak.cucumber.TestIdTilBehandlingIdHolder.testIdTilBehandlingId
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.TilkjentYtelseRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class OffentligTransportBeregningStepDefinitions {
    val behandlingServiceMock = mockk<BehandlingService>()
    val vilkårServiceMock = mockk<VilkårService>()
    val vilkårRepositoryFake = VilkårRepositoryFake()
    val vilkårperiodeRepositoryFake = VilkårperiodeRepositoryFake()
    val vedtakRepositoryFake = VedtakRepositoryFake()
    val tilkjentYtelseRepositoryFake = TilkjentYtelseRepositoryFake()
    val utledTidligsteEndringService =
        mockk<UtledTidligsteEndringService> {
            every { utledTidligsteEndringForBeregning(any(), any()) } returns null
        }

    val unleashService = mockUnleashService()

    val vilkårperiodeServiceMock =
        mockk<VilkårperiodeService>().apply {
            every { hentVilkårperioder(any()) } answers {
                val vilkårsperioder =
                    vilkårperiodeRepositoryFake.findByBehandlingId(BehandlingId.random()).sorted()
                Vilkårperioder(
                    målgrupper = vilkårsperioder.ofType<MålgruppeFaktaOgVurdering>(),
                    aktiviteter = vilkårsperioder.ofType<AktivitetFaktaOgVurdering>(),
                )
            }
        }

    val dagligReiseVilkårService =
        DagligReiseVilkårService(
            vilkårRepository = vilkårRepositoryFake,
            vilkårService = vilkårServiceMock,
            behandlingService = behandlingServiceMock,
        )

    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(vilkårperiodeService = vilkårperiodeServiceMock)

    val offentligTransportBeregningService =
        OffentligTransportBeregningService()

    val vilkårService =
        VilkårService(
            behandlingService = behandlingServiceMock,
            vilkårRepository = vilkårRepositoryFake,
            barnService = mockk(relaxed = true),
        )

    val simuleringServiceMock = mockk<SimuleringService>(relaxed = true)

    val beregningService =
        DagligReiseBeregningService(
            offentligTransportBeregningService = offentligTransportBeregningService,
            vilkårService = vilkårService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepositoryFake,
            utledTidligsteEndringService = utledTidligsteEndringService,
        )
    val opphørValideringService =
        OpphørValideringService(
            vilkårsperiodeService = vilkårperiodeServiceMock,
            vilkårService = vilkårService,
        )
    val steg =
        DagligReiseBeregnYtelseSteg(
            beregningService = beregningService,
            utledTidligsteEndringService = utledTidligsteEndringService,
            vedtakRepository = vedtakRepositoryFake,
            tilkjentYtelseService = TilkjentYtelseService(tilkjentYtelseRepositoryFake),
            simuleringService = simuleringServiceMock,
        )

    var beregningsResultat: BeregningsresultatOffentligTransport? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var feil: Exception? = null

    @Gitt("følgende vedtaksperioder for daglig reise offentlig transport")
    fun `følgende vedtaksperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende beregningsinput for offentlig transport behandling={}")
    fun `følgende beregnins input offentlig transport`(
        behandlingIdTall: Int,
        utgiftData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )
        utgiftData.mapRad { rad ->
            val nyttVilkår = mapTilVilkårDagligReise(rad)
            dagligReiseVilkårService.opprettNyttVilkår(behandlingId = behandlingId, nyttVilkår = nyttVilkår)
        }

        dagligReiseVilkårService.hentVilkårForBehandling(behandlingId)
    }

    @Når("beregner for daglig reise offentlig transport behandling={}")
    fun `beregner for daglig reise offentlig transport`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)

        beregningsResultat =
            offentligTransportBeregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkår = dagligReiseVilkårService.hentVilkårForBehandling(behandlingId),
            )
    }

    @Gitt("vi kopierer perioder fra forrige daglig reise behandling for behandling={}")
    fun `kopierer perioder`(behandlingIdTall: Int) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forrigeIverksatteBehandlingId =
            forrigeIverksatteBehandlingId(behandlingId) ?: error("Forventer å finne forrigeIverksatteBehandlingId")

        val tidligereVilkårsperioder = vilkårperiodeRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)
        val tidligereVilkår = vilkårRepositoryFake.findByBehandlingId(forrigeIverksatteBehandlingId)

        vilkårperiodeRepositoryFake.insertAll(tidligereVilkårsperioder.map { it.kopierTilBehandling(behandlingId) })
        vilkårRepositoryFake.insertAll(tidligereVilkår.map { it.kopierTilBehandling(behandlingId) })
    }

    @Når("vi innvilger daglig reise behandling={} med tidligsteEndring={}")
    fun `innvilgelse med tidligsteEndring`(
        behandlingIdTall: Int,
        tidligsteEndringStr: String,
        vedtaksperiodeData: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val tidligsteEndring = parseDato(tidligsteEndringStr)
        val vedtaksperioder = mapVedtaksperioder(vedtaksperiodeData).map { it.tilDto() }

        every {
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId,
                any(),
            )
        } returns tidligsteEndring

        // Beregne vden kopierte behandlingen
        steg.utførSteg(
            dummyBehandling(behandlingId),
            InnvilgelseDagligReiseRequest(vedtaksperioder = vedtaksperioder),
        )
    }

    @Så("forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr={}")
    fun `forventer vi følgende beregningsrsultat for daglig reise offentlig transport`(
        reiserNummer: Int,
        dataTable: DataTable,
    ) {
        val forventetBeregningsresultatForReise =
            BeregningsresultatForReise(
                perioder = mapBeregningsresultatForPeriode(dataTable),
            )
        val beregningsreulsresultatForReise =
            beregningsResultat!!.reiser[reiserNummer - 1]

        assertThat(beregningsreulsresultatForReise.perioder.size).isEqualTo(forventetBeregningsresultatForReise.perioder.size)
        forventetBeregningsresultatForReise.perioder.forEachIndexed { index, periode ->
            assertThat(beregningsreulsresultatForReise.perioder[index].grunnlag.fom).isEqualTo(periode.grunnlag.fom)
            assertThat(beregningsreulsresultatForReise.perioder[index].grunnlag.tom).isEqualTo(periode.grunnlag.tom)
            assertThat(beregningsreulsresultatForReise.perioder[index].beløp).isEqualTo(periode.beløp)
            assertThat(beregningsreulsresultatForReise.perioder[index].billettdetaljer)
                .isEqualTo(periode.billettdetaljer)
        }
    }

    @Så("kan vi forvente følgende daglig reise beregningsresultat for behandling={}")
    fun `forvent beregningsresultatet daglig reise`(
        behandlingIdTall: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = testIdTilBehandlingId.getValue(behandlingIdTall)
        val forventetBeregningsresultatForReise =
            BeregningsresultatForReise(
                perioder = mapBeregningsresultatForPeriode(dataTable),
            )
        val beregningsresultat1 = beregningsResultat
    }
}

enum class DomenenøkkelOffentligtransport(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
    PRIS_ENKELTBILLETT("Pris enkeltbillett"),
    PRIS_SYV_DAGERS_BILLETT("Pris syv-dagersbillett"),
    PRIS_TRETTI_DAGERS_BILLETT(
        "Pris tretti-dagersbillett",
    ),
}

private fun forrigeIverksatteBehandlingId(behandlingId: BehandlingId): BehandlingId? {
    val behandlingIdInt = behandlingIdTilTestId(behandlingId)
    return if (behandlingIdInt > 1) {
        testIdTilBehandlingId.getValue(behandlingIdInt - 1)
    } else {
        null
    }
}
