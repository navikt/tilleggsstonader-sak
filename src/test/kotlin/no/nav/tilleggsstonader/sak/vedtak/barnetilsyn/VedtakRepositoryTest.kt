package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
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

class VedtakRepositoryTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal kunne lagre og hente vedtak`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val beregningsresultat = BeregningsresultatTilsynBarn(emptyList())
        vedtakRepository.insert(
            innvilgetVedtak(
                behandlingId = behandling.id,
                vedtak =
                    InnvilgelseTilsynBarn(
                        beregningsresultat = beregningsresultat,
                        vedtaksperioder = emptyList(),
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    ),
            ),
        )

        val lagretVedtak = vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseTilsynBarn>()
        assertThat(lagretVedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.data.beregningsresultat).isEqualTo(beregningsresultat)
    }

    @Test
    fun `skal bevare beregningsplan med fraDato`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val fraDato = 10 januar 2025

        vedtakRepository.insert(
            innvilgetVedtak(
                behandlingId = behandling.id,
                vedtak =
                    InnvilgelseTilsynBarn(
                        beregningsresultat = BeregningsresultatTilsynBarn(emptyList()),
                        vedtaksperioder = emptyList(),
                        beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato),
                    ),
            ),
        )

        val lagretVedtak = vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseTilsynBarn>()
        assertThat(lagretVedtak.data.beregningsplan.beregnFra()).isEqualTo(fraDato)
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
