package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilVedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.tilVedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeTest {
    val fom = LocalDate.of(2025, 1, 1)
    val tom = LocalDate.of(2025, 3, 1)
    val målgruppe = MålgruppeType.AAP
    val aktivitet = AktivitetType.TILTAK

    val vedtaksperiode =
        listOf(
            Vedtaksperiode(
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
                    fom = fom,
                    tom = tom,
                    målgruppeType = målgruppe,
                    aktivitetType = aktivitet,
                ),
            )

        assertThat(vedtaksperiodeDto.tilVedtaksperiode()).isEqualTo(vedtaksperiode)
    }

    @Test
    fun `mapper StønadsperiodeBeregningsgrunnlag til VedtaksperiodeBeregningsgrunnlag`() {
        val stønadsperiodeBeregningsgrunnlag =
            listOf(
                stønadsperiode(
                    behandlingId = BehandlingId.random(),
                    fom = fom,
                    tom = tom,
                    målgruppe = målgruppe,
                    aktivitet = aktivitet,
                ),
            )
        assertThat(stønadsperiodeBeregningsgrunnlag.tilVedtaksperiode()).isEqualTo(
            vedtaksperiode,
        )
    }
}
