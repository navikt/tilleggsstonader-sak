package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

object FaktaOgVurderingUtil {

    inline fun <reified T : Vurderinger> Vurderinger.takeIfVurderinger() =
        takeIf { it is T }?.let { it as T }

    inline fun <reified T : Fakta> Fakta.takeIfFakta() =
        takeIf { it is T }?.let { it as T }

    inline fun <reified T : Fakta> Fakta.takeIfFaktaOrThrow(): T {
        require(this is T) {
            "Ugyldig fakta, er av type=${this::class.simpleName} forventet ${T::class.simpleName}"
        }
        return this
    }

    inline fun <reified T : Vurderinger> Vurderinger.takeIfVurderingOrThrow(): T {
        require(this is T) {
            "Ugyldig vurdering, er av type=${this::class.simpleName} forventet ${T::class.simpleName}"
        }
        return this
    }
}
