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
import no.nav.tilleggsstonader.sak.cucumber.parseValgfriInt
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMåned
import no.nav.tilleggsstonader.sak.cucumber.parseÅrMånedEllerDato
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth
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
        val forventetBeregningsresultat = dataTable.mapRad { rad ->
            ForventetBeregningsresultat(
                dagsats = parseBigDecimal(NøkkelBeregningTilsynBarn.DAGSATS, rad),
                grunnlag = ForventetBeregningsgrunnlag(
                    måned = parseÅrMåned(NøkkelBeregningTilsynBarn.MÅNED, rad),
                    makssats = parseValgfriInt(NøkkelBeregningTilsynBarn.MAKSSATS, rad),
                    antallDagerTotal = parseValgfriInt(NøkkelBeregningTilsynBarn.ANTALL_DAGER, rad),
                    utgifterTotal = parseValgfriInt(NøkkelBeregningTilsynBarn.UTGIFT, rad),
                    antallBarn = parseValgfriInt(NøkkelBeregningTilsynBarn.ANTALL_BARN, rad),
                ),
            )
        }

        val perioder = beregningsresultat!!.perioder
        perioder.forEachIndexed { index, resultat ->
            val forventetResultat = forventetBeregningsresultat[index]
            try {
                assertThat(resultat.dagsats)
                    .`as` { "dagsats" }
                    .isEqualTo(forventetResultat.dagsats)
                forventetResultat.grunnlag.antallDagerTotal?.let {
                    assertThat(resultat.grunnlag.antallDagerTotal)
                        .`as` { "antallDagerTotal" }
                        .isEqualTo(it)
                }

                forventetResultat.grunnlag.utgifterTotal?.let {
                    assertThat(resultat.grunnlag.utgifterTotal)
                        .`as` { "utgifterTotal" }
                        .isEqualTo(it)
                }

                forventetResultat.grunnlag.makssats?.let {
                    assertThat(resultat.grunnlag.makssats)
                        .`as` { "makssats" }
                        .isEqualTo(it)
                }
            } catch (e: Throwable) {
                logger.error("Feilet validering av rad ${index + 1}")
                throw e
            }
        }

        assertThat(perioder).hasSize(perioder.size)
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

data class ForventetBeregningsresultat(
    val dagsats: BigDecimal,
    val grunnlag: ForventetBeregningsgrunnlag,
)

data class ForventetBeregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int?,
    val antallDagerTotal: Int?,
    val utgifterTotal: Int?,
    val antallBarn: Int?,
)
