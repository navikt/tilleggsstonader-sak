package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TilsynBarnVedtakRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var tilsynBarnVedtakRepository: TilsynBarnVedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        tilsynBarnVedtakRepository.insert(
            VedtakTilsynBarn(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGET,
                vedtak = InnvilgelseTilsynBarnDto(behandling.id, emptyList(), emptyMap()),
                beregningsresultat = null,
            ),
        )
    }
}
