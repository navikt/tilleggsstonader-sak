package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vedtak.dto.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeTest {
    val fom = LocalDate.of(2025, 1, 1)
    val tom = LocalDate.of(2025, 3, 1)
    val målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
    val aktivitet = AktivitetType.TILTAK
    val uuid = UUID.randomUUID()

    val vedtaksperiode =
        listOf(
            VedtaksperiodeBeregning(
                fom = fom,
                tom = tom,
                målgruppe = målgruppe,
                aktivitet = aktivitet,
            ),
        )

    @Test
    fun `mapper VedtaksperiodeDto til VedtaksperiodeBeregningsgrunnlag`() {
        val vedtaksperiodeDto =
            listOf(
                VedtaksperiodeDto(
                    id = uuid,
                    fom = fom,
                    tom = tom,
                    målgruppeType = målgruppe,
                    aktivitetType = aktivitet,
                ),
            )

        assertThat(vedtaksperiodeDto.tilVedtaksperiodeBeregning()).isEqualTo(vedtaksperiode)
    }
}
