package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VedtakRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårperiodeRepositoryFake
import no.nav.tilleggsstonader.sak.tidligsteendring.TidligsteEndringForBeregning
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.OffentligTransportBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
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
    val vedtakRepositoryFake = VedtakRepositoryFake()
    val unleashServiceMock = mockk<UnleashService>()
    val vilkårperiodeRepositoryFake = VilkårperiodeRepositoryFake()
    val behandlingId = BehandlingId.random()
    val utledTidligsteEndringService =
        mockk<UtledTidligsteEndringService> {
            every { utledTidligsteEndringForBeregning(any(), any()) } returns TidligsteEndringForBeregning(null, null)
        }
    val vilkårperiodeServiceMock =
        mockk<VilkårperiodeService>().apply {
            every { hentVilkårperioder(any()) } answers {
                val vilkårsperioder =
                    vilkårperiodeRepositoryFake.findByBehandlingId(behandlingId).sorted()
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
            unleashService = unleashServiceMock,
        )

    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(vilkårperiodeService = vilkårperiodeServiceMock)

    val offentligTransportBeregningService = OffentligTransportBeregningService()

    var beregningsResultat: BeregningsresultatOffentligTransport? = null
    var forventetBeregningsresultat: BeregningsresultatOffentligTransport? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var vilkårperioder: List<Vilkårperioder> = emptyList()
    var vilkår: List<VilkårDagligReise> = emptyList()

    @Gitt("følgende vedtaksperioder for daglig reise offentlig transport")
    fun `følgende vedtaksperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende beregningsinput for offentlig transport")
    fun `følgende beregnins input offentlig transport`(utgiftData: DataTable) {
        every { behandlingServiceMock.hentSaksbehandling(any<BehandlingId>()) } returns
            dummyBehandling(
                behandlingId = behandlingId,
                steg = StegType.VILKÅR,
            )

        every { unleashServiceMock.isEnabled(any()) } returns true

        vilkår =
            utgiftData.mapRad { rad ->
                val nyttVilkår = mapTilVilkårDagligReise(TypeDagligReise.OFFENTLIG_TRANSPORT, rad)
                dagligReiseVilkårService.opprettNyttVilkår(behandlingId = behandlingId, nyttVilkår = nyttVilkår)
            }
    }

    @Når("beregner for daglig reise offentlig transport")
    fun `beregner for daglig reise offentlig transport`() {
        beregningsResultat =
            offentligTransportBeregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkår = vilkår,
                brukersNavKontor = null,
            )
    }

    @Så("forventer vi følgende beregningsrsultat for daglig reise offentlig transport, reiseNr={}")
    fun `forventer vi følgende beregningsrsultat for daglig reise offentlig transport`(
        reiserNummer: Int,
        dataTable: DataTable,
    ) {
        val forventetBeregningsresultatForReise =
            BeregningsresultatForReise(
                reiseId = dummyReiseId,
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
