package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.splitPerMåned
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.YearMonth
import java.util.UUID

object TilsynBeregningUtil {
    /**
     * Deler opp  stønadsperioder i atomiske deler (mnd).
     *
     * Eksempel 1:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=31.03.24)) deles opp i 3:
     * jan -> listOf(StønadsperiodeDto(jan), feb -> StønadsperiodeDto(feb), mar -> StønadsperiodeDto(mars))
     *
     * Eksempel 2:
     * listOf(StønadsperiodeDto(fom = 01.01.24, tom=10.01.24), StønadsperiodeDto(fom = 20.01.24, tom=31.01.24)) deles opp i 2 innenfor samme måned:
     * jan -> listOf(StønadsperiodeDto(fom = 01.01.24, tom = 10.01.24), StønadsperiodeDto(fom = 20.01.24, tom = 31.01.24))
     */
    fun List<StønadsperiodeDto>.tilÅrMåned(): Map<YearMonth, List<StønadsperiodeDto>> {
        return this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
    }

    /**
     * Splitter opp utgifter slik at de blir fordelt per måned og grupperer de etter måned.
     * Resultatet gir en map med måned som key og en liste av utgifter.
     * Listen med utgifter består av utgiften knyttet til et barn for gitt måned.
     */
    fun Map<UUID, List<Utgift>>.tilÅrMåned(): Map<YearMonth, List<UtgiftBarn>> {
        return this.entries.flatMap { (barnId, utgifter) ->
            utgifter.flatMap { utgift -> utgift.splitPerMåned { _, periode -> UtgiftBarn(barnId, periode.utgift) } }
        }.groupBy({ it.first }, { it.second })
    }

    /**
     * Deler opp aktiviteter i atomiske deler (mnd) og grupperer aktivitetene per AktivitetType.
     */
    fun List<Aktivitet>.tilAktiviteterPerMånedPerType(): Map<YearMonth, Map<AktivitetType, List<Aktivitet>>> {
        return this
            .flatMap { stønadsperiode ->
                stønadsperiode.splitPerMåned { måned, periode ->
                    periode.copy(
                        fom = maxOf(periode.fom, måned.atDay(1)),
                        tom = minOf(periode.tom, måned.atEndOfMonth()),
                    )
                }
            }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.groupBy { it.type } }
    }
}
