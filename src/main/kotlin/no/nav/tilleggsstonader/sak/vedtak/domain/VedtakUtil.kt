package no.nav.tilleggsstonader.sak.vedtak.domain

object VedtakUtil {
    inline fun <reified T : Vedtaksdata> GeneriskVedtak<*>.takeIfType(): GeneriskVedtak<T>? {
        @Suppress("UNCHECKED_CAST")
        return this.takeIf { it.data is T } as GeneriskVedtak<T>?
    }

    inline fun <reified T : Vedtaksdata> GeneriskVedtak<*>.withTypeOrThrow(): GeneriskVedtak<T> {
        require(this.data is T) {
            "Ugyldig data, er av type=${this.data::class.simpleName} forventet ${T::class.simpleName}"
        }
        @Suppress("UNCHECKED_CAST")
        return this as GeneriskVedtak<T>
    }
}
