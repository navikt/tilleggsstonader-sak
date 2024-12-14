package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.delTilUtbetalingsPerioder
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LæremidlerBeregningUtil2Test {

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var antallAvvik = 0
    val liste = mutableListOf<String>()

    @Test
    fun name() {
        val rader = FileUtil.readFile("laremidler_datoer.csv").split("\n")
            .drop(1)
        rader.forEach { rad ->
            //"VEDTAK_ID","VEDTAKTYPEKODE","FRA_DATO","TIL_DATO","TOTALBELOP","ANTALL_MND","OPPRINNELIG_TIL_DATO","ANTALL_DAGER"
            try {
                beregn(rad)
            } catch (e: Exception) {
                throw RuntimeException("Feil ved prosessering av $rad", e)
            }
        }
        println("Totalt antall: ${rader.size}")
        println("Antall Avvik: $antallAvvik")

        skriv("avvik_v2.txt")
    }

    @Test
    fun `finn liknende matcher`() {
        val v1 = finn("avvik_v1.txt")
        val v2 = finn("avvik_v2.txt")

        println()
        println("Comparing")
        val v1OgV2 = v1.filter { v2.contains(it) }
        println("Antall like=${v1OgV2.size}")

        val v1Unike = v1.filterNot { v2.contains(it) }
        val v2Unike = v2.filterNot { v1.contains(it) }
        println("V1 unike: ${v1Unike.size}")
        println("V2 unike: ${v2Unike.size}")

        println()
        println("v1")
        v1Unike.forEach { println(it) }
        println()
        println("v2")
        v2Unike.forEach { println(it) }

        println()
        println("V1 og V2")
        v1OgV2.forEach { println(it) }
    }

    private fun finn(filnavn: String): Set<String> {
        val rader = FileUtil.readFile(filnavn).split("\n")
        println("Filnavn: $filnavn")
        println("Antall rader=${rader.size}")
        println("Antall unike rader=${rader.distinct().size}")
        return rader.distinct().toSet()
    }

    private fun skriv(filnavn: String) {
        val file = File("src/test/resources/$filnavn")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeBytes(liste.joinToString("\n").toByteArray())
    }

    private fun beregn(rad: String) {
        val kolumner = rad.split(",")
        val vedtakId = kolumner[0]
        val type = kolumner[1].replace("\"", "")
        if (type == "S") {
            return
        }
        val fom = kolumner[2].let { LocalDate.parse(it, formatter) }
        val tom = kolumner[6].let { LocalDate.parse(it, formatter) }
        val antallMnd = kolumner[5].toInt()
        val beløp = kolumner[4].toInt()
        val periode = Vedtaksperiode(fom, tom)

        val utbetalingsperiode = periode.delTilUtbetalingsPerioder()
        if (utbetalingsperiode.size != antallMnd) {
            //println("vedtak=$vedtakId type=$type fom=$fom tom=$tom beløp=$beløp antallMnd=$antallMnd beregnet=${utbetalingsperiode.size}")
            val avvik = "fom=$fom tom=$tom antallMnd=$antallMnd beregnet=${utbetalingsperiode.size}"
            println(avvik)
            liste.add(avvik)
            antallAvvik++
        }
    }

    data class Vedtaksperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : Periode<LocalDate>
}

