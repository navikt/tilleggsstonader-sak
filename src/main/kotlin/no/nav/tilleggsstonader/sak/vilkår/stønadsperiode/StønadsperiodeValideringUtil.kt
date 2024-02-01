package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.formattertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.mergeSammenhengendeVilkårperioder

object StønadsperiodeValideringUtil {

    fun validerStønadsperioder(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
    ) {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeVilkårperioder()

        stønadsperioder.forEach { validerStønadsperiode(it, målgrupper, aktiviteter) }
    }

    private fun validerIkkeOverlappendeStønadsperioder(stønadsperioder: List<StønadsperiodeDto>) {
        stønadsperioder.sortedBy { it.fom }.fold(listOf<StønadsperiodeDto>()) { acc, periode ->
            val last = acc.lastOrNull()
            if (last != null) {
                feilHvis(last.tom >= periode.fom) {
                    "Stønadsperiode ${last.formattertPeriodeNorskFormat()} og ${periode.formattertPeriodeNorskFormat()} overlapper"
                }
            }
            acc + periode
        }
    }

    fun validerStønadsperiode(
        stønadsperiode: StønadsperiodeDto,
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
    ) {
        feilHvisIkke(stønadsperiode.målgruppe.gyldigeAktiviter.contains(stønadsperiode.aktivitet)) {
            "Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig"
        }

        val målgrupper = målgruppePerioderPerType[stønadsperiode.målgruppe]
            ?: error("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
        val aktiviteter = aktivitetPerioderPerType[stønadsperiode.aktivitet]
            ?: error("Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt")

        målgrupper.firstOrNull { it.inneholder(stønadsperiode) }
            ?: error("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.målgruppe} i perioden ${stønadsperiode.fom.norskFormat()} - ${stønadsperiode.tom.norskFormat()}")
        aktiviteter.firstOrNull { it.inneholder(stønadsperiode) }
            ?: error("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.aktivitet} i perioden ${stønadsperiode.fom.norskFormat()} - ${stønadsperiode.tom.norskFormat()}")
    }
}
