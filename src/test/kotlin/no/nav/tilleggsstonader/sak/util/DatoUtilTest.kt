package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class DatoUtilTest {

    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn dagens dato`() {
        assertThat(antallÅrSiden(osloDateNow())).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere null hvis vi sender inn null`() {
        assertThat(antallÅrSiden(null)).isNull()
    }

    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn morgendagens dato for ett år siden`() {
        val morgendagensDatoIFjor = osloDateNow().minusYears(1).plusDays(1)
        assertThat(antallÅrSiden(morgendagensDatoIFjor)).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere 1 hvis vi sender inn gårsdagens dato for ett år siden`() {
        val gårsdagensDatoIFjor = osloDateNow().minusYears(1).minusDays(1)
        assertThat(antallÅrSiden(gårsdagensDatoIFjor)).isEqualTo(1)
    }

    @Test
    fun `nesteMandagHvisHelg skal håndtere dagens dato hvis lørdag eller søndag`() {
        val april = YearMonth.of(2024, 4)
        listOf(
            april.atDay(1) to april.atDay(1),
            april.atDay(2) to april.atDay(2),
            april.atDay(3) to april.atDay(3),
            april.atDay(4) to april.atDay(4),
            april.atDay(5) to april.atDay(5),
            april.atDay(6) to april.atDay(8),
            april.atDay(7) to april.atDay(8),
        ).forEach {
            assertThat(it.first.datoEllerNesteMandagHvisLørdagEllerSøndag()).isEqualTo(it.second)
        }
    }
}
