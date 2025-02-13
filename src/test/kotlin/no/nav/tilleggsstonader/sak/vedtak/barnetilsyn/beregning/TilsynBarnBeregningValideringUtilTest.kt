package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningValideringUtil.validerIngenOverlapp
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TilsynBarnBeregningValideringUtilTest {
    @Nested
    inner class ValiderIngenOverlapp {
        val vedtaksperiodeJan =
            VedtaksperiodeDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                målgruppeType = MålgruppeType.AAP,
                aktivitetType = AktivitetType.TILTAK,
            )
        val vedtaksperiodeFeb =
            VedtaksperiodeDto(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppeType = MålgruppeType.AAP,
                aktivitetType = AktivitetType.TILTAK,
            )
        val vedtaksperiodeJanFeb =
            VedtaksperiodeDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
                målgruppeType = MålgruppeType.AAP,
                aktivitetType = AktivitetType.TILTAK,
            )

        @Test
        fun `kaster feil hvis vedtaksperioderDto overlapper`() {
            val feil = assertThrows<ApiFeil> { validerIngenOverlapp(listOf(vedtaksperiodeJan, vedtaksperiodeJanFeb)) }
            assertThat(feil.feil).contains("Vedtaksperioder kan ikke overlappe")
        }

        @Test
        fun `kaster ikke feil hvis vedtaksperioderDto ikke overlapper`() {
            assertDoesNotThrow { validerIngenOverlapp(listOf(vedtaksperiodeJan, vedtaksperiodeFeb)) }
        }
    }
}
