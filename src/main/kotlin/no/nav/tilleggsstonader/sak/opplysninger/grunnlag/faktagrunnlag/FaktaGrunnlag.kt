package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktaGrunnlagId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import kotlin.reflect.KClass

typealias FaktaGrunnlag = GeneriskFaktaGrunnlag<out FaktaGrunnlagData>

/**
 * [behandlingId], [type] og [typeId] er unik sammen
 * @param typeId er en ekstra identifikator, eks ident på barnet for [TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON]
 */
@Table("fakta_grunnlag")
data class GeneriskFaktaGrunnlag<T : FaktaGrunnlagData>(
    @Id
    val id: FaktaGrunnlagId = FaktaGrunnlagId.random(),
    val behandlingId: BehandlingId,
    val data: T,
    val type: TypeFaktaGrunnlag = data.type,
    val typeId: String?,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        feilHvis(data.type != type) {
            "Type=$type er ikke lik type i data (${data.type})"
        }
    }
}

sealed interface FaktaGrunnlagData : FaktaGrunnlagDataJson {
    val type: TypeFaktaGrunnlag
}

enum class TypeFaktaGrunnlag(
    val kClass: KClass<out FaktaGrunnlagData>,
) {
    BARN_ANDRE_FORELDRE_SAKSINFORMASJON(FaktaGrunnlagBarnAndreForeldreSaksinformasjon::class),
    ;

    companion object {
        private val perType = entries.associateBy(TypeFaktaGrunnlag::kClass)

        fun finnType(type: KClass<out FaktaGrunnlagData>) = perType[type] ?: error("Finner ikke type for ${type.simpleName}")
    }
}
