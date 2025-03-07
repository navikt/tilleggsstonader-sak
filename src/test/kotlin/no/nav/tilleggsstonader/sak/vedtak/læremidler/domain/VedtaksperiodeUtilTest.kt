package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperiodeOmfattesAvStønadsperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
    val behandlingService = mockk<BehandlingService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val læremidlerVedtaksperiodeValideringService =
        LæremidlerVedtaksperiodeValideringService(
            behandlingService = behandlingService,
            vedtakRepository = vedtakRepository,
        )
    val vedtaksperiodeJanuar =
        Vedtaksperiode(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
        )
    val vedtaksperiodeFebruar =
        Vedtaksperiode(
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 28),
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
            every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                læremidlerVedtaksperiodeValideringService.validerVedtaksperioder(
                    vedtaksperioder = vedtaksperioder,
                    stønadsperioder = stønadsperioder,
                    behandlingId = behandlingId,
                )
            }
        }

        @Test
        fun `Manglende vedtaksperioder kaster feil`() {
            every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()

            val vedtaksperioder = emptyList<Vedtaksperiode>()
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertThatThrownBy {
                læremidlerVedtaksperiodeValideringService.validerVedtaksperioder(
                    vedtaksperioder = vedtaksperioder,
                    stønadsperioder = stønadsperioder,
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
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertThatThrownBy {
                validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
            val vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        fom = LocalDate.of(2024, 1, 15),
                        tom = LocalDate.of(2024, 2, 14),
                    ),
                    Vedtaksperiode(
                        fom = LocalDate.of(2024, 2, 15),
                        tom = LocalDate.of(2024, 2, 28),
                    ),
                )
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
                validerVedtaksperiodeOmfattesAvStønadsperioder(vedtaksperioder, stønadsperioder)
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
                validerVedtaksperiodeOmfattesAvStønadsperioder(vedtaksperioder, stønadsperioder)
            }.hasMessageContaining("Vedtaksperiode er ikke innenfor en periode med overlapp")
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
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = LocalDate.of(2024, 1, 5),
                    tom = LocalDate.of(2024, 1, 10),
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
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 15), tom = LocalDate.of(2024, 2, 14)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = LocalDate.of(2024, 1, 15),
                    tom = LocalDate.of(2024, 2, 14),
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
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 1),
                        ),
                        Vedtaksperiode(
                            id = id1,
                            fom = LocalDate.of(2024, 1, 2),
                            tom = LocalDate.of(2024, 1, 2),
                        ),
                        Vedtaksperiode(
                            id = id2,
                            fom = LocalDate.of(2024, 1, 3),
                            tom = LocalDate.of(2024, 1, 3),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 1, 4),
                            tom = LocalDate.of(2024, 1, 4),
                        ),
                    ),
                    lagBeregningsgrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 3)),
                )

            assertThat(vedtaksperioder).containsExactly(
                Vedtaksperiode(id = id1, fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 2)),
                Vedtaksperiode(id = id2, fom = LocalDate.of(2024, 1, 3), tom = LocalDate.of(2024, 1, 3)),
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
                    aktivitet = AktivitetType.TILTAK,
                ),
        )
    }
}
