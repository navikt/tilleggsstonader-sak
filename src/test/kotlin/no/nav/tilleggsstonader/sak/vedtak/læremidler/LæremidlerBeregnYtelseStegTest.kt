package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class LæremidlerBeregnYtelseStegTest(
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
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
    }

    @Nested
    inner class Innvilgelse {
        @Test
        fun `skal lagre vedtak`() {
            val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
            val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))

            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(aktivitet)
            vilkårperiodeRepository.insert(målgruppe)

            steg.utførSteg(saksbehandling, innvilgelse)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<InnvilgelseLæremidler>()
            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
            with(vedtak.data.vedtaksperioder.single()) {
                assertThat(this.fom).isEqualTo(fom)
                assertThat(this.tom).isEqualTo(tom)
            }
            assertThat(vedtak.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
        }
    }

    @Nested
    inner class Opphør {
        @Test
        fun `skal lagre vedtak`() {
            val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
            val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))

            vilkårperiodeRepository.insertAll(listOf(målgruppe, aktivitet))
            stønadsperiodeRepository.insert(stønadsperiode)

            steg.utførSteg(saksbehandling, innvilgelse)

            testoppsettService.ferdigstillBehandling(behandling = behandling)
            val behandlingForOpphør =
                testoppsettService
                    .opprettRevurdering(
                        revurderFra = LocalDate.of(2025, 2, 1),
                        forrigeBehandling = behandling,
                        fagsak = fagsak,
                    ).let { testoppsettService.hentSaksbehandling(it.id) }

            val opphør =
                OpphørLæremidlerRequest(
                    årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "en begrunnelse",
                )
            steg.utførSteg(behandlingForOpphør, opphør)

            val vedtak = repository.findByIdOrThrow(behandlingForOpphør.id).withTypeOrThrow<OpphørLæremidler>()
            assertThat(vedtak.behandlingId).isEqualTo(behandlingForOpphør.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
            with(vedtak.data.vedtaksperioder.single()) {
                assertThat(this.fom).isEqualTo(fom)
                assertThat(this.tom).isEqualTo(LocalDate.of(2025, 1, 31))
            }
            assertThat(vedtak.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
        }
    }

    @Nested
    inner class Avslag {
        @Test
        fun `skal lagre vedtak`() {
            val request =
                AvslagLæremidlerDto(
                    årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                    begrunnelse = "en begrunnelse",
                )
            steg.utførSteg(saksbehandling, request)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<AvslagLæremidler>()
            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.AVSLAG)
            assertThat(vedtak.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
            val forventetAvslag =
                AvslagLæremidler(
                    årsaker = request.årsakerAvslag,
                    begrunnelse = request.begrunnelse,
                )
            assertThat(vedtak.data).isEqualTo(forventetAvslag)
        }
    }

    // Ved ny sats må man oppdatere datoer her
    @Test
    fun `skal splitte andeler i 2, en for høsten og en for våren som ikke har bekreftet sats ennå`() {
        val fom = LocalDate.of(2025, 8, 15)
        val tom = LocalDate.of(2026, 4, 30)
        val datoUtbetalingDel1 = LocalDate.of(2025, 8, 15)
        val datoUtbetalingDel2 = LocalDate.of(2026, 1, 1)

        lagreAktivitetOgStønadsperiode(fom, tom)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
        val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))
        steg.utførSteg(saksbehandling, innvilgelse)

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandling.id)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }

        with(andeler[0]) {
            assertThat(this.fom).isEqualTo(datoUtbetalingDel1)
            assertThat(this.tom).isEqualTo(datoUtbetalingDel1)
            assertThat(beløp).isEqualTo(4505)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        with(andeler[1]) {
            assertThat(this.fom).isEqualTo(datoUtbetalingDel2)
            assertThat(this.tom).isEqualTo(datoUtbetalingDel2)
            assertThat(beløp).isEqualTo(3604)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
        assertThat(andeler).hasSize(2)
    }

    @Test
    fun `Skal utbetale første ukedag i måneden dersom første dagen i måneden er lørdag eller søndag`() {
        val fom = LocalDate.of(2024, 8, 1)
        val tom = LocalDate.of(2025, 4, 30)
        val mandagEtterFom = LocalDate.of(2024, 12, 2)

        lagreAktivitetOgStønadsperiode(fom, tom)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode =
            vedtaksperiodeDto(
                id = UUID.randomUUID(),
                fom = LocalDate.of(2024, 12, 1),
                tom = LocalDate.of(2024, 12, 31),
            )
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        val andeler =
            tilkjentYtelseRepository
                .findByBehandlingId(behandling.id)!!
                .andelerTilkjentYtelse
                .sortedBy { it.fom }
        with(andeler.single()) {
            assertThat(this.fom).isEqualTo(mandagEtterFom)
            assertThat(this.tom).isEqualTo(mandagEtterFom)
            assertThat(beløp).isEqualTo(875)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
        }
    }

    @Test
    fun `en vedtaksperiode med 2 ulike målgrupper skal bli 2 ulike andeler med ulike typer som betales ut samtidig`() {
        val førsteJan = LocalDate.of(2025, 1, 1)
        val sisteJan = LocalDate.of(2025, 1, 31)
        val førsteFeb = LocalDate.of(2025, 2, 1)
        val sisteFeb = LocalDate.of(2025, 2, 28)

        val stønadsperiode =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = førsteJan,
                tom = sisteJan,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.UTDANNING,
            )
        val stønadsperiode2 =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = førsteFeb,
                tom = sisteFeb,
                målgruppe = MålgruppeType.OVERGANGSSTØNAD,
                aktivitet = AktivitetType.UTDANNING,
            )
        val aktivitet =
            aktivitet(
                behandlingId = behandling.id,
                fom = førsteJan,
                tom = sisteFeb,
                faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(type = AktivitetType.UTDANNING),
            )
        stønadsperiodeRepository.insertAll(listOf(stønadsperiode, stønadsperiode2))
        vilkårperiodeRepository.insert(aktivitet)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode =
            vedtaksperiodeDto(id = UUID.randomUUID(), fom = førsteJan, tom = sisteFeb)
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
        assertThat(andeler).hasSize(2)
        with(andeler.single { it.type == TypeAndel.LÆREMIDLER_AAP }) {
            assertThat(this.fom).isEqualTo(førsteJan)
            assertThat(this.tom).isEqualTo(førsteJan)
            assertThat(beløp).isEqualTo(901)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
            assertThat(utbetalingsdato).isEqualTo(førsteJan)
        }
        with(andeler.single { it.type == TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER }) {
            assertThat(this.fom).isEqualTo(førsteJan)
            assertThat(this.tom).isEqualTo(førsteJan)
            assertThat(beløp).isEqualTo(901)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
            assertThat(utbetalingsdato).isEqualTo(førsteJan)
        }
    }

    @Test
    fun `en vedtaksperiode med 2 ulike målgrupper men samme type andel skal bli 1 andel`() {
        val vedtaksperiodeStatus = UUID.randomUUID()
        val førsteJan = LocalDate.of(2025, 1, 1)
        val sisteJan = LocalDate.of(2025, 1, 31)
        val førsteFeb = LocalDate.of(2025, 2, 1)
        val sisteFeb = LocalDate.of(2025, 2, 28)

        val stønadsperiode =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = førsteJan,
                tom = sisteJan,
                målgruppe = MålgruppeType.AAP,
            )
        val stønadsperiode2 =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = førsteFeb,
                tom = sisteFeb,
                målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE,
            )
        val aktivitet =
            aktivitet(
                behandlingId = behandling.id,
                fom = førsteJan,
                tom = sisteFeb,
                faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
            )
        stønadsperiodeRepository.insertAll(listOf(stønadsperiode, stønadsperiode2))
        vilkårperiodeRepository.insert(aktivitet)
        val saksbehandling = testoppsettService.hentSaksbehandling(behandling.id)

        val vedtaksperiode =
            vedtaksperiodeDto(
                vedtaksperiodeStatus,
                fom = førsteJan,
                tom = sisteFeb,
            )
        steg.utførSteg(saksbehandling, InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode)))

        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.andelerTilkjentYtelse
        with(andeler.single()) {
            assertThat(this.fom).isEqualTo(førsteJan)
            assertThat(this.tom).isEqualTo(førsteJan)
            assertThat(beløp).isEqualTo(901 * 2)
            assertThat(type).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            assertThat(statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
            assertThat(satstype).isEqualTo(Satstype.DAG)
            assertThat(utbetalingsdato).isEqualTo(førsteJan)
        }
    }

    @Test
    fun `skal ikke lagre vedtak hvis revurdering ikke har forrige behandling`() {
    }

    fun lagreAktivitetOgStønadsperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        val stønadsperiode =
            stønadsperiode(
                behandlingId = behandling.id,
                fom = fom,
                tom = tom,
            )
        val aktivitet =
            aktivitet(
                behandlingId = behandling.id,
                fom = fom,
                tom = tom,
                faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
            )
        stønadsperiodeRepository.insert(stønadsperiode)
        vilkårperiodeRepository.insert(aktivitet)
    }
}
