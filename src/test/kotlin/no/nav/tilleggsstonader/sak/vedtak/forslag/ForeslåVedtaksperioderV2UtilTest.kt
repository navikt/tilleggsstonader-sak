package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperioderV2Util.ForenkletVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/*
with q as (select 'behandling' type, b.id behandling_id, b.id, current_date, current_date, f.stonadstype, 'type2'
           from behandling b
                    join fagsak f on f.id = b.fagsak_id
           where status = 'FERDIGSTILT'
             AND b.resultat = 'INNVILGET')
select *
from q
union all
select 'arena_vedtak_tom',
       fg.behandling_id,
       fg.behandling_id,
       (fg.data ->> 'vedtakTom')::date,
       current_date,
       'type1',
       'type2'
from fakta_grunnlag fg
         join q q1 on q1.behandling_id = fg.behandling_id
WHERE fg.type = 'ARENA_VEDTAK_TOM'
union all
select 'vilkarperiode' type, vp.behandling_id, vp.id, vp.fom, vp.tom, vp.type, 'type2'
from vilkar_periode vp
         join q q1 on q1.behandling_id = vp.behandling_id
where vp.resultat = 'OPPFYLT'
union all
select 'vilkar', v.behandling_id, v.id, v.fom, v.tom, 'type', 'type2'
from vilkar v
         join q q1 on q1.behandling_id = v.behandling_id
where v.resultat = 'OPPFYLT'
union all
select 'vedtaksperiode',
       v.behandling_id,
       (vedtaksperiode ->> 'id')::uuid,
       (vedtaksperiode ->> 'fom')::date,
       (vedtaksperiode ->> 'tom')::date,
       vedtaksperiode ->> 'målgruppe',
       vedtaksperiode ->> 'aktivitet'
from vedtak v
         join q q1 on q1.behandling_id = v.behandling_id,
     jsonb_array_elements(data -> 'vedtaksperioder') vedtaksperiode

 */
class ForeslåVedtaksperioderV2UtilTest {

    @Test
    fun abc() {
        printTotalAntallDagerAvvik(avvik)

        fil.values.groupBy { it.stønadstype }
            .mapValues { it.value.count() }
            .entries.sortedBy { it.key }
            .forEach { (stønadstype, antall) ->
                println("Stønadstype: $stønadstype, Antall: $antall")
            }

        val ikkeKunSlutter30Juni = avvik.filterNot {
            val finnÅrsak = finnÅrsak(it)
            finnÅrsak.size == 2 && finnÅrsak.contains("FAKTISK_SLUTTER_30_JUNI")
        }

        avvik.filterNot {
            val finnÅrsak = finnÅrsak(it)
            finnÅrsak.contains("FAKTISK_SLUTTER_30_JUNI")
        }.groupBy { it.info.stønadstype }
            .mapValues { it.value.count() }
            .forEach {
                println("Stønadstype: ${it.key}, Antall avvik med FAKTISK_SLUTTER_30_JUNI: ${it.value}")
            }

        ikkeKunSlutter30Juni.groupBy { it.info.stønadstype }
            .values.forEach { avvikPerStønad ->
                println("")
                println("Stønadstype: ${avvikPerStønad.first().info.stønadstype}")
                printTotalAntallDagerAvvik(avvikPerStønad)
            }

        //printTilFil(avvik.filter { it.info.stønadstype == Stønadstype.BARNETILSYN })
    }

    @Test
    fun `print info til noen`() {
        avvik.filter { it.antallDagerAvvik.fom < 5 }
            .filter { it.info.stønadstype != Stønadstype.LÆREMIDLER }
            .forEach { printInfo(it) }
    }

    @Test
    fun `printInfo til behandling`() {
        val behandlingId = BehandlingId.fromString("f5c6bbb7-b035-48ae-9358-2a6228b302c2")
        printInfo(avvik.single { it.info.behandlingId == behandlingId })
    }

