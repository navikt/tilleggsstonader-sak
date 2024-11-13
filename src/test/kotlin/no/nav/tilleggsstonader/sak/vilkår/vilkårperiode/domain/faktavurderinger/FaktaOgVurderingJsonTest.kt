package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import com.fasterxml.jackson.annotation.JsonSubTypes
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

class FaktaOgVurderingJsonTest {

    val jsonSubTypes = FaktaOgVurderingJson::class.findAnnotation<JsonSubTypes>()!!

    @Test
    fun `sjekk at alle TypeFaktaOgVurdering er mappet`() {
        assertThat(jsonSubTypes.value.map { it.name })
            .containsExactlyInAnyOrderElementsOf(alleEnumTyperFaktaOgVurdering.map { it.second.enumName() })
    }

    @Test
    fun `sjekk at det finnes json-tester for alle typer`() {
        assertThat(FaktaVurderingerJsonFilesUtil.jsonFiler.map { it.fileName })
            .containsExactlyInAnyOrderElementsOf(jsonSubTypes.value.map { "${it.name}.json" })
    }
}
