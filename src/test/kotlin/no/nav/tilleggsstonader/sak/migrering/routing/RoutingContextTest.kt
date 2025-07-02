package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.vedtakStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.reflect.KClass

class RoutingContextTest {
    val ident = "testIdent"

    @ParameterizedTest
    @EnumSource(
        value = Stønadstype::class,
        names = ["DAGLIG_REISE_TSO", "DAGLIG_REISE_TSR"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal mappe stønadstype til riktig context`(stønadstype: Stønadstype) {
        val routingContext = stønadstype.tilRoutingContext()

        assertThat(routingContext.stønadstype).isEqualTo(stønadstype)
        assertThat(routingContext.ident).isEqualTo(ident)

        assertThat(routingContext).isInstanceOf(expected[stønadstype]!!.java)
    }

    /**
     * Kan erstattes med riktig kode når daglig reise er implementert.
     */
    @Disabled
    @Test
    fun `skal route daglig reise til arena hvis man ikke har aktivt vedtak`() {
        val routingContext = Stønadstype.BARNETILSYN.tilRoutingContext()

        assertThat(routingContext).isInstanceOf(FeatureTogglet::class.java)

        with(routingContext as FeatureTogglet) {
            assertThat(this.toggleId).isEqualTo(MockToggle.MOCK_TOGGLE)
            assertThat(this.harGyldigStateIArena(arenaStatus(harAktivtVedtak = false))).isTrue
            assertThat(this.harGyldigStateIArena(arenaStatus(harAktivtVedtak = true))).isFalse
        }
    }

    private val expected =
        mapOf<Stønadstype, KClass<out RoutingContext>>(
            Stønadstype.BARNETILSYN to SkalRouteAlleSøkereTilNyLøsning::class,
            Stønadstype.LÆREMIDLER to SkalRouteAlleSøkereTilNyLøsning::class,
            Stønadstype.BOUTGIFTER to SkalRouteAlleSøkereTilNyLøsning::class,
        )

    private fun Stønadstype.tilRoutingContext() = IdentStønadstype(ident = ident, stønadstype = this).tilRoutingContext()

    private fun arenaStatus(harAktivtVedtak: Boolean = false) =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = false),
            vedtakStatus(harVedtak = false, harAktivtVedtak = harAktivtVedtak, harVedtakUtenUtfall = false),
        )

    private enum class MockToggle(
        override val toggleId: String,
    ) : ToggleId {
        MOCK_TOGGLE("toggleId"),
    }
}
