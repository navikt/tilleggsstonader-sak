package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.FileUtil.listFiles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.io.path.name

internal class VilkårsregelTest {
    /*
     * Denne testen feiler hvis regel-treet endrer seg.
     * Var det meningen? I så fall kan du fikse det ved å sette SKAL_SKRIVE_TIL_FIL = true i FileUtil.kt.
     * Husk å sette tilbake til false etter du har verifisert at testen kjører grønt.
     */
    @Test
    internal fun `sjekker at output fortsatt er det samme på json`() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }
        vilkårsregler.forEach {
            assertFileIsEqual("vilkår/regler/${it.vilkårType}.json", it)
        }
    }

    @Test
    fun `vilkårregler skal ikke uaktuelle regler`() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }

        assertThat(listFiles("vilkår/regler").map { it.fileName.name })
            .containsExactlyInAnyOrderElementsOf(vilkårsregler.map { "${it.vilkårType}.json" }.toList())
    }

    @Test
    @Disabled
    internal fun `print alle vilkår`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        println(objectWriter.writeValueAsString(Vilkårsregler.ALLE_VILKÅRSREGLER))
    }
}
