package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilårperiodeGrunnlagUtilTest {
    @Nested
    inner class KanYtelseBrukesIBehandling {
        val ytelse =
            PeriodeGrunnlagYtelse(
                type = TypeYtelsePeriode.AAP,
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 1),
            )

        @Test
        fun `skal ikke kunne bruke AAP ferdig avklart`() {
            val ytelseAAPFerdigAvklart = ytelse.copy(subtype = PeriodeGrunnlagYtelse.YtelseSubtype.AAP_FERDIG_AVKLART)

            assertThat(kanYtelseBrukesIBehandling(Stønadstype.LÆREMIDLER, ytelseAAPFerdigAvklart)).isFalse
        }

        @Test
        fun `skal kunne bruke AAP for tilsyn barn`() {
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.BARNETILSYN, ytelse)).isTrue
        }

        @Test
        fun `skal kunne bruke dagpenger for daglig reise tsr`() {
            val ytelseDagpenger = ytelse.copy(type = TypeYtelsePeriode.DAGPENGER)
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.DAGLIG_REISE_TSR, ytelseDagpenger)).isTrue
        }

        @Test
        fun `skal ikke kunne bruke dagpenger for daglig reise tso`() {
            val ytelseDagpenger = ytelse.copy(type = TypeYtelsePeriode.DAGPENGER)
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.DAGLIG_REISE_TSO, ytelseDagpenger)).isFalse
        }
    }
}
