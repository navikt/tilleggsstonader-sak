package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Stønadsperiodefeil
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.dto.formattertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.dto.mergeSammenhengendeVilkårperioder
import java.util.UUID

/**
 * 2 ulike måter å returnere feil på
 */

/**
 * Mulighet 1, mapper feil direkt inn i StønadsperiodeDto
 */
data class LagretStønadsperioderResponse(
    val harFeil: Boolean,
    val stønadsperioder: List<StønadsperiodeDto>
)

/**
 * Mulighet 2, returnerer liste med ugyldige perioder
 * Hvis det finnes ugyldige perioder så lagres ikke dataen ned
 */
data class LagretStønadsperioderResponse2(
    val stønadsperioder: List<StønadsperiodeDto>,
    val ugyldigePerioder: List<Stønadsperiodefeil>
)

object StønadsperiodeValideringUtil {

    /**
     * Gir null hvis det ikke finnes noen feil
     */
    fun validerStønadsperioder(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: Vilkårperioder,
    ): List<StønadsperiodeDto> {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)?.let { return it }

        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeVilkårperioder()

        return stønadsperioder.mapNotNull { stønadsperiode ->
            validerStønadsperiode(stønadsperiode, målgrupper, aktiviteter)
                ?.let { StønadsperiodeDto(stønadsperiode.id ?: UUID.randomUUID(), it) }
        }.takeIf { it.isNotEmpty() }
    }

    private fun validerIkkeOverlappendeStønadsperioder(stønadsperioder: List<StønadsperiodeDto>): List<StønadsperiodeDto>? {
        val feil = mutableListOf<StønadsperiodeDto>()
        stønadsperioder.sortedBy { it.fom }.fold(listOf<StønadsperiodeDto>()) { acc, periode ->
            val last = acc.lastOrNull()
            if (last != null) {
                if (last.tom >= periode.fom) {
                    feil.add(
                        periode.copy(feil = "Stønadsperiode ${last.formattertPeriodeNorskFormat()} og ${periode.formattertPeriodeNorskFormat()} overlapper")
                    )
                }
            }
            acc + periode
        }
        return feil.takeIf { it.isNotEmpty() }
    }

    fun validerStønadsperiode(
        stønadsperiode: StønadsperiodeDto,
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
    ): String? {
        if (!stønadsperiode.målgruppe.gyldigeAktiviter.contains(stønadsperiode.aktivitet)) {
            return "Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig"
        }

        val målgrupper = målgruppePerioderPerType[stønadsperiode.målgruppe]
            ?: return "Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt"
        val aktiviteter = aktivitetPerioderPerType[stønadsperiode.aktivitet]
            ?: return "Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt"

        målgrupper.firstOrNull { it.inneholder(stønadsperiode) }
            ?: return "Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.målgruppe} i perioden ${stønadsperiode.formattertPeriodeNorskFormat()}"
        aktiviteter.firstOrNull { it.inneholder(stønadsperiode) }
            ?: return "Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.aktivitet} i perioden ${stønadsperiode.formattertPeriodeNorskFormat()}"
        return null
    }
}
