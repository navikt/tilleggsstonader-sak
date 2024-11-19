package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksdataTilsynBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VedtakRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val vedtak = VedtaksdataTilsynBarn(emptyMap())
        val beregningsresultat = BeregningsresultatTilsynBarn(emptyList())
        vedtakRepository.insert(
            VedtakTilsynBarn(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                vedtak = vedtak,
                beregningsresultat = beregningsresultat,
            ),
        )

        val lagretVedtak = vedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(lagretVedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.vedtak).isEqualTo(vedtak)
        assertThat(lagretVedtak.beregningsresultat).isEqualTo(beregningsresultat)
    }
}
