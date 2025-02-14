package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnVedtaksperiodeValidingerUtils.validerVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TilsynBarnVedtaksperiodeValidingerUtilsTest {
    @Nested
    inner class ValiderIngenOverlapp {
        val vedtaksperiodeJan =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
        val vedtaksperiodeFeb =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
        val vedtaksperiodeJanFeb =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )

        @Test
        fun `kaster feil hvis vedtaksperioderDto overlapper`() {
            val feil = assertThrows<ApiFeil> { validerVedtaksperioder(listOf(vedtaksperiodeJan, vedtaksperiodeJanFeb)) }
            assertThat(feil.feil).contains("Vedtaksperioder kan ikke overlappe")
        }

        @Test
        fun `kaster ikke feil hvis vedtaksperioderDto ikke overlapper`() {
            assertDoesNotThrow { validerVedtaksperioder(listOf(vedtaksperiodeJan, vedtaksperiodeFeb)) }
        }
    }
}
