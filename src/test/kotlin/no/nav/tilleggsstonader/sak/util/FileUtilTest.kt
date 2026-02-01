package no.nav.tilleggsstonader.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FileUtilTest {
    @Test
    fun `skal ikke sjekke inn SKRIV_TIL_FIL=true`() {
        assertThat(FileUtil.SKRIV_TIL_FIL).isFalse()
    }
}
