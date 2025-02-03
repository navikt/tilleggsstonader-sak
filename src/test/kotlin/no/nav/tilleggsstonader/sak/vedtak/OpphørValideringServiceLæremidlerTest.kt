package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class OpphørValideringServiceLæremidlerTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val læremidlerBeregningService = mockk<LæremidlerBeregningService>()

    val måned = YearMonth.of(2025, 1)
    val fom = måned.atDay(1)
    val tom = måned.atEndOfMonth()

    val saksbehandling =
        saksbehandling(
            revurderFra = LocalDate.of(2025, 2, 1),
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
        BeregningsresultatLæremidler(
            listOf(
                BeregningsresultatForMåned(
                    beløp = 100,
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
                ),
            ),
        )

    val forrigeBeregningsresultatFremITid =
        BeregningsresultatLæremidler(
            listOf(
                BeregningsresultatForMåned(
                    beløp = 100,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = fom.plusMonths(1),
                            tom = tom.plusMonths(1),
                            utbetalingsdato = fom,
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 100,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
            ),
        )

    @BeforeEach
    fun setUp() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår)
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = listOf(målgruppe),
                aktiviteter = listOf(aktivitet),
            )
        every { læremidlerBeregningService.beregn(any(), any()) } returns beregningsresultat
    }

    @Nested
    inner class `Valider ingen utbetaling etter opphør - Læremidler` {
        @Test
        fun `Kaster ikke feil ved korrekt data`() {
            assertThatCode {
                opphørValideringService.validerIngenUtbetalingEtterRevurderFraDatoLæremidler(
                    beregningsresultatForMånedListe = beregningsresultat.perioder,
                    revurderFra = saksbehandling.revurderFra,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil ved utbetaling etter opphørdato`() {
            val saksbehandlingRevurdertFraTilbakeITid = saksbehandling.copy(revurderFra = måned.atDay(1))

            assertThatThrownBy {
                opphørValideringService.validerIngenUtbetalingEtterRevurderFraDatoLæremidler(
                    beregningsresultatForMånedListe = beregningsresultat.perioder,
                    revurderFra = saksbehandlingRevurdertFraTilbakeITid.revurderFra,
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger på eller etter revurder fra dato")
        }

        @Nested
        inner class `Valider beregningsresultat er avkortet ved opphør` {
            @Test
            fun `Kaster ikke feil når forrige behandling sin tom er frem i tid`() {
                assertThatCode {
                    opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                        beregningsresultat.perioder,
                        forrigeBeregningsresultatForMåned = forrigeBeregningsresultatFremITid.perioder,
                    )
                }.doesNotThrowAnyException()
            }

            @Test
            fun `Kaster feil når nytt beregeningsresultat ikke er avkortet`() {
                assertThatThrownBy {
                    opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                        beregningsresultat.perioder,
                        beregningsresultat.perioder,
                    )
                }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi ingen beregningsresultat eller utbetalingsperioder blir avkortet")
            }
        }
    }
}
