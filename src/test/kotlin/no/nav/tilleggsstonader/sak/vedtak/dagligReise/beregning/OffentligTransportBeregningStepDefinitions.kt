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
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class OffentligTransportBeregningStepDefinitions {
    val behandlingServiceMock = mockk<BehandlingService>()
    val vilkårRepositoryFake = VilkårRepositoryFake()

    val vilkårService =
        VilkårService(
            behandlingService = behandlingServiceMock,
            vilkårRepository = vilkårRepositoryFake,
            barnService = mockk(relaxed = true),
        )
    val offentligTransportBeregningService = OffentligTransportBeregningService(vilkårService)

    var beregningsResultat: Beregningsresultat? = null
    var forventetBeregningsresultat: Beregningsresultat? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()

    val behandlingId = BehandlingId.random()

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

        utgiftData.mapRad { rad ->
            val nyttVilkår = mapTilVilkår(rad, behandlingId)
            vilkårService.opprettNyttVilkår(opprettVilkårDto = nyttVilkår)
        }
    }

    @Når("beregner for daglig reise offentlig transport")
    fun `beregner for daglig reise offentlig transport`() {
        beregningsResultat =
            offentligTransportBeregningService.beregn(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperioder,
            )
    }

    @Så("forventer vi følgende beregningsrsultat for daglig resie offentlig transport, reiseNr={}")
    fun `forventer vi følgende beregningsrsultat for daglig resie offentlig transport`(
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
        }
    }
}

enum class DomenenøkkelOffentligtransport(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
    PRIS_ENKELTBILLETT("Pris enkeltbillett"),
    PRIS_TRETTI_DAGERS_BILLETT(
        "Pris tretti-dagersbillett",
    ),
}
