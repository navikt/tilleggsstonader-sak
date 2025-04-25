package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktaGrunnlagId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagData
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

typealias FaktaGrunnlag = GeneriskFaktaGrunnlag<out FaktaGrunnlagData>

/**
 * [behandlingId], [type] og [typeId] er unik sammen
 * @param typeId er en ekstra identifikator, eks ident på barnet for [no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON]
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
