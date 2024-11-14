package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpphørValidatorServiceTest {

    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val saksbehandling = saksbehandling()

    val opphørValidatorService = OpphørValidatorService(vilkårperiodeService, vilkårService, tilsynBarnBeregningService)

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun validerValiderIngenOppfylteVilkårEllerVilkårperioderMedStatusNy() {

        opphørValidatorService.validerOpphør(saksbehandling)
    }
}