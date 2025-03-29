package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate

object StønadsperiodeValidering {
    /**
     * Validering av stønadsperioder som kjøres når man endrer vilkårperiode trenger ikke å validere fødselsdatoet.
     * Det er tilstrekkelig at det gjøres vid validering av stønadsperioder.
     */
    fun validerStønadsperioderVedEndringAvVilkårperiode(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: Vilkårperioder,
    ) = valider(stønadsperioder, vilkårperioder, null)

    /**
     * @param fødselsdato er nullable då alle behandlinger ikke har [fødselsdato] i grunnlagsdata fra før
     */
    fun valider(
        stønadsperioder: List<StønadsperiodeDto>,
        vilkårperioder: Vilkårperioder,
        fødselsdato: LocalDate?,
    ) {
        validerIkkeOverlappendeStønadsperioder(stønadsperioder)
        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteMålgrupper()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteAktiviteter()

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
        målgruppePerioderPerType: Map<MålgruppeType, List<Datoperiode>>,
        aktivitetPerioderPerType: Map<AktivitetType, List<Datoperiode>>,
        fødselsdato: LocalDate?,
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
                .filter { it.type.girIkkeRettPåStønadsperiode() && it.resultat != ResultatVilkårperiode.SLETTET }
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

/**
 *  @return En sortert map kategorisert på periodetype med de oppfylte vilkårsperiodene. Periodene slåes sammen dersom
 *  de er sammenhengende, også selv om de har overlapp.
 */
inline fun <reified T : VilkårperiodeType> List<Vilkårperiode>.mergeSammenhengendeOppfylte(): Map<T, List<Datoperiode>> =
    this
        .filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        .groupBy {
            require(it.type is T) { "${it.type} er ikke av type ${T::class.simpleName}" }
            it.type
        }.mapValues {
            it.value
                .sorted()
                .map { Datoperiode(fom = it.fom, tom = it.tom) }
                .mergeSammenhengende { a, b -> a.overlapperEllerPåfølgesAv(b) }
        }

fun List<Vilkårperiode>.mergeSammenhengendeOppfylteAktiviteter(): Map<AktivitetType, List<Datoperiode>> =
    this.mergeSammenhengendeOppfylte<AktivitetType>()

fun List<Vilkårperiode>.mergeSammenhengendeOppfylteMålgrupper(): Map<MålgruppeType, List<Datoperiode>> =
    this.mergeSammenhengendeOppfylte<MålgruppeType>()
