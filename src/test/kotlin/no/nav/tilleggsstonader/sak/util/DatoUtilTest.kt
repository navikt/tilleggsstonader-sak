package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    @Nested
    inner class FørsteDagIMpneden {

        @Test
        fun `erFørsteDagIMåneden skal sjekke om dagen er siste dag i måneden`() {
            assertThat(LocalDate.of(2024, 1, 1).erFørsteDagIMåneden()).isTrue
            assertThat(LocalDate.of(2024, 2, 1).erFørsteDagIMåneden()).isTrue

            assertThat(LocalDate.of(2024, 1, 31).erFørsteDagIMåneden()).isFalse
            assertThat(LocalDate.of(2024, 3, 30).erFørsteDagIMåneden()).isFalse
        }
    }

    @Nested
    inner class FørsteDagIMåneden {

        @Test
        fun `tilFørsteDagIMåneden skal endre dato til første dagen i måneden for inneværende måned`() {
            val førsteJan2024 = LocalDate.of(2024, 1, 1)
            assertThat(førsteJan2024.tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 1, 31).tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 1, 30).tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 2, 4).tilFørsteDagIMåneden()).isEqualTo(LocalDate.of(2024, 2, 1))
        }
    }

    @Nested
    inner class SisteDagIMåneden {

        @Test
        fun `erSisteDagIMåneden skal sjekke om dagen er siste dag i måneden`() {
            assertThat(LocalDate.of(2024, 1, 31).erSisteDagIMåneden()).isTrue
            assertThat(LocalDate.of(2024, 2, 1).erSisteDagIMåneden()).isFalse
            assertThat(LocalDate.of(2024, 3, 30).erSisteDagIMåneden()).isFalse
        }

        @Test
        fun `tilSisteDagIMåneden skal endre dato til siste dagen i måneden for inneværende måned`() {
            assertThat(LocalDate.of(2024, 1, 31).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(LocalDate.of(2024, 1, 30).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(LocalDate.of(2024, 2, 4).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 2, 29))
        }
    }

    @Nested
    inner class LørdagEllerSøndag {
        @Test
        fun `ukesdager skal gi false`() {
            assertThat(LocalDate.of(2025, 1, 1).lørdagEllerSøndag()).isFalse() // onsdag rød dag
            assertThat(LocalDate.of(2025, 1, 2).lørdagEllerSøndag()).isFalse() // torsdag
            assertThat(LocalDate.of(2025, 1, 3).lørdagEllerSøndag()).isFalse() // fredag

            assertThat(LocalDate.of(2025, 2, 3).lørdagEllerSøndag()).isFalse() // mandag
            assertThat(LocalDate.of(2025, 2, 4).lørdagEllerSøndag()).isFalse() // tirsdag
        }

        @Test
        fun `lørdag og søndag skal gi true`() {
            assertThat(LocalDate.of(2025, 1, 4).lørdagEllerSøndag()).isTrue() // lørdag
            assertThat(LocalDate.of(2025, 1, 5).lørdagEllerSøndag()).isTrue() // søndag
        }
    }
}
