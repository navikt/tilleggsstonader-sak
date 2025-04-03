package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe

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

fun List<Vilkårperiode>.mergeSammenhengendeOppfylteMålgrupper(): Map<FaktiskMålgruppe, List<Datoperiode>> =
    this.mergeSammenhengendeOppfylteFaktiskeMålgrupper()

fun List<Vilkårperiode>.mergeSammenhengendeOppfylteFaktiskeMålgrupper(): Map<FaktiskMålgruppe, List<Datoperiode>> =
    this
        .filter { it.resultat == ResultatVilkårperiode.OPPFYLT }
        .groupBy {
            require(it.type is MålgruppeType) { "${it.type} er ikke av type ${MålgruppeType::class.simpleName}" }
            it.type.faktiskMålgruppe()
        }.mapValues {
            it.value
                .sorted()
                .map { Datoperiode(fom = it.fom, tom = it.tom) }
                .mergeSammenhengende { a, b -> a.overlapperEllerPåfølgesAv(b) }
        }
