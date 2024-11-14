package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.interntVedtak.Vilkårperiode
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpphørValidatorServiceTest {

    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val saksbehandling = saksbehandling(revurderFra = osloDateNow(), type = BehandlingType.REVURDERING)

    val opphørValidatorService = OpphørValidatorService(vilkårperiodeService, vilkårService, tilsynBarnBeregningService)

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }


    @Test
    fun validerOpphørKasterIkkeValideringsfeil() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns emptyList()
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        opphørValidatorService.validerOpphør(saksbehandling)
    }

    @Test
    fun validerValiderIngenOppfylteVilkårEllerVilkårperioderMedStatusNy() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns emptyList()
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        opphørValidatorService.validerOpphør(saksbehandling)
    }
}