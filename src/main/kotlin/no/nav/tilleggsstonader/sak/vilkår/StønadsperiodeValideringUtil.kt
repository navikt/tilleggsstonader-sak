package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto

object StønadsperiodeValideringUtil {
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
