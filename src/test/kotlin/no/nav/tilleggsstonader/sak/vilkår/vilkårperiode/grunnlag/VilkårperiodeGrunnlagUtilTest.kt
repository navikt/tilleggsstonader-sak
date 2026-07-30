package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeGrunnlagUtilTest {
    @Nested
    inner class KanYtelseBrukesIBehandling {
        val ytelse =
            PeriodeGrunnlagYtelse.AAP(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 1),
            )

        @Test
        fun `skal ikke kunne bruke AAP ferdig avklart`() {
            val ytelseAAPFerdigAvklart = ytelse.copy(subtype = PeriodeGrunnlagYtelse.YtelseSubtype.AAP_FERDIG_AVKLART)

            assertThat(kanYtelseBrukesIBehandling(Stønadstype.LÆREMIDLER, ytelseAAPFerdigAvklart)).isFalse
        }

        @Test
        fun `skal kunne bruke AAP for pass av barn`() {
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.BARNETILSYN, ytelse)).isTrue
        }

        @Test
        fun `skal kunne bruke dagpenger for daglig reise tsr`() {
            val ytelseDagpenger = PeriodeGrunnlagYtelse.Dagpenger(fom = ytelse.fom, tom = ytelse.tom)
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.DAGLIG_REISE_TSR, ytelseDagpenger)).isTrue
        }

        @Test
        fun `skal ikke kunne bruke dagpenger for daglig reise tso`() {
            val ytelseDagpenger = PeriodeGrunnlagYtelse.Dagpenger(fom = ytelse.fom, tom = ytelse.tom)
            assertThat(kanYtelseBrukesIBehandling(Stønadstype.DAGLIG_REISE_TSO, ytelseDagpenger)).isFalse
        }
    }
}