    private fun printInfo(avvik: Avvik) {
        val info = avvik.info
        println("##############")
        println("##############")
        println("BehandlingId: ${info.behandlingId} stønadstype=${info.stønadstype}")
        println("fomAvvik= ${avvik.antallDagerAvvik.fom}, tomAvvik=${avvik.antallDagerAvvik.tom}")
        println("Årsaker: ${finnÅrsak(avvik).joinToString(", ")}")
        println("ArenaTom: ${info.arenaTom}")
        println()
        println("Målgrupper:")
        info.målgrupper.forEach {
            println("${it.type}, fom: ${it.fom}, tom: ${it.tom}")
        }
        println("Aktiviteter:")
        info.aktiviteter.forEach {
            println("${it.type}, fom: ${it.fom}, tom: ${it.tom}")
        }
        println("Vilkår:")
        info.vilkår.forEach {
            println("fom: ${it.fom}, tom: ${it.tom}")
        }
        println("Vedtaksperioder:")
        info.vedtaksperioder.forEach {
            println("målgruppe: ${it.målgruppe}, aktivitet: ${it.aktivitet}, fom: ${it.fom}, tom: ${it.tom}")
        }

        println("\nForslag:")
        avvik.foreslåtte.forEach {
            println("målgruppe: ${it.målgruppe}, aktivitet: ${it.aktivitet}, fom: ${it.fom}, tom: ${it.tom}")
        }
        println()
    }

    private fun mapForslag(info: Info): List<ForenkletVedtaksperiode> = when (info.stønadstype) {
        Stønadstype.LÆREMIDLER -> ForeslåVedtaksperioderV2Util.forslagVedtaksperiodeForInngangsvilkår(
            info.målgrupper,
            info.aktiviteter.groupBy { it.type })

        Stønadstype.BARNETILSYN,
        Stønadstype.BOUTGIFTER -> ForeslåVedtaksperioderV2Util.foreslåPerioder(
            info.målgrupper,
            info.aktiviteter.groupBy { it.type },
            info.vilkår
        )

        Stønadstype.DAGLIG_REISE_TSO -> TODO()
        Stønadstype.DAGLIG_REISE_TSR -> TODO()
    }.sorted().map { ForenkletVedtaksperiode(it) }
        .let {
            if (info.arenaTom != null) {
                it.avkortPerioderFør(info.arenaTom?.plusDays(1))
            } else {
                it
            }
        }

    private fun printTilFil(avvik: List<Avvik>) {
        File("src/test/resources/avvik.txt").apply {
            avvik.forEach {
                val dagerAvvik = it.antallDagerAvvik
                val årsaker = finnÅrsak(it)

                appendText("Stønadstype: ${it.info.stønadstype}, behandlingId: ${it.info.behandlingId}\n")
                appendText("FORESLÅTT:\n")
                it.foreslåtte.forEach { appendText("$it\n") }
                appendText("FAKTISKE:\n")
                it.faktiske.forEach { appendText("$it\n") }
                appendText("Avvik: ${årsaker.joinToString(", ")}\n")
                appendText("Dager avvik: fom=${dagerAvvik.fom}, tom=${dagerAvvik.tom}\n")
                it.info.arenaTom?.let { appendText("ArenaTom=$it\n") }
                appendText("\n")
            }
        }
    }

    private fun printTotalAntallDagerAvvik(avvik: List<Avvik>) {
        val totaltAvvik = avvik.map { it.antallDagerAvvik }
        val fomDager = totaltAvvik.map { it.fom }.groupBy {
            when {
                it < -1 -> -2
                it <= 1 -> it
                it <= 5 -> 5
                it <= 10 -> 10
                it <= 20 -> 20
                it <= 35 -> 35
                it <= 50 -> 50
                it <= 100 -> 100
                else -> 101
            }
        }.mapValues { it.value.size }.entries.sortedBy { it.key }
        val tomDager = totaltAvvik.map { it.tom }.groupBy {
            when {
                it < -1 -> -2
                it <= 1 -> it
                it <= 5 -> 5
                it <= 10 -> 10
                it <= 20 -> 20
                it <= 35 -> 35
                it <= 50 -> 50
                it <= 100 -> 100
                else -> 101
            }
        }.mapValues { it.value.size }.entries.sortedBy { it.key }

        println("FOM")
        fomDager.forEach {
            if (it.key == 101L) {
                println("over 100: ${it.value}")
                return@forEach
            } else {
                println("<= ${it.key}: ${it.value}")
            }
        }

        println("TOM")
        tomDager.forEach {
            if (it.key == 101L) {
                println("over 100: ${it.value}")
                return@forEach
            } else {
                println("<= ${it.key}: ${it.value}")
            }
        }
    }

