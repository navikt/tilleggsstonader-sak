package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.libs.utils.CollectionUtil.singleOrNullOrError

object FaktaGrunnlagUtil {
    inline fun <reified T : FaktaGrunnlagData> GeneriskFaktaGrunnlag<*>.takeIfType(): GeneriskFaktaGrunnlag<T>? {
        @Suppress("UNCHECKED_CAST")
        return this.takeIf { it.data is T } as GeneriskFaktaGrunnlag<T>?
    }

    inline fun <reified T : FaktaGrunnlagData> GeneriskFaktaGrunnlag<*>.withTypeOrThrow(): GeneriskFaktaGrunnlag<T> {
        require(this.data is T) {
            "Ugyldig data, er av type=${this.data::class.simpleName} forventet ${T::class.simpleName}"
        }
        @Suppress("UNCHECKED_CAST")
        return this as GeneriskFaktaGrunnlag<T>
    }

    inline fun <reified T : FaktaGrunnlagData> List<GeneriskFaktaGrunnlag<*>>.ofType(): List<GeneriskFaktaGrunnlag<T>> {
        @Suppress("UNCHECKED_CAST")
        return this.filter { it.data is T } as List<GeneriskFaktaGrunnlag<T>>
    }

    inline fun <reified T : FaktaGrunnlagData> List<GeneriskFaktaGrunnlag<*>>.singleOfType(): GeneriskFaktaGrunnlag<T> =
        ofType<T>().singleOrNullOrError() ?: error("Finner ikke fakta av type ${T::class.simpleName}")
}

sealed interface FaktaGrunnlagOpprettResultat {
    data object Opprettet : FaktaGrunnlagOpprettResultat

    data object IkkeOpprettet : FaktaGrunnlagOpprettResultat
}
