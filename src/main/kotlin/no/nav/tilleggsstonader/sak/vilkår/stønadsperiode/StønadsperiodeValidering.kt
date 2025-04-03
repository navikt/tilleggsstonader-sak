package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteMålgrupper

object StønadsperiodeValidering {
    /**
     * @param fødselsdato er nullable då alle behandlinger ikke har [fødselsdato] i grunnlagsdata fra før
     */
    fun valider(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: Vilkårperioder,
    ) {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteMålgrupper()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteAktiviteter()

        validerAtStønadsperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, stønadsperioder)
        stønadsperioder.forEach { validerEnkeltperiode(it, målgrupper, aktiviteter) }
    }

    private fun validerIkkeOverlappendeStønadsperioder(stønadsperioder: List<StønadsperiodeDto>) {
        stønadsperioder.sortedBy { it.fom }.fold(listOf<StønadsperiodeDto>()) { acc, periode ->
            val last = acc.lastOrNull()
            if (last != null) {
                brukerfeilHvis(last.tom >= periode.fom) {
                    "Stønadsperiode ${last.formatertPeriodeNorskFormat()} og ${periode.formatertPeriodeNorskFormat()} overlapper"
                }
            }
            acc + periode
        }
    }

    fun validerEnkeltperiode(
        stønadsperiode: StønadsperiodeDto,
        målgruppePerioderPerType: Map<MålgruppeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<AktivitetType, List<Datoperiode>>,
    ) {
        brukerfeilHvisIkke(stønadsperiode.målgruppe.gyldigeAktiviter.contains(stønadsperiode.aktivitet)) {
            "Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig"
        }

        val målgrupper =
            målgruppePerioderPerType[stønadsperiode.målgruppe]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
        val aktiviteter =
            aktivitetPerioderPerType[stønadsperiode.aktivitet]?.takeIf { it.isNotEmpty() }
                ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt")

        målgrupper.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.målgruppe} i perioden ${stønadsperiode.formatertPeriodeNorskFormat()}",
            )
        aktiviteter.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil(
                "Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.aktivitet} i perioden ${stønadsperiode.formatertPeriodeNorskFormat()}",
            )
    }

    /**
     * Stønadsperioder kan ikke overlappe med stønadsperiode som ikke gir rett på stønadsperiode,
     * eks 100% sykepenger eller INGEN_MÅLGRUPPE
     */
    private fun validerAtStønadsperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
        vilkårperioder: Vilkårperioder,
        stønadsperioder: List<StønadsperiodeDto>,
    ) {
        val perioderSomIkkeGirRett =
            (vilkårperioder.målgrupper + vilkårperioder.aktiviteter)
                .filter { it.type.girIkkeRettPåVedtaksperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
        stønadsperioder.forEach { validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(perioderSomIkkeGirRett, it) }
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        vilkårperioder: List<Vilkårperiode>,
        stønadsperiode: StønadsperiodeDto,
    ) {
        vilkårperioder
            .firstOrNull { vilkårperiode -> vilkårperiode.overlapper(stønadsperiode) }
            ?.let {
                brukerfeil(
                    "Stønadsperiode ${stønadsperiode.formatertPeriodeNorskFormat()} overlapper " +
                        "med ${it.type}(${it.formatertPeriodeNorskFormat()}) som ikke gir rett på stønad",
                )
            }
    }
}
