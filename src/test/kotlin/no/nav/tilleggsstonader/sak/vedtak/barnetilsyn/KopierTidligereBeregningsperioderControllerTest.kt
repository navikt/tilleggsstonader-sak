package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class KopierTidligereBeregningsperioderControllerTest : IntegrationTest() {

    @Autowired
    lateinit var controller: KopierTidligereBeregningsperioderController

    @Autowired
    lateinit var vedtakRepository: TilsynBarnVedtakRepository

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg

    val fom = LocalDate.of(2024, 10, 1)
    val tom = LocalDate.of(2024, 10, 31)

    val fagsak = fagsak()
    val behandling = behandling(fagsak, type = BehandlingType.REVURDERING, revurderFra = fom)
    val barn = behandlingBarn(behandlingId = behandling.id)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak = fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
        barnRepository.insert(barn)
    }

    @Test
    fun `skal oppdatere behandling som trenger oppdatering`() {
        opprettPerioder()

        tilsynBarnBeregnYtelseSteg.utførSteg(
            testoppsettService.hentSaksbehandling(behandling.id),
            InnvilgelseTilsynBarnDto(null),
        )

        // overskrever vedtak sån at det har endringer
        val opprinneligVedtak = vedtakRepository.update(
            innvilgetVedtak(
                behandlingId = behandling.id,
                beregningsresultat = BeregningsresultatTilsynBarn(
                    listOf(beregningsresultatForMåned(YearMonth.of(2023, 10))),
                ),
            ),
        )

        testWithBrukerContext {
            controller.oppdaterBeregningsresultat()
        }

        val oppdatertVedtak = vedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertVedtak.beregningsresultat!!).isNotEqualTo(opprinneligVedtak.beregningsresultat)
        assertThat(oppdatertVedtak.beregningsresultat!!.perioder.map { it.grunnlag.måned to it.månedsbeløp })
            .containsExactly(Pair(YearMonth.of(2024, 10), 679))
    }

    @Test
    fun `skal ikke oppdatere hvis andelene er annerledes`() {
        opprettPerioder()
        tilsynBarnBeregnYtelseSteg.utførSteg(
            testoppsettService.hentSaksbehandling(behandling.id),
            InnvilgelseTilsynBarnDto(null),
        )
        // overskrever tilkjent ytelse sån at det har endringer
        tilkjentYtelseRepository.deleteAll()
        tilkjentYtelseRepository.insert(
            tilkjentYtelse(
                behandlingId = behandling.id,
                andelTilkjentYtelse(kildeBehandlingId = behandling.id),
            ),
        )

        val opprinneligVedtak = vedtakRepository.update(
            innvilgetVedtak(
                behandlingId = behandling.id,
                beregningsresultat = BeregningsresultatTilsynBarn(
                    listOf(beregningsresultatForMåned(YearMonth.of(2023, 10))),
                ),
            ),
        )

        testWithBrukerContext {
            controller.oppdaterBeregningsresultat()
        }

        val oppdatertVedtak = vedtakRepository.findByIdOrThrow(behandling.id)

        assertThat(oppdatertVedtak.beregningsresultat).isEqualTo(opprinneligVedtak.beregningsresultat)
    }

    @Test
    fun `skal ikke oppdatere behandling hvis beregningsgrunnlaget er riktig fra før`() {
        opprettPerioder()

        tilsynBarnBeregnYtelseSteg.utførSteg(
            testoppsettService.hentSaksbehandling(behandling.id),
            InnvilgelseTilsynBarnDto(null),
        )

        val opprinneligVedtak = vedtakRepository.findByIdOrThrow(behandling.id)

        testWithBrukerContext {
            controller.oppdaterBeregningsresultat()
        }

        val oppdatertVedtak = vedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertVedtak.beregningsresultat).isEqualTo(opprinneligVedtak.beregningsresultat)
    }

    private fun opprettPerioder() {
        vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id, fom = fom, tom = tom))
        vilkårperiodeRepository.insert(aktivitet(behandlingId = behandling.id, fom = fom, tom = tom))
        stønadsperiodeRepository.insert(stønadsperiode(behandlingId = behandling.id, fom = fom, tom = tom))
        vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                fom = fom,
                tom = tom,
                utgift = 1000,
                type = VilkårType.PASS_BARN,
                barnId = barn.id,
            ),
        )
    }
}
