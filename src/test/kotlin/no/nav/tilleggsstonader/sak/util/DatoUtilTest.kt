package no.nav.tilleggsstonader.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DatoUtilTest {

    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn dagens dato`() {
        assertThat(antallÅrSiden(LocalDate.now())).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere null hvis vi sender inn null`() {
        assertThat(antallÅrSiden(null)).isNull()
    }

    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn morgendagens dato for ett år siden`() {
        val morgendagensDatoIFjor = LocalDate.now().minusYears(1).plusDays(1)
        assertThat(antallÅrSiden(morgendagensDatoIFjor)).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere 1 hvis vi sender inn gårsdagens dato for ett år siden`() {
        val gårsdagensDatoIFjor = LocalDate.now().minusYears(1).minusDays(1)
        assertThat(antallÅrSiden(gårsdagensDatoIFjor)).isEqualTo(1)
    }
}
