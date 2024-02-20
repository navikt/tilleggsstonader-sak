package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
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
    MÅNEDSBELØP("Månedsbeløp"),
    MAKSSATS("Makssats"),
}

class StepDefinitions {

    private val logger = LoggerFactory.getLogger(javaClass)

    val service = TilsynBarnBeregningService()

    var exception: Exception? = null

    var stønadsperioder = emptyList<Stønadsperiode>()
    var utgifter = mutableMapOf<UUID, List<Utgift>>()
    var aktiviteter = emptyList<TilsynBarnBeregningService.PeriodeMedDager>()
    var beregningsresultat: BeregningsresultatTilsynBarnDto? = null
    var totaltAntallDager: Int? = null

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

    @Gitt("følgende aktiviteter")
    fun `følgende aktiviteter`(dataTable: DataTable) {
        aktiviteter = mapAktivitetMedDager(dataTable)
    }

    private fun mapAktivitetMedDager(dataTable: DataTable) = dataTable.mapRad { rad ->
        TilsynBarnBeregningService.PeriodeMedDager(
            fom = parseÅrMånedEllerDato(DomenenøkkelFelles.FOM, rad).datoEllerFørsteDagenIMåneden(),
            tom = parseÅrMånedEllerDato(DomenenøkkelFelles.TOM, rad).datoEllerSisteDagenIMåneden(),
            dager = parseInt(DomenenøkkelFelles.AKTIVITETSDAGER, rad),
        )
    }

    @Når("beregner")
    fun `beregner`() {
        try {
            beregningsresultat = service.beregn(stønadsperioder, utgifter, aktiviteter)
            ekstraBeregninger()
        } catch (e: Exception) {
            exception = e
        }
    }

    // Kan slettes
    @Når("beregner for hele perioden")
    fun `beregner for hele perioden`() {
        try {
            totaltAntallDager = service.beregnHelePeriode(stønadsperioder, aktiviteter)
            println("Totalt antall dager: " + totaltAntallDager)
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
                månedsbeløp = parseValgfriInt(NøkkelBeregningTilsynBarn.MÅNEDSBELØP, rad),
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

                forventetResultat.månedsbeløp?.let {
                    assertThat(resultat.månedsbeløp)
                        .`as` { "totaltMånedsbeløp" }
                        .isEqualTo(it)
                }

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
                val acutal = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultat)
                logger.error("Feilet validering av rad ${index + 1} $acutal")
                throw e
            }
        }

        assertThat(perioder).hasSize(perioder.size)
    }

    private fun ekstraBeregninger() {
        val antallDagerTotaltIPeriode = service.beregnHelePeriode(stønadsperioder, aktiviteter)
        val antallDagerSummert = beregningsresultat!!.perioder.sumOf { it.grunnlag.antallDagerTotal }
        println("------------------------------")
        println("------------------------------")
        println("Aktivitetsdager = " + aktiviteter[0].dager)
        println("------------------------------")
        println("------------------------------")

        val v2 = service.antallDager2(aktiviteter)
        println("Antall dager summert i grunnlag: " + antallDagerSummert)
        println("Beregnet ant. dager i hele perioden: " + antallDagerTotaltIPeriode)
        println("Differanse dager: " + (antallDagerSummert - antallDagerTotaltIPeriode))
        println("Antall dager totalt v2 (sum): " + v2)
        println("Antall dager totalt v2: " + v2.entries.sumOf { it.value })

        val utgifter = beregningsresultat!!.perioder[1].grunnlag.utgifterTotal
        val dagsats = beregningsresultat!!.perioder[1].dagsats

        val totaltUtbetalt = beregningsresultat!!.perioder.sumOf { it.månedsbeløp }
        val utbetaltVedHelPeriode = dagsats.multiply(antallDagerTotaltIPeriode.toBigDecimal())
        val antallArbeidsdager = service.antallHverdager(stønadsperioder)

        val skalDekkes = utgifter.toBigDecimal().multiply(BigDecimal.valueOf(64)).divide(BigDecimal.valueOf(100))
        val gjennomsnittMnd = totaltUtbetalt / beregningsresultat!!.perioder.size

        println("------------------------------")
        println("Totalt utbetalt: " + totaltUtbetalt)
        println("Utbetalt ved beregning hel periode: " + utbetaltVedHelPeriode)
        println("Differanse utbetalt: " + (totaltUtbetalt.toBigDecimal() - utbetaltVedHelPeriode))
        println("Totalt utbetalt ved prosent: " + dagsats.multiply(aktiviteter[0].dager.toBigDecimal().divide(BigDecimal(5))).multiply(antallArbeidsdager.toBigDecimal()))

        println("------------------------------")
        println("64% av utgift i mnd: " + skalDekkes)
        println("Gjennomsnitt utbetalt i mnd: " + gjennomsnittMnd)
        println("Gjennomsnitt utbetalt i mnd vs total: " + (gjennomsnittMnd.toBigDecimal().minus(utbetaltVedHelPeriode/beregningsresultat!!.perioder.size.toBigDecimal())))
        println("Differanse: " + (gjennomsnittMnd.toBigDecimal()-skalDekkes))
    }

    @Så("forvent følgende summert antall dager: {}")
    fun `forvent følgende summert antall dager`(forventetSum: Int) {
        assertThat(beregningsresultat!!.perioder.sumOf { it.grunnlag.antallDagerTotal }).isEqualTo(forventetSum)
    }

    @Så("forvent følgende totalt antall dager: {}")
    fun `forvent følgende totalt antall dager`(forventetAntallDager: Int) {
        assertThat(totaltAntallDager).isEqualTo(forventetAntallDager)
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
    val månedsbeløp: Int?,
    val grunnlag: ForventetBeregningsgrunnlag,
)

data class ForventetBeregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int?,
    val antallDagerTotal: Int?,
    val utgifterTotal: Int?,
    val antallBarn: Int?,
)
