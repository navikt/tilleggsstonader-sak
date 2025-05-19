package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VedtaksperioderOversiktServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var vedtaksperioderOversiktService: VedtaksperioderOversiktService

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal returnere vedtaksperiodeoversikt for alle stønadstyper`() {
        val fagsakPerson = testoppsettService.opprettPerson("123")

        opprettBehandlingOgVedtakTilsynBarn(fagsakPerson)
        opprettBehandlingOgVedtakLæremidler(fagsakPerson)
        opprettBehandlingOgVedtakBoutgifter(fagsakPerson)

        val res = vedtaksperioderOversiktService.hentVedtaksperioderOversikt(fagsakPersonId = fagsakPerson.id)

        assertThat(res.tilsynBarn).isNotEmpty()
        assertThat(res.læremidler).isNotEmpty()
        assertThat(res.boutgifter).isNotEmpty()
    }

    private fun opprettBehandlingOgVedtakTilsynBarn(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.BARNETILSYN,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(TilsynBarnTestUtil.innvilgetVedtak(behandlingId = behandling.id))
    }

    private fun opprettBehandlingOgVedtakLæremidler(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.LÆREMIDLER,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(LæremidlerTestUtil.innvilgelse(behandlingId = behandling.id))
    }

    private fun opprettBehandlingOgVedtakBoutgifter(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.BOUTGIFTER,
                identer = fagsakPerson.identer,
            )

        val beregningsresultat =
            BoutgifterTestUtil.lagBeregningsresultatMåned(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                utgifter =
                    mapOf(
                        TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                            listOf(
                                UtgiftBeregningBoutgifter(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                    utgift = 1000,
                                ),
                            ),
                    ),
            )

        val vedtak =
            BoutgifterTestUtil.innvilgelseBoutgifter(
                behandlingId = behandling.id,
                vedtaksperioder = emptyList(),
                beregningsresultat = BeregningsresultatBoutgifter(listOf(beregningsresultat)),
            )

        vedtakRepository.insert(vedtak)
    }
}
