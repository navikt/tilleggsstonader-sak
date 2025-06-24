package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.brev.vedtaksbrev.VedtaksbrevRepository
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringTestUtil.simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksbrev
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeGrunnlagService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class NullstillBehandlingServiceTest : IntegrationTest() {
    @Autowired
    lateinit var nullstillBehandlingService: NullstillBehandlingService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository

    @Autowired
    lateinit var vilkårperiodeGrunnlagService: VilkårperiodeGrunnlagService

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var vedtaksbrevRepository: VedtaksbrevRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var simuleringsresultatRepository: SimuleringsresultatRepository

    val fagsak = fagsak()
    val behandling = behandling(fagsak, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
    val revurdering = behandling(fagsak, forrigeIverksatteBehandlingId = behandling.id)

    val behandlingBarn1 = behandlingBarn(behandlingId = behandling.id, personIdent = "1")
    val revurderingBarn1 = behandlingBarn(behandlingId = revurdering.id, personIdent = "1")
    val revurderingBarn2 = behandlingBarn(behandlingId = revurdering.id, personIdent = "2")

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
        testoppsettService.lagre(revurdering, opprettGrunnlagsdata = false)
    }

    @Test
    fun `skal fjerne alt dersom forrigeBehandling ikke har noe`() {
        opprettVilkårperiode(revurdering.id)

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(vilkårperiodeRepository.findByBehandlingId(revurdering.id)).isEmpty()
        assertThat(vilkårRepository.findByBehandlingId(revurdering.id)).isEmpty()
    }

    @Test
    fun `skal legge inn data fra forrige behandling på nytt`() {
        opprettBarn()
        opprettVilkårperiode(behandling.id)
        opprettVilkår(behandling.id, barnId = behandlingBarn1.id)

        val vilkårperiode = vilkårperiodeRepository.findByBehandlingId(behandling.id).single()
        val vilkår = vilkårRepository.findByBehandlingId(behandling.id).single()

        assertIngenDataPåRevurdering()

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertVilkårPeriodeErGjenbrukt(vilkårperiode)
        assertVilkårErGjenbrukt(vilkår)
    }

    @Test
    fun `barnen skal beholdes som tidligere`() {
        opprettBarn()

        val barnBehandling = barnService.finnBarnPåBehandling(behandling.id)
        val barnRevurdering = barnService.finnBarnPåBehandling(revurdering.id)

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(barnService.finnBarnPåBehandling(behandling.id))
            .hasSize(1)
            .containsExactlyInAnyOrderElementsOf(barnBehandling)

        assertThat(barnService.finnBarnPåBehandling(revurdering.id))
            .hasSize(2)
            .containsExactlyInAnyOrderElementsOf(barnRevurdering)
    }

    @Test
    fun `skal slette tilkjent ytelse`() {
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, andelTilkjentYtelse(behandling.id)))
        tilkjentYtelseRepository.insert(tilkjentYtelse(revurdering.id, andelTilkjentYtelse(revurdering.id)))

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(tilkjentYtelseRepository.findByBehandlingId(behandling.id)).isNotNull
        assertThat(tilkjentYtelseRepository.findByBehandlingId(revurdering.id)).isNull()
    }

    @Test
    fun `skal slette vedtak`() {
        vedtakRepository.insert(innvilgetVedtak(behandlingId = behandling.id))
        vedtakRepository.insert(innvilgetVedtak(behandlingId = revurdering.id))

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(vedtakRepository.findByIdOrNull(behandling.id)).isNotNull
        assertThat(vedtakRepository.findByIdOrNull(revurdering.id)).isNull()
    }

    @Test
    fun `skal slette brev`() {
        vedtaksbrevRepository.insert(vedtaksbrev(behandling.id))
        vedtaksbrevRepository.insert(vedtaksbrev(revurdering.id))

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(vedtaksbrevRepository.findByIdOrNull(behandling.id)).isNotNull
        assertThat(vedtaksbrevRepository.findByIdOrNull(revurdering.id)).isNull()
    }

    @Test
    fun `skal slette simuleringsresultat`() {
        simuleringsresultatRepository.insert(simuleringsresultat(behandling.id))
        simuleringsresultatRepository.insert(simuleringsresultat(revurdering.id))

        nullstillBehandlingService.nullstillBehandling(revurdering)

        assertThat(simuleringsresultatRepository.findByIdOrNull(behandling.id)).isNotNull
        assertThat(simuleringsresultatRepository.findByIdOrNull(revurdering.id)).isNull()
    }

    @Test
    fun `kan ikke nullstille er behandling som er ferdigstilt`() {
        assertThatThrownBy {
            nullstillBehandlingService.nullstillBehandling(behandling)
        }.hasMessageContaining("Behandlingen kan ikke nullstilles fordi den har status Ferdigstilt.")
    }

    @Nested
    inner class SlettVilkårperiodegrunnlag {
        @Test
        fun `skal ikke kunne slette for ferdigstilt behandling`() {
            assertThatThrownBy {
                nullstillBehandlingService.slettVilkårperiodegrunnlag(behandling.id)
            }.hasMessageContaining("Kan ikke slette vilkårperiodegrunnlag fordi behandlingen har status Ferdigstilt.")
        }

        @Test
        fun `skal slette grunnlag for behandling under arbeid`() {
            vilkårperiodeGrunnlagService.hentEllerOpprettGrunnlag(revurdering.id, Vilkårperioder(emptyList(), emptyList()))

            nullstillBehandlingService.slettVilkårperiodegrunnlag(revurdering.id)

            assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(revurdering.id)).isNull()
        }
    }

    private fun assertVilkårPeriodeErGjenbrukt(vilkårperiode: Vilkårperiode) {
        with(vilkårperiodeRepository.findByBehandlingId(revurdering.id).single()) {
            assertThat(this)
                .usingRecursiveComparison()
                .ignoringFields("id", "sporbar", "behandlingId", "forrigeVilkårperiodeId", "status")
                .isEqualTo(vilkårperiode)
            assertThat(forrigeVilkårperiodeId).isEqualTo(vilkårperiode.id)
            assertThat(status).isEqualTo(Vilkårstatus.UENDRET)
        }
    }

    private fun assertVilkårErGjenbrukt(vilkår: Vilkår) {
        with(vilkårRepository.findByBehandlingId(revurdering.id).single()) {
            assertThat(this)
                .usingRecursiveComparison()
                .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår", "status")
                .isEqualTo(vilkår)
            assertThat(barnId).isEqualTo(revurderingBarn1.id)
            assertThat(opphavsvilkår).isEqualTo(Opphavsvilkår(behandling.id, vilkår.sporbar.endret.endretTid))
            assertThat(status).isEqualTo(VilkårStatus.UENDRET)
        }
    }

    private fun assertIngenDataPåRevurdering() {
        assertThat(vilkårperiodeRepository.findByBehandlingId(revurdering.id)).isEmpty()
        assertThat(vilkårRepository.findByBehandlingId(revurdering.id)).isEmpty()
    }

    private fun opprettBarn() {
        barnService.opprettBarn(listOf(behandlingBarn1))
        barnService.opprettBarn(listOf(revurderingBarn1, revurderingBarn2))
    }

    private fun opprettVilkårperiode(behandlingId: BehandlingId) {
        vilkårperiodeRepository.insert(målgruppe(behandlingId = behandlingId))
    }

    private fun opprettVilkår(
        behandlingId: BehandlingId,
        barnId: BarnId,
    ) {
        vilkårRepository.insert(vilkår(behandlingId = behandlingId, type = VilkårType.PASS_BARN, barnId = barnId))
    }
}
