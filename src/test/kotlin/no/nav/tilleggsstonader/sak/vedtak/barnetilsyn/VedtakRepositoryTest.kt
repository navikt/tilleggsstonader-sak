package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VedtakRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val beregningsresultat = BeregningsresultatTilsynBarn(emptyList())
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                data =
                    InnvilgelseTilsynBarn(
                        beregningsresultat = beregningsresultat,
                    ),
            ),
        )

        val lagretVedtak = vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseTilsynBarn>()
        assertThat(lagretVedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.data.beregningsresultat).isEqualTo(beregningsresultat)
    }
}
