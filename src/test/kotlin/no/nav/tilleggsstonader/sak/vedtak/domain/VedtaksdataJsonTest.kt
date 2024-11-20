package no.nav.tilleggsstonader.sak.vedtak.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

class VedtaksdataJsonTest {
    val jsonSubTypes = VedtaksdataJson::class.findAnnotation<JsonSubTypes>()!!

    @Test
    fun `sjekk at alle VedtakJson er mappet`() {
        assertThat(jsonSubTypes.value.map { it.name })
            .containsExactlyInAnyOrderElementsOf(alleEnumTypeVedtaksdata.map { it.second.enumName() })
    }

    @Test
    fun `sjekk at det finnes json-tester for alle typer`() {
        assertThat(VedtaksdataFilesUtil.jsonFiler.map { it.fileName })
            .containsExactlyInAnyOrderElementsOf(jsonSubTypes.value.map { "${it.name}.json" })
    }
}
