package no.nav.tilleggsstonader.sak.vilkår.regler

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.util.FileUtil.listFiles
import no.nav.tilleggsstonader.sak.util.FileUtil.readFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.name

internal class VilkårsregelTest {

    @Test
    internal fun `sjekker at output fortsatt er det samme på json`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }
        vilkårsregler.forEach {
            val json = objectWriter.writeValueAsString(it)
            // kommentere ut hvis regler har endret seg for å lagre de nye reglene
            // skrivTilFil(it, json)
            val fileJson = readFile("vilkårregler/${it.vilkårType}.json")
            assertThat(json).isEqualTo(fileJson)
        }
    }

    @Test
    fun `vilkårregler skal ikke uaktuelle regler`() {
        val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.map { it.value }

        assertThat(listFiles("vilkårregler").map { it.fileName.name })
            .containsExactlyInAnyOrderElementsOf(vilkårsregler.map { "${it.vilkårType}.json" }.toList())
    }

    @Suppress("unused")
    private fun skrivTilFil(it: Vilkårsregel, json: String) {
        val file = File("src/test/resources/vilkårregler/${it.vilkårType}.json")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(json)
    }

    @Test
    @Disabled
    internal fun `print alle vilkår`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        println(objectWriter.writeValueAsString(Vilkårsregler.ALLE_VILKÅRSREGLER))
    }
}
