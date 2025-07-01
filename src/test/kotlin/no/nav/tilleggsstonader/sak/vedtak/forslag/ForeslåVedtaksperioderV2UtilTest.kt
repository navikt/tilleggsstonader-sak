package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
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
        val avvik = mutableListOf<Avvik>()
        var antallRiktige = 0
        var antallMedOverlappende = 0
        fil.entries.forEach { (id, it) ->
            try {
                val forslagVedtaksperiode = when (it.stønadstype) {
                    Stønadstype.LÆREMIDLER -> ForeslåVedtaksperioderV2Util.forslagVedtaksperiodeForInngangsvilkår(
                        it.målgrupper,
                        it.aktiviteter.groupBy { it.type })

                    Stønadstype.BARNETILSYN,
                    Stønadstype.BOUTGIFTER -> ForeslåVedtaksperioderV2Util.foreslåPerioder(
                        it.målgrupper,
                        it.aktiviteter.groupBy { it.type },
                        it.vilkår
                    )
                }.sorted().map { ForenkletVedtaksperiode(it) }

                val faktiskeVedtaksperioder = it.vedtaksperioder.mergeSammenhengende(
                    skalMerges = { a, b ->
                        a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet && a.overlapperEllerPåfølgesAv(b)
                    },
                    merge = { a, b -> a.copy(fom = minOf(a.fom, b.fom), tom = maxOf(a.tom, b.tom)) }
                )

                if (forslagVedtaksperiode != faktiskeVedtaksperioder) {
                    val set = forslagVedtaksperiode.toSet()
                    val set2 = faktiskeVedtaksperioder.toSet()
                    avvik.add(
                        Avvik(
                            faktiske = faktiskeVedtaksperioder.filterNot { it in set && it in set2 },
                            foreslåtte = forslagVedtaksperiode.filterNot { it in set && it in set2 },
                            info = it
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

        val akko = avvik.map {
            val fomForeslått = it.foreslåtte.minOfOrNull { it.fom }
            val fomFaktisk = it.faktiske.minOfOrNull { it.fom }
            val fomDager = if (fomForeslått == null || fomFaktisk == null) -1 else
                ChronoUnit.DAYS.between(fomForeslått, fomFaktisk)

            val tomForeslått = it.foreslåtte.minOfOrNull { it.fom }
            val tomFaktisk = it.faktiske.minOfOrNull { it.fom }
            val tomDager = if (tomForeslått == null || tomFaktisk == null) -1 else
                ChronoUnit.DAYS.between(tomForeslått, tomFaktisk)
            Pair(fomDager, tomDager)
        }
        val fomDager = akko.map { it.first }.groupBy {
            when {
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
        val tomDager = akko.map { it.second }.groupBy {
            when {
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
            if(it.key == 101L) {
                println("over 100: ${it.value}")
                return@forEach
            } else {
                println("<= ${it.key}: ${it.value}")
            }
        }

        println("TOM")
        tomDager.forEach {
            if(it.key == 101L) {
                println("over 100: ${it.value}")
                return@forEach
            } else {
                println("<= ${it.key}: ${it.value}")
            }
        }

        File("src/test/resources/avvik.txt").apply {
            avvik.forEach {
                val årsaker = finnÅrsak(it)
                /*
                if(årsaker.contains("KUNNE_BEGYNT_FØR") || årsaker.contains("KUNNE_SLUTTET_SENERE")) {
                    return@forEach
                }
                 */
                appendText("Stønadstype: ${it.info.stønadstype}, behandlingId: ${it.info.behandlingId}\n")
                appendText("FORESLÅTT:\n")
                it.foreslåtte.forEach { appendText("$it\n") }
                appendText("FAKTISKE:\n")
                it.faktiske.forEach { appendText("$it\n") }
                appendText("Avvik: ${årsaker.joinToString(", ")}\n")
                appendText("\n")
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
                }
                if (foreslått.fom > faktisk.fom) {
                    årsak.add("FOM_ETTER_CHECK")
                }
                if (foreslått.tom > faktisk.tom) {
                    årsak.add("KUNNE_SLUTTET_SENERE")
                }
                if (foreslått.tom < faktisk.tom) {
                    årsak.add("TOM_FØR_CHECK")
                }
            } else {
                årsak.add("CHECK")
            }
        } else {
            årsak.add("MER_ENN_EN_PERIODE")
        }
        return årsak
    }

    data class Avvik(
        val faktiske: List<ForenkletVedtaksperiode>,
        val foreslåtte: List<ForenkletVedtaksperiode>,
        val info: Info,
    )

    val fil = FileUtil.readFile("Result_25.csv").split("\n").let {
        val map = mutableMapOf<BehandlingId, Info>()
        it
            .drop(1)
            .filter { it.isNotBlank() }
            .forEach { line ->
                val parts = line.split(",")
                try {
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
        val fom = LocalDate.parse(parts[3])
        val tom = LocalDate.parse(parts[4])
        val behandlingId = BehandlingId.fromString(parts[1])

        when (type) {
            "behandling" -> {
                val stønadstype = Stønadstype.valueOf(type2)
                map.put(behandlingId, Info(behandlingId, stønadstype))
            }

            "vilkarperiode" -> {
                val value = map.getValue(behandlingId)
                val typeVilkårperiode = vilkårperiodetyper[type2]!!
                when (typeVilkårperiode) {
                    is MålgruppeType -> value.målgrupper.add(
                        ForenkletVilkårperiode(
                            fom = fom,
                            tom = tom,
                            type = typeVilkårperiode.faktiskMålgruppe()
                        )
                    )

                    is AktivitetType -> value.aktiviteter.add(
                        ForenkletVilkårperiode(
                            fom = fom,
                            tom = tom,
                            type = typeVilkårperiode
                        )
                    )
                }
            }

            "vilkar" -> {
                val value = map.getValue(behandlingId)
                value.vilkår.add(Datoperiode(fom = fom, tom = tom))
            }

            "vedtaksperiode" -> {
                val value = map.getValue(behandlingId)
                value.vedtaksperioder.add(
                    ForenkletVedtaksperiode(
                        fom = fom,
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
    ) : Periode<LocalDate> {
        constructor(vedtaksperiode: Vedtaksperiode) : this(
            fom = vedtaksperiode.fom,
            tom = vedtaksperiode.tom,
            målgruppe = vedtaksperiode.målgruppe,
            aktivitet = vedtaksperiode.aktivitet
        )
    }
}