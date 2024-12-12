package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import java.time.LocalDate

object StønadsperiodeValidering {

    /**
     * Validering av stønadsperioder som kjøres når man endrer vilkårperiode trenger ikke å validere fødselsdatoet.
     * Det er tilstrekkelig at det gjøres vid validering av stønadsperioder.
     */
    fun validerStønadsperioderVedEndringAvVilkårperiode(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
    ) = valider(stønadsperioder, vilkårperioder, null)

    /**
     * @param fødselsdato er nullable då alle behandlinger ikke har [fødselsdato] i grunnlagsdata fra før
     */
    fun valider(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: VilkårperioderDto,
        fødselsdato: LocalDate?,
    ) {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteVilkårperioder()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteVilkårperioder()

        validerAtStønadsperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, stønadsperioder)
        stønadsperioder.forEach { validerEnkeltperiode(it, målgrupper, aktiviteter, fødselsdato) }
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
        målgruppePerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<VilkårperiodeType, List<Datoperiode>>,
        fødselsdato: LocalDate?,
    ) {
        brukerfeilHvisIkke(stønadsperiode.målgruppe.gyldigeAktiviter.contains(stønadsperiode.aktivitet)) {
            "Kombinasjonen av ${stønadsperiode.målgruppe} og ${stønadsperiode.aktivitet} er ikke gyldig"
        }

        val målgrupper = målgruppePerioderPerType[stønadsperiode.målgruppe]?.takeIf { it.isNotEmpty() }
            ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.målgruppe} er oppfylt")
        val aktiviteter = aktivitetPerioderPerType[stønadsperiode.aktivitet]?.takeIf { it.isNotEmpty() }
            ?: brukerfeil("Finner ingen perioder hvor vilkår for ${stønadsperiode.aktivitet} er oppfylt")

        målgrupper.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.målgruppe} i perioden ${stønadsperiode.formatertPeriodeNorskFormat()}")
        aktiviteter.firstOrNull { it.inneholder(stønadsperiode) }
            ?: brukerfeil("Finnes ingen periode med oppfylte vilkår for ${stønadsperiode.aktivitet} i perioden ${stønadsperiode.formatertPeriodeNorskFormat()}")

        validerStønadsperiodeErInnenfor18og67år(fødselsdato, stønadsperiode)
    }

    /**
     * Stønadsperioder kan ikke overlappe med stønadsperiode som ikke gir rett på stønadsperiode,
     * eks 100% sykepenger eller INGEN_MÅLGRUPPE
     */
    private fun validerAtStønadsperioderIkkeOverlapperMedVilkårPeriodeUtenRett(
        vilkårperioder: VilkårperioderDto,
        stønadsperioder: List<StønadsperiodeDto>,
    ) {
        val perioderSomIkkeGirRett = (vilkårperioder.målgrupper + vilkårperioder.aktiviteter)
            .filter { it.type.girIkkeRettPåStønadsperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
        stønadsperioder.forEach { validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(perioderSomIkkeGirRett, it) }
    }

    private fun validerIkkeOverlapperMedPeriodeSomIkkeGirRettPåStønad(
        vilkårperioder: List<VilkårperiodeDto>,
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

    private fun validerStønadsperiodeErInnenfor18og67år(
        fødselsdato: LocalDate?,
        stønadsperiode: StønadsperiodeDto,
    ) {
        if (fødselsdato != null && stønadsperiode.målgruppe.gjelderNedsattArbeidsevne()) {
            val dato18år = fødselsdato.plusYears(18)
            brukerfeilHvis(stønadsperiode.fom < dato18år) {
                "Periode kan ikke begynne før søker fyller 18 år (${dato18år.norskFormat()})"
            }
            val dato67år = fødselsdato.plusYears(67)
            brukerfeilHvis(stønadsperiode.tom >= dato67år) {
                "Periode kan ikke slutte etter søker fylt 67 år (${dato67år.norskFormat()})"
            }
        }
    }
}

/**
 *  @return En sortert map kategorisert på periodetype med de oppfylte vilkårsperiodene. Periodene slåes sammen dersom
 *  de er sammenhengende, også selv om de har overlapp.
 */
fun List<VilkårperiodeDto>.mergeSammenhengendeOppfylteVilkårperioder(): Map<VilkårperiodeType, List<Datoperiode>> {
    return this.sorted().filter { it.resultat == ResultatVilkårperiode.OPPFYLT }.groupBy { it.type }
        .mapValues {
            it.value.map { Datoperiode(it.fom, it.tom) }
                .mergeSammenhengende { a, b -> a.overlapper(b) || a.tom.plusDays(1) == b.fom }
        }
}
