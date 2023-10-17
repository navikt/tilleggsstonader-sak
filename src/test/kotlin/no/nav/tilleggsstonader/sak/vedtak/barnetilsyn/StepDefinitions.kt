package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.barnIder
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

private enum class NøkkelBeregningTilsynBarn(
    override val nøkkel: String,
) : Domenenøkkel {
    UTGIFT("Utgift"),
}

class StepDefinitions {

    val service = TilsynBarnBeregningService()

    var exception: Exception? = null

    var stønadsperioder = emptyList<Stønadsperiode>()
    var utgifter = mutableMapOf<UUID, List<Utgift>>()

    @Gitt("følgende støndsperioder")
    fun `følgende støndsperioder`(dataTable: DataTable) {
        stønadsperioder = dataTable.mapRad { rad ->
            Stønadsperiode(
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
            )
        }
    }

    @Gitt("følgende utgifter for barn: {}")
    fun `følgende utgifter`(barnId: Int, dataTable: DataTable) {
        utgifter[barnIder[barnId]!!] = dataTable.mapRad { rad ->
            Utgift(
                fom = parseÅrMåned(DomenenøkkelFelles.FOM, rad),
                tom = parseÅrMåned(DomenenøkkelFelles.TOM, rad),
                utgift = parseInt(NøkkelBeregningTilsynBarn.UTGIFT, rad),
            )
        }
    }

    @Når("beregner")
    fun `beregner`() {
        try {
            service.beregn(stønadsperioder, utgifter)
        } catch (e: Exception) {
            exception = e
        }
    }

    @Så("forvent følgende feil: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(exception!!).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende beregningsresultat")
    fun `forvent følgende beregningsresultat`() {
        assertThat(exception).isNull()
        // TODO
    }
}
