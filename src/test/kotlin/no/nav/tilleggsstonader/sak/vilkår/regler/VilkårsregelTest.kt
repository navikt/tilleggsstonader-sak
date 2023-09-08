package no.nav.tilleggsstonader.sak.vilkår.regler

import no.nav.tilleggsstonader.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

internal class VilkårsregelTest {

    @Test
    @Disabled // TOOO FIX
    internal fun `sjekker at output fortsatt er det samme på json`() {
        val objectWriter = objectMapper.writerWithDefaultPrettyPrinter()
        Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.forEach {
            val json = objectWriter.writeValueAsString(it.value)
            // kommentere ut hvis regler har endret seg for å lagre de nye reglene
            // skrivTilFil(it.value, json)
            val fileJson = readFile(it.value)
            assertThat(json).isEqualTo(fileJson)
        }
    }

    @Suppress("unused")
    private fun skrivTilFil(it: Vilkårsregel, json: String) {
        val file = File("src/test/resources/regler/${it.vilkårType}.json")
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

    private fun readFile(it: Vilkårsregel) =
        this::class.java.classLoader.getResource("regler/${it.vilkårType}.json")!!.readText()
}
