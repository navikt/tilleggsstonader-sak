package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class DagligReiseOffentligTransportBeregningStepDefinitions {
    val dagligReiseOffentligTransportBeregningService = DagligReiseOffentligTransportBeregningService()

    var beregningsInputOffentligTransport: BeregningsInputOffentligTransport? = null
    var beregningsResultat: Int? = null

    @Gitt("følgende beregnings input for offentlig transport")
    fun `følgende beregnins input offentlig transport`(dataTable: DataTable) {
        val beregningsInputOffentligTransportHentet =
            dataTable.mapRad { rad ->
                BeregningsInputOffentligTransport(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    prisEnkelBilett = parseInt(DomenenøkkelFelles.BELØP, rad),
                    antallReisedagerPerUke = parseInt(DomenenøkkelOffentligtransport.ANTALL_REISEDAGER_PER_UKE, rad),
                )
            }

        beregningsInputOffentligTransport = beregningsInputOffentligTransportHentet.single()
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
        val beregningsresultatHentetUt = beregningsresultatListe.single()
        assertThat(beregningsResultat).isEqualTo(beregningsresultatHentetUt)
    }
}

enum class DomenenøkkelOffentligtransport(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
}
