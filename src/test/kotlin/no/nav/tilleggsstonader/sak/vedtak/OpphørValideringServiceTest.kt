package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.fagsakBoutgifter
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class OpphørValideringServiceTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val måned = YearMonth.of(2025, 1)
    val fom = måned.atDay(1)
    val tom = måned.atEndOfMonth()

    val opphørsdato = LocalDate.of(2025, 2, 1)

    val saksbehandlingBoutgifter =
        saksbehandling(
            type = BehandlingType.REVURDERING,
            fagsak = fagsakBoutgifter(),
        )

    val saksbehandling =
        saksbehandling(
            type = BehandlingType.REVURDERING,
        )
    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)
    val vilkår =
        vilkår(
            behandlingId = saksbehandling.id,
            type = VilkårType.PASS_BARN,
            resultat = Vilkårsresultat.OPPFYLT,
            status = VilkårStatus.UENDRET,
            fom = fom,
            tom = tom,
        )
    val målgruppe = VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET, fom = fom, tom = tom)
    val aktivitet = VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET, fom = fom, tom = tom)

    val beregningsresultat =
        BeregningsresultatTilsynBarn(
            listOf(
                beregningsresultatForMåned(
                    måned = måned,
                    beløpsperioder = listOf(Beløpsperiode(dato = fom, 10, FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)),
                ),
            ),
        )

    val vilkårBoutgifter =
        vilkår(
            behandlingId = saksbehandlingBoutgifter.id,
            type = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
            resultat = Vilkårsresultat.OPPFYLT,
            status = VilkårStatus.ENDRET,
            fom = fom,
            tom = tom,
        )

    @BeforeEach
    fun setUp() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår)
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = listOf(målgruppe),
                aktiviteter = listOf(aktivitet),
            )
        every { tilsynBarnBeregningService.beregn(any(), any(), any(), any()) } returns beregningsresultat
    }

    @Nested
    inner class `Valider ingen utbetaling etter opphør` {
        @Test
        fun `Kaster ikke feil ved korrekt data`() {
            assertThatCode {
                opphørValideringService.validerIngenUtbetalingEtterOpphørsdato(
                    beregningsresultatTilsynBarn = beregningsresultat,
                    opphørsdato = opphørsdato,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil ved utbetaling etter opphørdato`() {
            val opphørsdatoTilbakeITid = måned.atDay(1)

            assertThatThrownBy {
                opphørValideringService.validerIngenUtbetalingEtterOpphørsdato(
                    beregningsresultatTilsynBarn = beregningsresultat,
                    opphørsdato = opphørsdatoTilbakeITid,
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger på eller etter opphørsdato")
        }

        @Test
        fun `Skal ikke kaste feil hvis det finnes en utbetaling med 0 i beløp etter opphørsdato`() {
            val beregningsresultatForMåned =
                beregningsresultatForMåned(
                    måned = måned,
                    beløpsperioder = listOf(Beløpsperiode(dato = fom, 0, FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)),
                )
            val opphørsdatoTilbakeITid = måned.atDay(1)

            assertThatCode {
                opphørValideringService.validerIngenUtbetalingEtterOpphørsdato(
                    beregningsresultatTilsynBarn = BeregningsresultatTilsynBarn(listOf(beregningsresultatForMåned)),
                    opphørsdato = opphørsdatoTilbakeITid,
                )
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class `Valider perioder` {
        @Test
        fun `validerPerioder kaster ikke feil ved korrekt data`() {
            assertThatCode { opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato) }
                .doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil ved nye oppfylte vilkår`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår.copy(status = VilkårStatus.NY))

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye utgifter med oppfylte vilkår")
        }

        @Test
        fun `Kaster feil ved nye oppfylte målgrupper`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns
                Vilkårperioder(
                    målgrupper = listOf(målgruppe.copy(status = Vilkårstatus.NY)),
                    aktiviteter = listOf(aktivitet),
                )

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye målgrupper med oppfylte vilkår")
        }

        @Test
        fun `Kaster feil ved nye oppfylte aktivteter`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns
                Vilkårperioder(
                    målgrupper = listOf(målgruppe),
                    aktiviteter = listOf(aktivitet.copy(status = Vilkårstatus.NY)),
                )

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye aktiviteter med oppfylte vilkår")
        }

        @Test
        fun `Kaster feil ved målgruppe flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns
                Vilkårperioder(
                    målgrupper =
                        listOf(
                            målgruppe.copy(
                                tom = LocalDate.now().plusMonths(2),
                                status = Vilkårstatus.ENDRET,
                            ),
                        ),
                    aktiviteter = listOf(aktivitet),
                )

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret målgruppe er etter opphørsdato")
        }

        @Test
        fun `Kaster feil ved aktivitet flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns
                Vilkårperioder(
                    målgrupper = listOf(målgruppe),
                    aktiviteter =
                        listOf(
                            aktivitet.copy(
                                tom = måned.plusMonths(1).atEndOfMonth(),
                                status = Vilkårstatus.ENDRET,
                            ),
                        ),
                )

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret aktivitet er etter opphørsdato")
        }

        @Test
        fun `Kaster feil ved vilkår flyttet til etter opphørt dato`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns
                listOf(
                    vilkår.copy(
                        status = VilkårStatus.ENDRET,
                        tom = YearMonth.from(opphørsdato).plusMonths(1).atEndOfMonth(),
                    ),
                )

            assertThatThrownBy {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter opphørsdato")
        }

        @Test
        fun `Kaster ikke feil ved vilkår flyttet til etter opphørt dato men er i samme måned`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns
                listOf(
                    vilkår.copy(
                        status = VilkårStatus.ENDRET,
                        tom = YearMonth.from(opphørsdato).atEndOfMonth(),
                    ),
                )

            assertThatCode {
                opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class `Valider at vedtaksperioden er avkortet ved opphør` {
        val vedtaksperiodeJanuar = vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)
        val vedtaksperiodeFebruar = vedtaksperiode(fom = 1 februar 2025, tom = 28 februar 2025)

        @Test
        fun `Kaster ikke feil når vedtaksperioden er avkortet`() {
            assertThatCode {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    opphørsdato = vedtaksperiodeFebruar.tom,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når vedtaksperioden ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    opphørsdato = 1 mars 2025,
                )
            }.hasMessage(
                "Opphør er et ugyldig valg fordi ønsket opphørsdato ikke korter ned vedtaket.",
            )
        }
    }

    @Test
    fun `Kaster feil ved vilkår flyttet til etter opphørt dato for boutgifter`() {
        every { vilkårService.hentVilkår(saksbehandlingBoutgifter.id) } returns
            listOf(
                vilkårBoutgifter.copy(
                    status = VilkårStatus.ENDRET,
                    tom = YearMonth.from(opphørsdato).atEndOfMonth(),
                ),
            )

        assertThatThrownBy {
            opphørValideringService.validerVilkårperioder(saksbehandlingBoutgifter, opphørsdato)
        }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter opphørsdato")
    }
}
