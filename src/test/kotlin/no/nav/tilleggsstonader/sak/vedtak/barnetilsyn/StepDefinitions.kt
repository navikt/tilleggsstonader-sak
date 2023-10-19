package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.sak.cucumber.Domenenøkkel
import no.nav.tilleggsstonader.sak.cucumber.DomenenøkkelFelles
import no.nav.tilleggsstonader.sak.cucumber.IdTIlUUIDHolder.barnIder
import no.nav.tilleggsstonader.sak.cucumber.mapRad
import no.nav.tilleggsstonader.sak.cucumber.parseBigDecimal
import no.nav.tilleggsstonader.sak.cucumber.parseInt
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.util.UUID

private enum class NøkkelBeregningTilsynBarn(
    override val nøkkel: String,
) : Domenenøkkel {
    MÅNED("Måned"),
    ANTALL_DAGER("Antall dager"),
    ANTALL_BARN("Antall barn"),
    UTGIFT("Utgift"),
    DAGSATS("Dagsats"),
    MAKSSATS("Makssats"),
}

class StepDefinitions {

    private val logger = LoggerFactory.getLogger(javaClass)

    val service = TilsynBarnBeregningService()

    var exception: Exception? = null

    var stønadsperioder = emptyList<Stønadsperiode>()
    var utgifter = mutableMapOf<UUID, List<Utgift>>()
    var beregningsresultat: BeregningsresultatTilsynBarnDto? = null

    @Gitt("følgende støndsperioder")
    fun `følgende støndsperioder`(dataTable: DataTable) {
        stønadsperioder = mapStønadsperider(dataTable)
    }

    private fun mapStønadsperider(dataTable: DataTable) = dataTable.mapRad { rad ->
        Stønadsperiode(
            fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
            tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
        )
    }

    @Gitt("følgende utgifter for barn med id: {}")
    fun `følgende utgifter`(barnId: Int, dataTable: DataTable) {
        val barnUuid = barnIder[barnId]!!
        assertThat(utgifter).doesNotContainKey(barnUuid)
        utgifter[barnUuid] = dataTable.mapRad { rad ->
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
            beregningsresultat = service.beregn(stønadsperioder, utgifter)
        } catch (e: Exception) {
            exception = e
        }
    }

    @Så("forvent følgende feil: {}")
    fun `forvent følgende feil`(forventetFeil: String) {
        assertThat(exception!!).hasMessageContaining(forventetFeil)
    }

    @Så("forvent følgende beregningsresultat")
    fun `forvent følgende beregningsresultat`(dataTable: DataTable) {
        assertThat(exception).isNull()
        val forventetBeregningsresultat = BeregningsresultatTilsynBarnDto(
            perioder = dataTable.mapRad { rad ->
                Beregningsresultat(
                    dagsats = parseBigDecimal(NøkkelBeregningTilsynBarn.DAGSATS, rad),
                    grunnlag = Beregningsgrunnlag(
                        måned = parseÅrMåned(NøkkelBeregningTilsynBarn.MÅNED, rad),
                        makssats = parseInt(NøkkelBeregningTilsynBarn.MAKSSATS, rad),
                        stønadsperioder = emptyList(),
                        utgifter = emptyList(),
                        antallDagerTotal = parseInt(NøkkelBeregningTilsynBarn.ANTALL_DAGER, rad),
                        utgifterTotal = parseInt(NøkkelBeregningTilsynBarn.UTGIFT, rad),
                        antallBarn = parseInt(NøkkelBeregningTilsynBarn.ANTALL_BARN, rad),
                    ),
                )
            },
        )

        val perioder = beregningsresultat!!.perioder
        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventetBeregningsresultat.perioder[index]
            try {
                assertThat(resultat.dagsats)
                    .`as` { "dagsats" }
                    .isEqualTo(forventetResultat.dagsats)
                assertThat(resultat.grunnlag.antallDagerTotal)
                    .`as` { "antallDagerTotal" }
                    .isEqualTo(forventetResultat.grunnlag.antallDagerTotal)
                assertThat(resultat.grunnlag.utgifterTotal)
                    .`as` { "utgifterTotal" }
                    .isEqualTo(forventetResultat.grunnlag.utgifterTotal)
                assertThat(resultat.grunnlag.makssats)
                    .`as` { "makssats" }
                    .isEqualTo(forventetResultat.grunnlag.makssats)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }

        assertThat(perioder).hasSize(forventetBeregningsresultat.perioder.size)
    }

    @Så("forvent følgende stønadsperioder for: {}")
    fun `forvent følgende stønadsperioder`(månedStr: String, dataTable: DataTable) {
        assertThat(exception).isNull()
        val måned = parseÅrMåned(månedStr)
        val forventeteStønadsperioder = mapStønadsperider(dataTable)

        val perioder = beregningsresultat!!.perioder.find { it.grunnlag.måned == måned }
            ?.grunnlag?.stønadsperioder
            ?: error("Finner ikke beregningsresultat for $måned")

        forventeteStønadsperioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventeteStønadsperioder[index]
            try {
                assertThat(resultat.fom).`as` { "fom" }.isEqualTo(forventetResultat.fom)
                assertThat(resultat.tom).`as` { "tom" }.isEqualTo(forventetResultat.tom)
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }

        assertThat(perioder).containsExactlyElementsOf(forventeteStønadsperioder)
    }
}