    private fun finnÅrsak(avvik1: Avvik): MutableList<String> {
        val årsak = mutableListOf<String>()
        if (avvik1.foreslåtte.size == 1 && avvik1.faktiske.size == 1) {
            val foreslått = avvik1.foreslåtte[0]
            val faktisk = avvik1.faktiske[0]
            if (foreslått.overlapper(faktisk)) {
                if (foreslått.fom < faktisk.fom) {
                    årsak.add("KUNNE_BEGYNT_FØR")
                    if (faktisk.fom == avvik1.info.arenaTom?.plusDays(1)) {
                        årsak.add("FAKTISK_BEGYNNER_DAGEN_ETTER_ARENA_TOM")
                    }
                }
                if (foreslått.tom > faktisk.tom) {
                    årsak.add("KUNNE_SLUTTET_SENERE")
                    if (faktisk.tom.month == Month.JUNE && faktisk.tom.dayOfMonth == 30) {
                        årsak.add("FAKTISK_SLUTTER_30_JUNI")
                    } else {
                        årsak.add("FAKTISK_SLUTTER_ANNET_DATO")
                    }
                }

                // skal ikke skje
                if (foreslått.fom > faktisk.fom || foreslått.tom < faktisk.tom) {
                    årsak.add("CHECK")
                }
            } else {
                // skal ikke skje
                årsak.add("CHECK")
            }
        } else {
            årsak.add("MER_ENN_EN_PERIODE")
        }
        return årsak
    }

    val fil = readFile()

    val avvik = finnAvvik()

    fun finnAvvik(): List<Avvik> {
        val avvik = mutableListOf<Avvik>()
        var antallRiktige = 0
        var antallMedOverlappende = 0
        fil.entries.forEach { (id, info) ->
            try {
                val forslagVedtaksperiode = mapForslag(info)

                val faktiskeVedtaksperioder = info.vedtaksperioder.mergeSammenhengende(
                    skalMerges = { a, b ->
                        a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet && a.overlapperEllerPåfølgesAv(b)
                    },
                    merge = { a, b -> a.copy(fom = minOf(a.fom, b.fom), tom = maxOf(a.tom, b.tom)) }
                )

                if (forslagVedtaksperiode != faktiskeVedtaksperioder) {
                    // mulighet for å filtrere vekk de perioder som finnes i begge settene
                    val set = forslagVedtaksperiode.toSet()
                    val set2 = faktiskeVedtaksperioder.toSet()
                    avvik.add(
                        Avvik(
                            faktiske = faktiskeVedtaksperioder
                            //.filterNot { it in set && it in set2 }
                            ,
                            foreslåtte = forslagVedtaksperiode
                            //.filterNot { it in set && it in set2 }
                            ,
                            info = info
                        )
                    )
                } else {
                    antallRiktige++
                }
            } catch (e: Exception) {
                if (e.message!!.contains("Foreløpig klarer vi bare å foreslå perioder når målgruppe og aktivitet har ett sammenhengende overlapp")) {
                    antallMedOverlappende++
                } else {
                    throw e
                }
            }
        }
        println("Antall riktige: $antallRiktige")
        println("Antall overlapp: $antallMedOverlappende")
        println("Antall avvik: ${avvik.size}")
        println("Antall med vedtak i Arena=${fil.values.count { it.arenaTom != null }}")
        return avvik
    }

    data class AntallDagerAvvik(
        val fom: Long,
        val tom: Long,
    )

