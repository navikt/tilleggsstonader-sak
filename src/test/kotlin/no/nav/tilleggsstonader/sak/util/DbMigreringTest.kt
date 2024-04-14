package no.nav.tilleggsstonader.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.toPath

class DbMigreringTest {
    @Test
    fun `migreringsscript skal inneholde 2 underscores og endte på sql`() {
        val regex = """V(\d+)__.*\.sql""".toRegex()
        val directory = this::class.java.classLoader.getResource("db/migration")
        val filer = Files.list(directory!!.toURI().toPath()).toList()
            .map { it.fileName.toString() }

        assertThat(filer).hasSizeGreaterThan(33)
        assertThat(filer.filterNot { it.matches(regex) }).isEmpty()
    }
}