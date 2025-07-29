package no.nav.tilleggsstonader.sak.vedtak.læremidler

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class LæremidlerBeregnYtelseStegTest : IntegrationTest() {
    @Autowired
    lateinit var steg: LæremidlerBeregnYtelseSteg

    @Autowired
    lateinit var repository: VedtakRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak)
    val saksbehandling = saksbehandling(behandling = behandling, fagsak = fagsak)

    final val fom = LocalDate.of(2025, 1, 1)
    final val tom = LocalDate.of(2025, 4, 30)

    val aktivitet =
        aktivitet(behandling.id, fom = fom, tom = tom, faktaOgVurdering = faktaOgVurderingAktivitetLæremidler())
    val målgruppe = målgruppe(behandling.id, fom = fom, tom = tom)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
    }

    @Nested
    inner class Innvilgelse {
        @Test
        fun `skal lagre vedtak`() {
            val vedtaksperiode = vedtaksperiodeDto(id = UUID.randomUUID(), fom = fom, tom = tom)
            val innvilgelse = InnvilgelseLæremidlerRequest(vedtaksperioder = listOf(vedtaksperiode))

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
                    opphørsdato = behandlingForOpphør.revurderFra,
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

        @Test
        fun `feiler hvis feature-toggle for utleding av endringsdato er av og revurderFra er null`() {
            testoppsettService.ferdigstillBehandling(behandling = behandling)
            val behandlingForOpphør =
                testoppsettService
                    .opprettRevurdering(
                        revurderFra = null,
                        forrigeBehandling = behandling,
                        fagsak = fagsak,
                    ).let { testoppsettService.hentSaksbehandling(it.id) }

            val opphør =
                OpphørLæremidlerRequest(
                    årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "en begrunnelse",
                    opphørsdato = LocalDate.of(2025, 2, 1),
                )

            every { unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK) } returns false
            assertThatThrownBy {
                steg.utførSteg(behandlingForOpphør, opphør)
            }.hasMessage("revurderFra-dato er påkrevd for opphør")
        }

        @Test
        fun `feiler hvis feature-toggle for utleding av endringsdato er på og opphørsdato er null`() {
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
                    opphørsdato = null,
                )

            every { unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK) } returns true
            assertThatThrownBy {
                steg.utførSteg(behandlingForOpphør, opphør)
            }.hasMessage("opphørsdato er påkrevd for opphør")
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

        lagreMålgruppeOgAktivitet(fom, tom)
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

        lagreMålgruppeOgAktivitet(fom, tom)
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
    fun `skal ikke lagre vedtak hvis revurdering ikke har forrige behandling`() {
    }

    fun lagreMålgruppeOgAktivitet(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        val målgruppe =
            målgruppe(
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
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
    }
}
