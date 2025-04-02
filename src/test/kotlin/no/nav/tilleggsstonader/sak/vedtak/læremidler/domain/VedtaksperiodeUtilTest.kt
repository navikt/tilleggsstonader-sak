package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.BrukVedtaksperioderForBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {
    val behandlingId = BehandlingId(UUID.randomUUID())
    val behandlingService = mockk<BehandlingService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val læremidlerVedtaksperiodeValideringService =
        LæremidlerVedtaksperiodeValideringService(
            behandlingService = behandlingService,
            vedtakRepository = vedtakRepository,
        )
    val vedtaksperiodeJanuar =
        vedtaksperiode(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
        )
    val vedtaksperiodeFebruar =
        vedtaksperiode(
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 28),
        )

    @Nested
    inner class ValiderVedtaksperioder {
        @Test
        fun `Kaster ikke feil ved gyldig data`() {
            every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)

            assertDoesNotThrow {
                læremidlerVedtaksperiodeValideringService.validerVedtaksperioder(
                    vedtaksperioder = vedtaksperioder,
                    behandlingId = behandlingId,
                    brukVedtaksperioderForBeregning = BrukVedtaksperioderForBeregning(false),
                )
            }
        }

        @Test
        fun `Manglende vedtaksperioder kaster feil`() {
            every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()

            val vedtaksperioder = emptyList<Vedtaksperiode>()

            assertThatThrownBy {
                læremidlerVedtaksperiodeValideringService.validerVedtaksperioder(
                    vedtaksperioder = vedtaksperioder,
                    behandlingId = behandlingId,
                    brukVedtaksperioderForBeregning = BrukVedtaksperioderForBeregning(false),
                )
            }.hasMessageContaining("Kan ikke innvilge når det ikke finnes noen vedtaksperioder.")
        }

        @Test
        fun `Overlappende vedtaksperioder kaster feil`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeJanuar,
                    vedtaksperiodeFebruar.copy(fom = LocalDate.of(2024, 1, 31)),
                )

            assertThatThrownBy {
                validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiode(
                        fom = LocalDate.of(2024, 1, 15),
                        tom = LocalDate.of(2024, 2, 14),
                    ),
                    vedtaksperiode(
                        fom = LocalDate.of(2024, 2, 15),
                        tom = LocalDate.of(2024, 2, 28),
                    ),
                )

            assertDoesNotThrow {
                validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
            }
        }
    }

    @Nested
    inner class VedtaksperioderInnenforLøpendeMåned {
        @Test
        fun `skal ikke avkorte vedtaksperiode hvis den omslutes av beregningsgrunnlag`() {
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 5),
                            tom = LocalDate.of(2024, 1, 10),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2024, 1, 5),
                    tom = LocalDate.of(2024, 1, 10),
                ),
            )
        }

        @Test
        fun `skal avkorte vedtaksperiode hvis den er lengre enn beregningsgrunnlag`() {
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 2, 29),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 15), tom = LocalDate.of(2024, 2, 14)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2024, 1, 15),
                    tom = LocalDate.of(2024, 2, 14),
                ),
            )
        }

        @Test
        fun `skal returnere alle perioder innenfor et beregningsgrunnlag`() {
            val vedtaksperioder =
                vedtaksperioderInnenforLøpendeMåned(
                    listOf(
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 1),
                        ),
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 2),
                            tom = LocalDate.of(2024, 1, 2),
                        ),
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 3),
                            tom = LocalDate.of(2024, 1, 3),
                        ),
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 4),
                            tom = LocalDate.of(2024, 1, 4),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 3)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 2)),
                vedtaksperiodeBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 3), tom = LocalDate.of(2024, 1, 3)),
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
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
        )
    }
}