    data class Avvik(
        val faktiske: List<ForenkletVedtaksperiode>,
        val foreslåtte: List<ForenkletVedtaksperiode>,
        val info: Info,
    ) {

        val antallDagerAvvik = run {
            val fomForeslått = foreslåtte.minOfOrNull { it.fom }
            val fomFaktisk = faktiske.minOfOrNull { it.fom }
            val fomDager = if (fomForeslått == null || fomFaktisk == null) -1 else
                ChronoUnit.DAYS.between(fomForeslått, fomFaktisk)

            val tomForeslått = foreslåtte.maxOfOrNull { it.tom }
            val tomFaktisk = faktiske.maxOfOrNull { it.tom }
            val tomDager = if (tomForeslått == null || tomFaktisk == null) -1 else
                ChronoUnit.DAYS.between(tomFaktisk, tomForeslått)
            AntallDagerAvvik(fomDager, tomDager)
        }
    }

    private fun readFile(): MutableMap<BehandlingId, Info> = FileUtil.readFile("Result_25.csv").split("\n").let {
        val map = mutableMapOf<BehandlingId, Info>()
        it
            .drop(1)
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->
                val parts = line.split(",")
                try {
                    //println("$index $line")
                    map(parts, map)
                } catch (e: Throwable) {
                    println("Feil ved parsing av linje: $line")
                    throw e
                }
            }
        map
    }

    private fun map(
        parts: List<String>,
        map: MutableMap<BehandlingId, Info>
    ) {
        val type = parts[0]
        val type2 = parts[5]
        val type3 = parts[6]
        val fomEllerArenaTom = parts[3].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val tom = LocalDate.parse(parts[4])
        val behandlingId = BehandlingId.fromString(parts[1])

        when (type) {
            "behandling" -> {
                val stønadstype = Stønadstype.valueOf(type2)
                map.put(behandlingId, Info(behandlingId, stønadstype))
            }

            "arena_vedtak_tom" -> {
                map.getValue(behandlingId).arenaTom = fomEllerArenaTom
            }

            "vilkarperiode" -> {
                val value = map.getValue(behandlingId)
                val typeVilkårperiode = vilkårperiodetyper[type2]!!
                when (typeVilkårperiode) {
                    is MålgruppeType -> value.målgrupper.add(
                        ForenkletVilkårperiode(
                            fom = fomEllerArenaTom!!,
                            tom = tom,
                            type = typeVilkårperiode.faktiskMålgruppe()
                        )
                    )

                    is AktivitetType -> value.aktiviteter.add(
                        ForenkletVilkårperiode(
                            fom = fomEllerArenaTom!!,
                            tom = tom,
                            type = typeVilkårperiode
                        )
                    )
                }
            }

            "vilkar" -> {
                val value = map.getValue(behandlingId)
                value.vilkår.add(Datoperiode(fom = fomEllerArenaTom!!, tom = tom))
            }

            "vedtaksperiode" -> {
                val value = map.getValue(behandlingId)
                value.vedtaksperioder.add(
                    ForenkletVedtaksperiode(
                        fom = fomEllerArenaTom!!,
                        tom = tom,
                        målgruppe = FaktiskMålgruppe.valueOf(type2),
                        aktivitet = AktivitetType.valueOf(type3)
                    )
                )
            }
        }
    }

    data class Info(
        val behandlingId: BehandlingId,
        val stønadstype: Stønadstype,
        var arenaTom: LocalDate? = null,
        val målgrupper: MutableList<ForenkletVilkårperiode<FaktiskMålgruppe>> = mutableListOf(),
        val aktiviteter: MutableList<ForenkletVilkårperiode<AktivitetType>> = mutableListOf(),
        val vilkår: MutableList<Datoperiode> = mutableListOf(),
        val vedtaksperioder: MutableList<ForenkletVedtaksperiode> = mutableListOf()
    )

    data class ForenkletVedtaksperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: FaktiskMålgruppe,
        val aktivitet: AktivitetType,
    ) : Periode<LocalDate>, KopierPeriode<ForenkletVedtaksperiode> {
        constructor(vedtaksperiode: Vedtaksperiode) : this(
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            målgruppe = vedtaksperiode.målgruppe,
            aktivitet = vedtaksperiode.aktivitet
        )

        override fun medPeriode(
            fom: LocalDate,
            tom: LocalDate
        ): ForenkletVedtaksperiode {
            return this.copy(fom = fom, tom = tom)
        }
    }
}