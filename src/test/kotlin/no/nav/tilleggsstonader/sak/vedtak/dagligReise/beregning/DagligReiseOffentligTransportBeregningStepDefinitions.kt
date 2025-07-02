package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class DagligReiseOffentligTransportBeregningStepDefinitions {
    val dagligReiseOffentligTransportBeregningService = DagligReiseOffentligTransportBeregningService()

    var beregningsResultat: Int? = null

    @Når("beregner for daglig reise offentlig transport")
    fun `beregner for daglig reise offentlig transport`() {
        beregningsResultat = dagligReiseOffentligTransportBeregningService.beregn()
    }

    @Så("forventer vi følgende beregningsrsultat for daglig resie offentlig transport")
    fun `forventer vi følgende beregningsrsultat for daglig resie offentlig transport`(dataTable: DataTable) {
        val beregningsresultatListe =
            dataTable.mapRad { rad ->
                parseInt(DomenenøkkelFelles.BELØP, rad)
            }
        val beregningsresultatHentetUt = beregningsresultatListe.single()
        assertThat(beregningsresultatHentetUt).isEqualTo(beregningsResultat)
    }
}
