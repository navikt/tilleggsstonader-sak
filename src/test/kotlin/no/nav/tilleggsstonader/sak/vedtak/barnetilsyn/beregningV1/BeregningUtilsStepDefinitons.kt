package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtilsFelles.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtilsFelles.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.Uke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import org.assertj.core.api.Assertions.assertThat

@Suppress("ktlint:standard:function-naming", "unused")
class BeregningUtilsStepDefinitons {
    val behandlingId = BehandlingId.random()

    var stønadsperioder: StønadsperiodeBeregningsgrunnlag? = null
    var stønadsperiodePerUke: Map<Uke, PeriodeMedDager> = emptyMap()

    var aktiviteter = emptyList<Aktivitet>()
    var aktiviteterPerUke: Map<Uke, List<PeriodeMedDager>> = emptyMap()

    @Gitt("disse stønadsperiodene")
    fun `denne stønadsperioden`(dataTable: DataTable) {
        stønadsperioder = mapStønadsperioder(behandlingId, dataTable).first().tilStønadsperiodeBeregningsgrunnlag()
    }

    @Gitt("disse aktivitetene")
    fun `disse aktivitetene`(dataTable: DataTable) {
        aktiviteter = mapAktiviteter(behandlingId, dataTable).tilAktiviteter()
    }

    @Når("splitter stønadsperiode per uke")
    fun `splitter stønadsperioder per uke`() {
        stønadsperiodePerUke = stønadsperioder!!.tilUke()
    }

    @Når("splitter aktiviteter per uke")
    fun `splitter aktiviteter per uke`() {
        aktiviteterPerUke = aktiviteter.tilDagerPerUke()
    }

    @Så("forvent at aktivitetene ble splittet til {} uker")
    fun `forvent antall uker`(antallUker: Int) {
        assertThat(aktiviteterPerUke).hasSize(antallUker)
    }

    @Så("forvent følgende stønadsperioder per uke")
    fun `forvent følgende stønadsperioder per uke`(dataTable: DataTable) {
        val uker = parseUke(dataTable)
        val forventedePerioder = parsePeriodeMedDager(dataTable)

        uker.forEachIndexed { indeks, uke ->
            assertThat(stønadsperiodePerUke[uke]).isEqualTo(forventedePerioder[indeks])
        }

        // Sjekk at det ikke ble laget flere uker enn forventet
        assertThat(stønadsperiodePerUke.size).isEqualTo(uker.size)
    }

    @Så("forvent følgende aktiviteter for uke med fom={} og tom={}")
    fun `forvent følgende aktiviteter for uke`(
        fom: String,
        tom: String,
        dataTable: DataTable,
    ) {
        val uke = Uke(parseDato(fom), parseDato(tom))
        val forventedePerioder = parsePeriodeMedDager(dataTable)

        assertThat(aktiviteterPerUke[uke])
            .containsExactlyElementsOf(forventedePerioder)
    }

    private fun parseUke(dataTable: DataTable): List<Uke> =
        dataTable.mapRad { rad ->
            Uke(
                fom = parseDato(BeregningNøkler.FOM_UKE, rad),
                tom = parseDato(BeregningNøkler.TOM_UKE, rad),
            )
        }

    private fun parsePeriodeMedDager(dataTable: DataTable): List<PeriodeMedDager> =
        dataTable.mapRad { rad ->
            PeriodeMedDager(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                antallDager = parseInt(BeregningNøkler.ANTALL_DAGER, rad),
            )
        }
}
