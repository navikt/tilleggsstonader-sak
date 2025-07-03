package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import BeregningsresultatOffentligTransport
import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class DagligReiseOffentligTransportBeregningStepDefinitions {
    val dagligReiseOffentligTransportBeregningService = DagligReiseOffentligTransportBeregningService()

    var beregningsInputOffentligTransport: List<UtgiftOffentligTransport>? = null
    var beregningsResultat: BeregningsresultatOffentligTransport? = null

    @Gitt("følgende beregnings input for offentlig transport")
    fun `følgende beregnins input offentlig transport`(dataTable: DataTable) {
        beregningsInputOffentligTransport =
            dataTable.mapRad { rad ->
                UtgiftOffentligTransport(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    antallReisedagerPerUke = parseInt(DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE, rad),
                    prisEnkelbillett = parseInt(DomenenøkkelOffentligtransport.PRIS_ENKELTBILLETT, rad),
                    pris7dagersbillett = parseInt(DomenenøkkelOffentligtransport.PRIS_SYV_DAGERS_BILLETT, rad),
                    pris30dagersbillett = parseInt(DomenenøkkelOffentligtransport.PRIS_TRETTI_DAGERS_BILLETT, rad),
                )
            }
    }

    @Når("beregner for daglig reise offentlig transport")
    fun `beregner for daglig reise offentlig transport`() {
        beregningsResultat = dagligReiseOffentligTransportBeregningService.beregn(beregningsInputOffentligTransport!!)
    }

    @Så("forventer vi følgende beregningsrsultat for daglig resie offentlig transport")
    fun `forventer vi følgende beregningsrsultat for daglig resie offentlig transport`(dataTable: DataTable) {
        val beregningsresultatListe =
            dataTable.mapRad { rad ->
                parseInt(DomenenøkkelFelles.BELØP, rad)
            }
        beregningsResultat!!.peroder.forEachIndexed { index, it ->
            assertThat(it.beløp).isEqualTo(beregningsresultatListe[index])
        }
    }
}

enum class DomenenøkkelOffentligtransport(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
    PRIS_ENKELTBILLETT("Pris enkeltbillett"),
    PRIS_SYV_DAGERS_BILLETT("Pris syv-dagersbillett"),
    PRIS_TRETTI_DAGERS_BILLETT("Pris tretti-dagersbillett"),
}
