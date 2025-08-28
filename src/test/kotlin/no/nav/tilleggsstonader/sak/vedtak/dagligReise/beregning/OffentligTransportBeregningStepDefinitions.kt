package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.VilkårRepositoryFake
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.vedtak.cucumberUtils.mapVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
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

    var utgiftOffentligTransport: UtgiftOffentligTransport? = null
    var beregningsResultat: Beregningsresultat? = null
    var forventetBeregningsresultat: Beregningsresultat? = null
    var vedtaksperioder: List<Vedtaksperiode> = emptyList()

    @Gitt("følgende vedtaksperioder for daglig reise offentlig transport")
    fun `følgende vedtaksperioder`(dataTable: DataTable) {
        vedtaksperioder = mapVedtaksperioder(dataTable)
    }

    @Gitt("følgende beregningsinput for offentlig transport")
    fun `følgende beregnins input offentlig transport`(dataTable: DataTable) {
        val reiseInformasjon =
            dataTable.mapRad { rad ->
                UtgiftOffentligTransport(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    antallReisedagerPerUke =
                        parseInt(
                            DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE,
                            rad,
                        ),
                    prisEnkelbillett = parseInt(DomenenøkkelOffentligtransport.PRIS_ENKELTBILLETT, rad),
                    pris30dagersbillett =
                        parseInt(
                            DomenenøkkelOffentligtransport.PRIS_TRETTI_DAGERS_BILLETT,
                            rad,
                        ),
                )
            }

        utgiftOffentligTransport =
            UtgiftOffentligTransport(
                fom = reiseInformasjon.first().fom,
                tom = reiseInformasjon.first().tom,
                antallReisedagerPerUke = reiseInformasjon.sumOf { it.antallReisedagerPerUke },
                prisEnkelbillett = reiseInformasjon.sumOf { it.prisEnkelbillett },
                pris30dagersbillett = reiseInformasjon.sumOf { it.pris30dagersbillett },
            )
    }

    @Når("beregner for daglig reise offentlig transport")
    fun `beregner for daglig reise offentlig transport`() {
        beregningsResultat =
            offentligTransportBeregningService.beregn(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperioder,
            )
    }

    @Så("forventer vi følgende beregningsrsultat for daglig resie offentlig transport")
    fun `forventer vi følgende beregningsrsultat for daglig resie offentlig transport`(dataTable: DataTable) {
        val forventetBeregninsresultat =
            Beregningsresultat(
                reiser =
                    listOf(
                        BeregningsresultatForReise(
                            perioder = mapBeregningsresultatForPeriode(dataTable),
                        ),
                    ),
            )

        beregningsResultat!!.reiser.forEachIndexed { index, reise ->
            reise.perioder.forEachIndexed { index2, periode ->
                assertThat(periode.grunnlag.fom).isEqualTo(
                    forventetBeregninsresultat.reiser[index]
                        .perioder[index2]
                        .grunnlag.fom,
                )
                assertThat(periode.grunnlag.tom).isEqualTo(
                    forventetBeregninsresultat.reiser[index]
                        .perioder[index2]
                        .grunnlag.tom,
                )
                assertThat(periode.beløp).isEqualTo(
                    forventetBeregninsresultat.reiser[index].perioder[index2].beløp,
                )
            }
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

private fun mapBeregningsresultatForPeriode(dataTable: DataTable) =
    dataTable.mapRad { rad ->
        BeregningsresultatForPeriode(
            grunnlag =
                Beregningsgrunnlag(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    prisEnkeltbillett = 0,
                    pris30dagersbillett = 0,
                    antallReisedagerPerUke = 0,
                    antallReisedager = 0,
                    vedtaksperioder = emptyList(),
                ),
            beløp = parseInt(DomenenøkkelFelles.BELØP, rad),
        )
    }
