package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

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
}
