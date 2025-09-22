package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBoolean
import no.nav.tilleggsstonader.sak.cucumber.parseDato
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused", "ktlint:standard:function-naming")
class PrivatBilBeregningStepDefinitions {
    val beregningService =
        PrivatBilBeregningService()

    var reiser: List<DummyReiseMedBil> = emptyList()

    var beregningsResultat: BeregningsresultatPrivatBil? = null
    var forventetBeregningsresultat: List<BeregningsresultatUkeCucumber> = emptyList()

    @Gitt("følgende dummyperioder for daglig reise privat bil")
    fun `følgende dummyperioder`(dataTable: DataTable) {
        reiser =
            dataTable.mapRad { rad ->
                DummyReiseMedBil(
                    fom = parseDato(DomenenøkkelFelles.FOM, rad),
                    tom = parseDato(DomenenøkkelFelles.TOM, rad),
                    reisedagerPerUke = parseInt(DomenenøkkelPrivatBil.ANTALL_REISEDAGER_PER_UKE, rad),
                    reiseavstandEnVei = parseInt(DomenenøkkelPrivatBil.REISEAVSTAND_EN_VEI, rad),
                    dagligParkeringsutgift = parseValgfriInt(DomenenøkkelPrivatBil.PARKERINGSUTGIFT, rad),
                    bompengerEnVei = parseValgfriInt(DomenenøkkelPrivatBil.BOMPENGER, rad),
                    dagligPiggdekkavgift = parseValgfriInt(DomenenøkkelPrivatBil.PIGGDEKKAVGIFT, rad),
                    fergekostnadEnVei = parseValgfriInt(DomenenøkkelPrivatBil.FERGEKOSTNAD, rad),
                )
            }
    }

    @Når("beregner for daglig reise privat bil")
    fun `beregner for daglig reise privat bil`() {
        beregningsResultat = beregningService.beregn(reiser)
    }

    @Så("forventer vi følgende beregningsrsultat for daglig reise privatBil")
    fun `forventer vi følgende beregningsrsultat for daglig reise privat bil`(dataTable: DataTable) {
        val forventetBeregningsresultatForReise = mapUker(dataTable)

        forventetBeregningsresultatForReise.forEachIndexed { index, uke ->
            val gjeldendeReise = beregningsResultat!!.reiser[uke.reiseNr - 1]

            assertThat(gjeldendeReise.uker[index].grunnlag.fom).isEqualTo(uke.grunnlag.fom)
            assertThat(gjeldendeReise.uker[index].grunnlag.tom).isEqualTo(uke.grunnlag.tom)
            assertThat(
                gjeldendeReise.uker[index].grunnlag.antallDagerDenneUkaSomKanDekkes,
            ).isEqualTo(uke.grunnlag.antallDagerDenneUkaSomKanDekkes)
            assertThat(gjeldendeReise.uker[index].grunnlag.antallDagerInkludererHelg).isEqualTo(uke.grunnlag.antallDagerInkludererHelg)
            assertThat(gjeldendeReise.uker[index].stønadsbeløp).isEqualTo(uke.stønadsbeløp)
        }
    }

    private fun mapUker(dataTable: DataTable) =
        dataTable.mapRad { rad ->
            BeregningsresultatUkeCucumber(
                reiseNr = parseInt(DomenenøkkelPrivatBil.REISENR, rad),
                stønadsbeløp = parseInt(DomenenøkkelFelles.BELØP, rad),
                grunnlag =
                    BeregningsgrunnlagForUke(
                        fom = parseDato(DomenenøkkelFelles.FOM, rad),
                        tom = parseDato(DomenenøkkelFelles.TOM, rad),
                        antallDagerDenneUkaSomKanDekkes = parseInt(DomenenøkkelPrivatBil.ANTALL_DAGER_DEKT_UKE, rad),
                        antallDagerInkludererHelg = parseBoolean(DomenenøkkelPrivatBil.INKLUDERER_HELG, rad),
                    ),
            )
        }
}

enum class DomenenøkkelPrivatBil(
    override val nøkkel: String,
) : Domenenøkkel {
    ANTALL_REISEDAGER_PER_UKE("Antall reisedager per uke"),
    REISEAVSTAND_EN_VEI("Reiseavstand"),
    PARKERINGSUTGIFT("Parkering"),
    BOMPENGER("Bompenger"),
    PIGGDEKKAVGIFT("Piggdekkavgift"),
    FERGEKOSTNAD("Fergekostnad"),
    REISENR("Reisenr"),
    ANTALL_DAGER_DEKT_UKE("Antall dager dekt i uke"),
    INKLUDERER_HELG("Inkluderer helg"),
}

data class BeregningsresultatUkeCucumber(
    val reiseNr: Int,
    val grunnlag: BeregningsgrunnlagForUke,
    val stønadsbeløp: Int,
)
