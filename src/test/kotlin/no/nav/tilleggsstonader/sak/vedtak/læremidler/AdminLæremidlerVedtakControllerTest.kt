package no.nav.tilleggsstonader.sak.vedtak.læremidler

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.resetMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class AdminLæremidlerVedtakControllerTest(
    @Autowired
    val steg: LæremidlerBeregnYtelseSteg,
    @Autowired
    val repository: VedtakRepository,
    @Autowired
    val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    val stønadsperiodeRepository: StønadsperiodeRepository,
    @Autowired
    val vilkårperiodeRepository: VilkårperiodeRepository,
    @Autowired
    val adminLæremidlerVedtakController: AdminLæremidlerVedtakController,
) : IntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak)
    val saksbehandling = saksbehandling(behandling = behandling, fagsak = fagsak)

    final val fom = LocalDate.of(2025, 1, 1)
    final val tom = LocalDate.of(2025, 4, 30)

    val stønadsperiode = stønadsperiode(behandlingId = behandling.id, fom = fom, tom = tom)
    val aktivitet =
        aktivitet(behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingAktivitetLæremidler())
    val målgruppe = målgruppe(behandling.id, fom = fom, tom = tom)

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.LÆREMIDLER_VEDTAKSPERIODER_V2) } returns false
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)

        vilkårperiodeRepository.insert(aktivitet)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(unleashService)
    }

    @Test
    fun `skal ikke oppdatere hvis flagg er oppdater-flagg er false`() {
        val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
        val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))

        vilkårperiodeRepository.insert(målgruppe)
        stønadsperiodeRepository.insert(stønadsperiode)
        steg.utførSteg(saksbehandling, innvilgelse)

        adminLæremidlerVedtakController.kjørOppdatering(oppdater = false)

        val vedtak = repository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseLæremidler>()
        assertThat(vedtak.data.vedtaksperioder).containsExactly(
            Vedtaksperiode(
                id = vedtaksperiode.id,
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom,
                målgruppe = null,
                aktivitet = null,
            ),
        )
    }

    @Test
    fun `skal bruke målgruppe og aktivitet fra stønadsperiode`() {
        val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
        val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))

        vilkårperiodeRepository.insert(målgruppe)
        stønadsperiodeRepository.insert(stønadsperiode)
        steg.utførSteg(saksbehandling, innvilgelse)

        adminLæremidlerVedtakController.kjørOppdatering(oppdater = true)

        val vedtak = repository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseLæremidler>()
        assertThat(vedtak.data.vedtaksperioder).containsExactly(
            Vedtaksperiode(
                id = vedtaksperiode.id,
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        )
    }

    @Test
    fun `skal slå sammen 2 stønadsperioder med samme faktiske målgruppe`() {
        val periode1FomTom = fom
        val periode2FomTom = periode1FomTom.plusDays(1)

        vilkårperiodeRepository.insert(
            målgruppe(
                behandlingId = behandling.id,
                fom = periode1FomTom,
                tom = periode1FomTom,
            ),
        )
        vilkårperiodeRepository.insert(
            målgruppe(
                behandlingId = behandling.id,
                fom = periode2FomTom,
                tom = periode2FomTom,
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.NEDSATT_ARBEIDSEVNE),
                begrunnelse = "begrunnelse",
            ),
        )
        stønadsperiodeRepository.insert(
            stønadsperiode(
                behandlingId = behandling.id,
                fom = periode1FomTom,
                tom = periode1FomTom,
            ),
        )
        stønadsperiodeRepository.insert(
            stønadsperiode(
                behandlingId = behandling.id,
                fom = periode2FomTom,
                tom = periode2FomTom,
                målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE,
            ),
        )
        val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = periode1FomTom, tom = periode2FomTom)
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        adminLæremidlerVedtakController.kjørOppdatering(oppdater = true)

        val vedtak = repository.findByIdOrThrow(behandling.id).withTypeOrThrow<InnvilgelseLæremidler>()
        assertThat(vedtak.data.vedtaksperioder).containsExactly(
            Vedtaksperiode(
                id = vedtaksperiode.id,
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        )
    }

    @Test
    fun `skal feile hvis en vedtaksperiode går over 2 ulike faktiske målgrupper`() {
        val periode1FomTom = fom
        val periode2FomTom = periode1FomTom.plusDays(1)
        vilkårperiodeRepository.insert(
            målgruppe(
                behandlingId = behandling.id,
                fom = periode1FomTom,
                tom = periode2FomTom,
            ),
        )
        vilkårperiodeRepository.insert(
            målgruppe(
                behandlingId = behandling.id,
                fom = periode2FomTom,
                tom = periode2FomTom,
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                begrunnelse = "begrunnelse",
            ),
        )
        val stønadsperiode =
            stønadsperiodeRepository.insert(
                stønadsperiode(
                    behandlingId = behandling.id,
                    fom = periode1FomTom,
                    tom = periode2FomTom,
                ),
            )
        val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = periode1FomTom, tom = periode2FomTom)
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        stønadsperiodeRepository.insert(
            stønadsperiode(
                behandlingId = behandling.id,
                fom = periode2FomTom,
                tom = periode2FomTom,
                målgruppe = MålgruppeType.OVERGANGSSTØNAD,
            ),
        )
        stønadsperiodeRepository.update(stønadsperiode.copy(tom = periode1FomTom))

        assertThatThrownBy {
            adminLæremidlerVedtakController.kjørOppdatering(oppdater = true)
        }.hasMessageContaining("Finner ikke vedtaksperiode som overlapper stønadsperiode")

        // TODO assert har oppdatert vedtaksperioder
    }
}
