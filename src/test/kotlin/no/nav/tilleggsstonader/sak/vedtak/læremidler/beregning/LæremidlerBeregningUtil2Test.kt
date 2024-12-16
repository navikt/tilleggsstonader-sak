package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.libs.utils.VirkedagerProvider
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.tilSisteDagIMåneden
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
    fun `antall som begynner og slutter i samma dato`() {
        val rader = FileUtil.readFile("laremidler_datoer.csv").split("\n")
            .drop(1)
        rader.forEach { rad ->
            //"VEDTAK_ID","VEDTAKTYPEKODE","FRA_DATO","TIL_DATO","TOTALBELOP","ANTALL_MND","OPPRINNELIG_TIL_DATO","ANTALL_DAGER"
            try {
                val kolumner = rad.split(",")
                val type = kolumner[1].replace("\"", "")
                if (type == "S") {
                    return
                }
                val fom = kolumner[2].let { LocalDate.parse(it, formatter) }
                val tom = kolumner[6].let { LocalDate.parse(it, formatter) }
                val antallMnd = kolumner[5].toInt()

                if (fom.dayOfMonth == fom.lengthOfMonth() && tom.dayOfMonth == tom.lengthOfMonth()) {
                    println("fom=$fom tom=$tom")
                }

                if (fom.dayOfMonth == tom.dayOfMonth) {
                    println("fom=$fom tom=$tom")
                }

            } catch (e: Exception) {
                throw RuntimeException("Feil ved prosessering av $rad", e)
            }
        }
    }

    @Test
    fun `arena-beregning antall mnd`() {
        val rader = FileUtil.readFile("laremidler_datoer.csv").split("\n")
            .drop(1)
        rader.forEach { rad ->
            //"VEDTAK_ID","VEDTAKTYPEKODE","FRA_DATO","TIL_DATO","TOTALBELOP","ANTALL_MND","OPPRINNELIG_TIL_DATO","ANTALL_DAGER"
            try {
                val kolumner = rad.split(",")
                val vedtakId = kolumner[0]
                val type = kolumner[1].replace("\"", "")
                if (type == "S") {
                    return
                }
                val fom = kolumner[2].let { LocalDate.parse(it, formatter) }
                val tom = kolumner[6].let { LocalDate.parse(it, formatter) }
                val antallMnd = kolumner[5].toInt()

                val utbetalingsperiode = Vedtaksperiode(fom, tom).split2MedHelg { fom, tom -> Vedtaksperiode(fom, tom) }

                if (utbetalingsperiode.size != antallMnd) {
                    //println("vedtak=$vedtakId type=$type fom=$fom tom=$tom beløp=$beløp antallMnd=$antallMnd beregnet=${utbetalingsperiode.size}")
                    val avvik = "fom=$fom tom=$tom antallMnd=$antallMnd beregnet=${utbetalingsperiode.size}"
                    println(avvik)
                    liste.add(avvik)
                    antallAvvik++
                }


            } catch (e: Exception) {
                throw RuntimeException("Feil ved prosessering av $rad", e)
            }
        }
    }

    @Test
    fun testSplit() {
        val vedtaksperiode = Vedtaksperiode(
            LocalDate.parse("2024-01-10"),
            LocalDate.parse("2024-02-11")
        )
        vedtaksperiode.split { fom, tom -> Vedtaksperiode(fom, tom) }.forEach { println(it) }
        println("...")
        vedtaksperiode.split2 { fom, tom -> Vedtaksperiode(fom, tom) }.forEach { println(it) }
        println("...")
        vedtaksperiode.v2MedHelg { fom, tom -> Vedtaksperiode(fom, tom) }.forEach { println(it) }
    }

    @Test
    fun testSplit2() {
        val vedtaksperiode = Vedtaksperiode(
            LocalDate.parse("2023-01-25"),
            LocalDate.parse("2023-06-25")
        )
        vedtaksperiode.split { fom, tom -> Vedtaksperiode(fom, tom) }.forEach { println(it) }
        println("...")
        vedtaksperiode.split2 { fom, tom -> Vedtaksperiode(fom, tom) }.forEach { println(it) }
    }

    fun <P : Periode<LocalDate>, VAL> P.split(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom = if (gjeldendeFom.dayOfMonth == gjeldendeFom.lengthOfMonth()) {
                gjeldendeFom.plusMonths(1).tilSisteDagIMåneden()
            } else {
                gjeldendeFom.plusMonths(1)
            }.minusDays(1)
                .let { minOf(it, tom) }

            perioder.add(medNyPeriode(gjeldendeFom, nyTom))

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun <P : Periode<LocalDate>, VAL> P.split2(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom =
                if (fom.dayOfMonth == fom.lengthOfMonth()) {
                    gjeldendeFom.plusMonths(1).tilSisteDagIMåneden()
                } else {
                    gjeldendeFom.plusMonths(1)
                }.minusDays(1)
                    .let { minOf(it, tom) }

            perioder.add(medNyPeriode(gjeldendeFom, nyTom))

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun <P : Periode<LocalDate>> P.split2MedHelg(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom =
                if (fom.dayOfMonth == fom.lengthOfMonth()) {
                    gjeldendeFom.plusMonths(1).tilSisteDagIMåneden()
                } else {
                    gjeldendeFom.plusMonths(1)
                }.minusDays(1)
                    .let { minOf(it, tom) }

            val element = medNyPeriode(gjeldendeFom, nyTom)

            if(element.alleDatoer().any { !VirkedagerProvider.erHelgEllerHelligdag(it) }) {
                perioder.add(element)
            }

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    fun <P : Periode<LocalDate>> P.v2MedHelg(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldendeFom = fom
        while (gjeldendeFom <= tom) {
            val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)

            val element = medNyPeriode(gjeldendeFom, nyTom)
            if (element.alleDatoer().any { !VirkedagerProvider.erHelgEllerHelligdag(it) }) {
                perioder.add(element)

            }

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
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

