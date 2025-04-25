package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import kotlin.reflect.KClass

sealed interface FaktaGrunnlagData : FaktaGrunnlagDataJson {
    val type: TypeFaktaGrunnlag
}

enum class TypeFaktaGrunnlag(
    val kClass: KClass<out FaktaGrunnlagData>,
) {
    PERSONOPPLYSNINGER(FaktaGrunnlagPersonopplysninger::class),
    BARN_ANDRE_FORELDRE_SAKSINFORMASJON(FaktaGrunnlagBarnAndreForeldreSaksinformasjon::class),
    ARENA_VEDTAK_TOM(FaktaGrunnlagArenaVedtak::class),
    ;

    companion object {
        private val perType = entries.associateBy(TypeFaktaGrunnlag::kClass)

        fun finnType(type: KClass<out FaktaGrunnlagData>) = perType[type] ?: error("Finner ikke type for ${type.simpleName}")
    }
}
