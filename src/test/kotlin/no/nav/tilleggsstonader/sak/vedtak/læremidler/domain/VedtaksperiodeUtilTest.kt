package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {
    val behandlingId = BehandlingId(UUID.randomUUID())
    val vedtaksperiodeJanuar =
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            status = VedtaksperiodeStatus.NY,
        )
    val vedtaksperiodeFebruar =
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 28),
            status = VedtaksperiodeStatus.NY,
        )

    val stønadsperiodeJanTilFeb =
        stønadsperiode(
            behandlingId = behandlingId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 2, 28),
        ).tilStønadsperiodeBeregningsgrunnlag()

    @Nested
    inner class ValiderVedtaksperioder {
        @Test
        fun `Kaster ikke feil ved gyldig data`() {
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }
        }

        @Test
        fun `Overlappende vedtaksperioder kaster feil`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeJanuar,
                    vedtaksperiodeFebruar.copy(fom = LocalDate.of(2024, 1, 31)),
                )
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertThatThrownBy {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
            val vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2024, 1, 15),
                        tom = LocalDate.of(2024, 2, 14),
                        status = VedtaksperiodeStatus.NY,
                    ),
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2024, 2, 15),
                        tom = LocalDate.of(2024, 2, 28),
                        status = VedtaksperiodeStatus.NY,
                    ),
                )
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }
        }

        @Test
        fun `Vedtaksperiode ikke innenfor en stønadsperiode kaster feil`() {
            val behandlingId = BehandlingId(UUID.randomUUID())
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)
            val stønadsperioder =
                listOf(
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = LocalDate.of(2024, 1, 2),
                        tom = LocalDate.of(2024, 1, 31),
                    ).tilStønadsperiodeBeregningsgrunnlag(),
                )

            assertThatThrownBy {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }.hasMessageContaining("Vedtaksperiode er ikke innenfor en overlappsperiode")
        }
    }

    @Nested
    inner class VedtaksperioderInnenforLøpendeMåned {
        @Test
        fun `skal ikke avkorte vedtaksperiode hvis den omslutes av beregningsgrunnlag`() {
            val vedtaksperiodeId = UUID.randomUUID()
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        Vedtaksperiode(
                            id = vedtaksperiodeId,
                            fom = LocalDate.of(2024, 1, 5),
                            tom = LocalDate.of(2024, 1, 10),
                            status = VedtaksperiodeStatus.NY,
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    fom = LocalDate.of(2024, 1, 5),
                    tom = LocalDate.of(2024, 1, 10),
                    status = VedtaksperiodeStatus.NY,
                ),
            )
        }

        @Test
        fun `skal avkorte vedtaksperiode hvis den er lengre enn beregningsgrunnlag`() {
            val vedtaksperiodeId = UUID.randomUUID()
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        Vedtaksperiode(
                            id = vedtaksperiodeId,
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 2, 29),
                            status = VedtaksperiodeStatus.NY,
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 15), tom = LocalDate.of(2024, 2, 14)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = LocalDate.of(2024, 1, 15),
                    tom = LocalDate.of(2024, 2, 14),
                    status = VedtaksperiodeStatus.NY,
                ),
            )
        }

        @Test
        fun `skal returnere alle perioder innenfor et beregningsgrunnlag`() {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        Vedtaksperiode(
                            id = UUID.randomUUID(),
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 1),
                            status = VedtaksperiodeStatus.NY,
                        ),
                        Vedtaksperiode(
                            id = id1,
                            fom = LocalDate.of(2024, 1, 2),
                            tom = LocalDate.of(2024, 1, 2),
                            status = VedtaksperiodeStatus.NY,
                        ),
                        Vedtaksperiode(
                            id = id2,
                            fom = LocalDate.of(2024, 1, 3),
                            tom = LocalDate.of(2024, 1, 3),
                            status = VedtaksperiodeStatus.NY,
                        ),
                        Vedtaksperiode(
                            id = UUID.randomUUID(),
                            fom = LocalDate.of(2024, 1, 4),
                            tom = LocalDate.of(2024, 1, 4),
                            status = VedtaksperiodeStatus.NY,
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 3)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(id = id1, fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 2), status = VedtaksperiodeStatus.NY),
                Vedtaksperiode(id = id2, fom = LocalDate.of(2024, 1, 3), tom = LocalDate.of(2024, 1, 3), status = VedtaksperiodeStatus.NY),
            )
        }

        private fun lagBeregningsgrunnlag(
            fom: LocalDate,
            tom: LocalDate,
        ) = BeregningsresultatForMåned(
            10,
            grunnlag =
                Beregningsgrunnlag(
                    fom = fom,
                    tom = tom,
                    utbetalingsdato = fom,
                    studienivå = Studienivå.HØYERE_UTDANNING,
                    studieprosent = 100,
                    sats = 100,
                    satsBekreftet = true,
                    målgruppe = MålgruppeType.AAP,
                ),
        )
    }
}
