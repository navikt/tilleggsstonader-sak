package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriEnum
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningPeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import org.assertj.core.api.Assertions.assertThat

enum class BeregningNøkler(
    override val nøkkel: String,
) : Domenenøkkel {
    STUDIENIVÅ("Studienivå"),
    STUDIEPROSENT("Studieprosent"),
    MÅNED("Måned"),
    SATS("Sats"),
}

class StepDefinitions {
    val læremidlerBeregningService = LæremidlerBeregningService()

    var beregningPeriode: BeregningPeriode? = null
    var resultat: BeregningsresultatLæremidler? = null

    @Gitt("følgende beregningsperiode for læremidler")
    fun `følgende beregningsperiode for læremidler`(dataTable: DataTable) {
        beregningPeriode = dataTable.mapRad { rad ->
            BeregningPeriode(
                fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
                tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
                studienivå = parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad)
                    ?: Studienivå.HØYERE_UTDANNING,
                studieprosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
            )
        }.first()
    }

    @Når("beregner stønad for læremidler")
    fun `beregner stønad for læremidler`() {
        resultat = læremidlerBeregningService.beregn(beregningPeriode!!)
    }

    @Så("skal stønaden være")
    fun `skal stønaden være`(dataTable: DataTable) {
        val perioder = dataTable.mapRad { rad ->
            BeregningsresultatForMåned(
                beløp = parseBigDecimal(DomenenøkkelFelles.BELØP, rad),
                grunnlag = Beregningsgrunnlag(
                    måned = parseÅrMåned(BeregningNøkler.MÅNED, rad),
                    studienivå = parseValgfriEnum<Studienivå>(BeregningNøkler.STUDIENIVÅ, rad)
                        ?: Studienivå.HØYERE_UTDANNING,
                    studieprosent = parseInt(BeregningNøkler.STUDIEPROSENT, rad),
                    sats = parseBigDecimal(BeregningNøkler.SATS, rad).toInt(),
                ),
            )
        }
        val forventetBeregningsresultat = BeregningsresultatLæremidler(
            perioder = perioder,
        )
        assertThat(resultat).isEqualTo(forventetBeregningsresultat)
    }
}
