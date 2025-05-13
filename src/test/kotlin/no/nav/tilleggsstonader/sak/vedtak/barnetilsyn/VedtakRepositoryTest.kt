package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class VedtakRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val beregningsresultat = BeregningsresultatTilsynBarn(emptyList())
        vedtakRepository.insert(
            innvilgetVedtak(
                behandlingId = behandling.id,
                vedtak = InnvilgelseTilsynBarn(beregningsresultat = beregningsresultat, vedtaksperioder = emptyList()),
            ),
        )

        val lagretVedtak = vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseTilsynBarn>()
        assertThat(lagretVedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.data.beregningsresultat).isEqualTo(beregningsresultat)
    }

    @Nested
    inner class GitVersjon {
        @Test
        fun `skal returnere null hvis det ikke finnes en versjon`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val vedtak = vedtakRepository.insert(innvilgetVedtak(behandling.id).copy(gitVersjon = null))
            assertThat(vedtakRepository.findByIdOrThrow(vedtak.behandlingId).gitVersjon).isNull()
        }

        @Test
        fun `skal returnere versjon hvis det finnes en versjon`() {
            val gitVersjon = UUID.randomUUID().toString()
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val vedtak = vedtakRepository.insert(innvilgetVedtak(behandling.id).copy(gitVersjon = gitVersjon))
            assertThat(vedtakRepository.findByIdOrThrow(vedtak.behandlingId).gitVersjon).isEqualTo(gitVersjon)
        }
    }
}
