package no.nav.tilleggsstonader.sak.vedtak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningMåned
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class VedtaksperiodeValideringServiceTest {
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val vedtaksperiodeValidingerService =
        VedtaksperiodeValideringService(
            vilkårperiodeService = vilkårperiodeService,
            vedtakRepository = vedtakRepository,
        )

    val behandling = saksbehandling()

    val vedtaksperiodeJanuar =
        vedtaksperiode(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
        )
    val vedtaksperiodeFebruar =
        vedtaksperiode(
            fom = LocalDate.of(2025, 2, 1),
            tom = LocalDate.of(2025, 2, 28),
        )

    val målgrupper =
        listOf(
            målgruppe(
                faktaOgVurdering = VilkårperiodeTestUtil.faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )
    val aktiviteter =
        listOf(
            aktivitet(
                faktaOgVurdering = VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )

    val utgifter: Map<BarnId, List<UtgiftBeregningMåned>> =
        mapOf(
            BarnId.random() to
                listOf(
                    UtgiftBeregningMåned(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 2),
                        utgift = 1000,
                    ),
                ),
        )

    @BeforeEach
    fun setup() {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(målgrupper = målgrupper, aktiviteter = aktiviteter)
    }

    @Test
    fun `skal ikke kaste feil for gyldig vedtaksperiode`() {
        val vedtaksperiode = lagVedtaksperiode()

        assertDoesNotThrow {
            validerInnvilgelse(listOf(vedtaksperiode))
        }
    }

    @Nested
    inner class ValiderFinnesVedaksperioder {
        @Test
        fun `skal kaste feil hvis innvilgelse ikke inneholder noen vedtaksperioder`() {
            assertThatThrownBy {
                validerInnvilgelse(listOf())
            }.hasMessageContaining("Kan ikke innvilge når det ikke finnes noen vedtaksperioder")
        }

        @Test
        fun `skal ikke validere at det finnes vedtaksperioder for innvilgelse`() {
            assertDoesNotThrow {
                validerOpphør(listOf())
            }
        }
    }

    @Nested
    inner class HappyCase {
        @Test
        fun `Kaster ikke feil ved gyldig data`() {
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)

            assertDoesNotThrow {
                validerInnvilgelse(vedtaksperioder)
            }
        }
    }

    @Nested
    inner class ValiderIngenOverlappendeVedtaksperioder {
        @Test
        fun `Overlappende vedtaksperioder kaster feil`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeJanuar,
                    vedtaksperiodeFebruar.copy(fom = LocalDate.of(2024, 1, 31)),
                )

            assertThatThrownBy {
                validerInnvilgelse(vedtaksperioder)
            }.hasMessageContaining("Vedtaksperioder kan ikke overlappe")
        }

        @Test
        fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeJanuar.copy(tom = LocalDate.of(2025, 1, 14)),
                    vedtaksperiodeJanuar.copy(fom = LocalDate.of(2025, 1, 16)),
                )

            assertDoesNotThrow {
                validerInnvilgelse(vedtaksperioder)
            }
        }
    }

    /**
     * Fler tester finnes i [no.nav.tilleggsstonader.sak.vedtak.ValiderValiderIngenEndringerFørRevurderFraTest]
     */
    @Nested
    inner class ValiderIngenEndringerFørRevurderFra {
        val behandling = behandling()
        val revurdering =
            saksbehandling(
                type = BehandlingType.REVURDERING,
                revurderFra = LocalDate.of(2025, 2, 1),
                forrigeIverksatteBehandlingId = behandling.id,
            )

        val vedtaksperiode = vedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 31))

        @BeforeEach
        fun setUp() {
            every { vedtakRepository.findByIdOrThrow(behandling.id) } returns
                innvilgetVedtak(vedtaksperioder = listOf(vedtaksperiode()))
        }

        @Test
        fun `skal kaste feil hvis det finnes endring før revurder fra`() {
            assertThatThrownBy {
                validerInnvilgelse(listOf(vedtaksperiode.copy(tom = LocalDate.of(2025, 1, 15))), revurdering)
            }.hasMessageContaining("Det er ikke tillat å legge til, endre eller slette vedtaksperioder fra før revurder fra dato")
        }
    }

    @Nested
    inner class ValiderIkkeOverlappMedMålgruppeSomIkkeGirRettPåStønad {
        val målgruppeUtenRett =
            målgruppe(
                faktaOgVurdering = VilkårperiodeTestUtil.faktaOgVurderingMålgruppe(type = MålgruppeType.INGEN_MÅLGRUPPE),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
                begrunnelse = "begrunnelse",
            )

        @Test
        fun `skal validere at man ikke innvilger for en periode med overlapp med målgruppe som ikke gir rett på stønad`() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(målgrupper = målgrupper + målgruppeUtenRett, aktiviteter = aktiviteter)

            assertThatThrownBy {
                validerInnvilgelse(listOf(vedtaksperiodeJanuar))
            }.hasMessageContaining(
                "Vedtaksperiode 01.01.2025–31.01.2025 overlapper med INGEN_MÅLGRUPPE(01.01.2025–28.02.2025) " +
                    "som ikke gir rett på stønad",
            )
        }
    }

    @Nested
    inner class ValideringMedMålgruppeOgAktivitet {
        @Test
        fun `skal validere at det finnes målgruppe og aktivitet for vedtaksperiode`() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(målgrupper = emptyList(), aktiviteter = emptyList())

            assertThatThrownBy {
                validerInnvilgelse(listOf(vedtaksperiodeJanuar))
            }.hasMessageContaining("Finner ingen perioder hvor vilkår for NEDSATT_ARBEIDSEVNE er oppfylt")
        }
    }

    private fun validerInnvilgelse(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling = saksbehandling(),
    ) {
        vedtaksperiodeValidingerService.validerVedtaksperioder(
            vedtaksperioder,
            behandling,
            TypeVedtak.INNVILGELSE,
        )
    }

    private fun validerOpphør(vedtaksperioder: List<Vedtaksperiode>) {
        vedtaksperiodeValidingerService.validerVedtaksperioder(
            vedtaksperioder,
            saksbehandling(),
            TypeVedtak.OPPHØR,
        )
    }

    private fun lagVedtaksperiode(
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = UUID.randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
