package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {
    val behandlingId = BehandlingId(UUID.randomUUID())
    val behandlingService = mockk<BehandlingService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val læremidlerVedtaksperiodeValideringService =
        LæremidlerVedtaksperiodeValideringService(
            behandlingService = behandlingService,
            vedtakRepository = vedtakRepository,
            vilkårperiodeService = vilkårperiodeService,
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

    val målgrupper =
        listOf(
            målgruppe(
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                fom = vedtaksperiodeJanuar.fom,
                tom = vedtaksperiodeFebruar.tom,
            ),
        )
    val aktiviteter =
        listOf(
            aktivitet(
                faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                fom = vedtaksperiodeJanuar.fom,
                tom = vedtaksperiodeFebruar.tom,
            ),
        )

    @BeforeEach
    fun setUp() {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteter,
            )
    }

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
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 5),
                            tom = LocalDate.of(2024, 1, 10),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregning(
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
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 2, 29),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 15), tom = LocalDate.of(2024, 2, 14)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregning(
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
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 1),
                        ),
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 2),
                            tom = LocalDate.of(2024, 1, 2),
                        ),
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 3),
                            tom = LocalDate.of(2024, 1, 3),
                        ),
                        vedtaksperiodeBeregning(
                            fom = LocalDate.of(2024, 1, 4),
                            tom = LocalDate.of(2024, 1, 4),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 3)),
                )

            assertThat(vedtaksperioder).containsExactly(
                vedtaksperiodeBeregning(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 2)),
                vedtaksperiodeBeregning(fom = LocalDate.of(2024, 1, 3), tom = LocalDate.of(2024, 1, 3)),
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
