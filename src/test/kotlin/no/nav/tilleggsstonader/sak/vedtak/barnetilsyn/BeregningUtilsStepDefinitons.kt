package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBeregningUtil.tilUke
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.tilAktiviteter
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID

class BeregningUtilsStepDefinitons {
    val behandlingId = UUID.randomUUID()

    var stønadsperioder: StønadsperiodeDto? = null
    var stønadsperiodePerUke: Map<Uke, PeriodeMedDager> = emptyMap()

    var aktiviteter = emptyList<Aktivitet>()
    var aktiviteterPerUke: Map<Uke, List<PeriodeMedDager>> = emptyMap()

    @Gitt("disse stønadsperiodene")
    fun `denne stønadsperioden`(dataTable: DataTable) {
        stønadsperioder = mapStønadsperioder(behandlingId, dataTable).first().tilDto()
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
    fun `forvent følgende aktiviteter for uke`(fom: String, tom: String, dataTable: DataTable) {
        val uke = Uke(parseDato(fom), parseDato(tom))
        val forventedePerioder = parsePeriodeMedDager(dataTable)

        assertThat(aktiviteterPerUke[uke])
            .containsExactlyElementsOf(forventedePerioder)
    }

    private fun parseUke(dataTable: DataTable): List<Uke> {
        return dataTable.mapRad { rad ->
            Uke(
                fom = parseDato(BeregningNøkler.FOM_UKE, rad),
                tom = parseDato(BeregningNøkler.TOM_UKE, rad),
            )
        }
    }

    private fun parsePeriodeMedDager(dataTable: DataTable): List<PeriodeMedDager> {
        return dataTable.mapRad { rad ->
            PeriodeMedDager(
                fom = parseDato(DomenenøkkelFelles.FOM, rad),
                tom = parseDato(DomenenøkkelFelles.TOM, rad),
                antallDager = parseInt(BeregningNøkler.ANTALL_DAGER, rad),
            )
        }
    }
}
