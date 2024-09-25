package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TilsynBarnVedtakRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var tilsynBarnVedtakRepository: TilsynBarnVedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val vedtak = VedtaksdataTilsynBarn(emptyMap())
        val beregningsresultat = BeregningsresultatTilsynBarn(emptyList())
        tilsynBarnVedtakRepository.insert(
            VedtakTilsynBarn(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                vedtak = vedtak,
                beregningsresultat = beregningsresultat,
            ),
        )

        val lagretVedtak = tilsynBarnVedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(lagretVedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.vedtak).isEqualTo(vedtak)
        assertThat(lagretVedtak.beregningsresultat).isEqualTo(beregningsresultat)
    }
}
