package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.tilSisteDagIMåneden
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
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

class OpphørValideringServiceTest {

    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val saksbehandling =
        saksbehandling(
            revurderFra = osloDateNow().plusMonths(1).plusDays(1),
            type = BehandlingType.REVURDERING,
        )
    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)
    val vilkår = vilkår(
        behandlingId = saksbehandling.id,
        type = VilkårType.PASS_BARN,
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.UENDRET,
    )
    val målgruppe = VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET)
    val aktivitet = VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET)

    @BeforeEach
    fun setUp() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår)
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(
            målgrupper = listOf(målgruppe),
            aktiviteter = listOf(aktivitet),
        )
        every { tilsynBarnBeregningService.beregn(any(), any()) } returns vedtakBeregningsresultat
    }

    @Nested
    inner class `Valider ingen utbetaling etter opphør` {

        @Test
        fun `Kaster ikke feil ved korrekt data`() {
            assertThatCode {
                opphørValideringService.validerIngenUtbetalingEtterRevurderFraDato(
                    beregningsresultatTilsynBarn = vedtakBeregningsresultat,
                    revurderFra = saksbehandling.revurderFra,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil ved utbetaling etter opphørdato`() {
            val saksbehandlingRevurdertFraTilbakeITid = saksbehandling.copy(revurderFra = LocalDate.of(2024,1,1))

            assertThatThrownBy {
                opphørValideringService.validerIngenUtbetalingEtterRevurderFraDato(
                    beregningsresultatTilsynBarn = vedtakBeregningsresultat,
                    revurderFra = saksbehandlingRevurdertFraTilbakeITid.revurderFra,
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger på eller etter revurder fra dato")
        }
    }

    @Nested
    inner class `Valider perioder` {

        @Test
        fun `validerPerioder kaster ikke feil ved korrekt data`() {
            assertThatCode { opphørValideringService.validerPerioder(saksbehandling = saksbehandling) }
                .doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil ved nye oppfylte vilkår`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår.copy(status = VilkårStatus.NY))

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er nye utgifter som er oppfylt")
        }

        @Test
        fun `Kaster feil ved nye oppfylte målgrupper`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(
                målgrupper = listOf(målgruppe.copy(status = Vilkårstatus.NY)),
                aktiviteter = listOf(aktivitet),
            )

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er nye målgrupper som er oppfylt")
        }

        @Test
        fun `Kaster feil ved nye oppfylte aktivteter`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(
                målgrupper = listOf(målgruppe),
                aktiviteter = listOf(aktivitet.copy(status = Vilkårstatus.NY)),
            )

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi det er nye aktiviteter som er oppfylt")
        }

        @Test
        fun `Kaster feil ved målgruppe flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(
                målgrupper = listOf(målgruppe.copy(tom = osloDateNow().plusMonths(2), status = Vilkårstatus.ENDRET)),
                aktiviteter = listOf(aktivitet),
            )

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret målgruppe er etter revurder fra dato")
        }

        @Test
        fun `Kaster feil ved aktivitet flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(
                målgrupper = listOf(målgruppe),
                aktiviteter = listOf(aktivitet.copy(tom = osloDateNow().plusMonths(2), status = Vilkårstatus.ENDRET)),
            )

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret aktivitet er etter revurder fra dato")
        }

        @Test
        fun `Kaster feil ved vilkår flyttet til etter opphørt dato`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(
                vilkår.copy(
                    status = VilkårStatus.ENDRET,
                    tom = osloDateNow().plusMonths(1).tilSisteDagIMåneden(),
                ),
            )

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter revurder fra dato")
        }
    }
}
