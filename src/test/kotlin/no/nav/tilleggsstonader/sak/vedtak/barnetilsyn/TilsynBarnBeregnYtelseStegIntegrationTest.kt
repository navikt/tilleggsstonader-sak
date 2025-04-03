package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.resetMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TilsynBarnBeregnYtelseStegIntegrationTest(
    @Autowired
    val steg: TilsynBarnBeregnYtelseSteg,
    @Autowired
    val repository: VedtakRepository,
    @Autowired
    val barnRepository: BarnRepository,
    @Autowired
    val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    val vilkårperiodeRepository: VilkårperiodeRepository,
    @Autowired
    val vilkårRepository: VilkårRepository,
    @Autowired
    val vedtakService: VedtakService,
) : IntegrationTest() {
    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak)
    val saksbehandling = saksbehandling(behandling = behandling)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val vedtaksperiode =
        VedtaksperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 31),
            målgruppeType = NEDSATT_ARBEIDSEVNE,
            aktivitetType = AktivitetType.TILTAK,
        )
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val målgruppe = målgruppe(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 2, 28))

    val januar = YearMonth.of(2023, 1)
    val februar = YearMonth.of(2023, 2)
    val mars = YearMonth.of(2023, 3)
    val april = YearMonth.of(2023, 4)
    val utgift = 100

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        barnRepository.insert(barn)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(unleashService)
    }

    @Nested
    inner class Innvilgelse {
        @Test
        fun `skal lagre vedtak`() {
            vilkårperiodeRepository.insert(aktivitet)
            vilkårperiodeRepository.insert(målgruppe)
            lagVilkårForPeriode(saksbehandling, januar, januar, 100)

            val vedtakDto = innvilgelseDto(listOf(vedtaksperiode))
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<InnvilgelseTilsynBarn>()

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
            assertThat(vedtak.data.beregningsresultat.perioder).hasSize(1)
            assertThat(vedtak.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
        }

        @Test
        fun `skal lagre andeler for hver vedtakssperiode, splittede per måned`() {
            val vedtaksperiode1 = vedtaksperiode.copy(fom = januar.atDay(2), tom = januar.atDay(6))
            val vedtaksperiode2 = vedtaksperiode.copy(fom = januar.atDay(10), tom = januar.atDay(11))
            val vedtaksperiode3 = vedtaksperiode.copy(fom = januar.atDay(24), tom = februar.atDay(3))
            val vedtaksperiode4 = vedtaksperiode.copy(fom = februar.atDay(28), tom = april.atDay(3))

            vilkårperiodeRepository.insert(aktivitet(behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            vilkårperiodeRepository.insert(målgruppe(behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            lagVilkårForPeriode(saksbehandling, januar, februar, 100)
            lagVilkårForPeriode(saksbehandling, mars, april, 200)

            steg.utførOgReturnerNesteSteg(
                saksbehandling,
                innvilgelseDto(
                    listOf(
                        vedtaksperiode1,
                        vedtaksperiode2,
                        vedtaksperiode3,
                        vedtaksperiode4,
                    ),
                ),
            )

            val dagsatsForUtgift100 = BigDecimal("2.95")
            val dagsatsForUtgift200 = BigDecimal("5.91")

            val forventedeAndeler =
                listOf(
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = vedtaksperiode1.fom,
                        beløp = finnTotalbeløp(dagsatsForUtgift100, 5),
                        utbetalingsdato = januar.atDay(2),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = vedtaksperiode2.fom,
                        beløp = finnTotalbeløp(dagsatsForUtgift100, 2),
                        utbetalingsdato = januar.atDay(2),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = vedtaksperiode3.fom,
                        beløp = finnTotalbeløp(dagsatsForUtgift100, 6),
                        utbetalingsdato = januar.atDay(2),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = februar.atDay(1),
                        beløp = finnTotalbeløp(dagsatsForUtgift100, 3),
                        utbetalingsdato = februar.atDay(1),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = vedtaksperiode4.fom,
                        beløp = finnTotalbeløp(dagsatsForUtgift100, 1),
                        utbetalingsdato = februar.atDay(1),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = mars.atDay(1),
                        beløp = finnTotalbeløp(dagsatsForUtgift200, 23),
                        utbetalingsdato = mars.atDay(1),
                    ),
                    andelTilkjentYtelse(
                        kildeBehandlingId = behandling.id,
                        fom = april.atDay(3),
                        beløp = finnTotalbeløp(dagsatsForUtgift200, 1),
                        utbetalingsdato = april.atDay(3),
                    ),
                )
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(forventedeAndeler)
        }

        @Test
        fun `hvis en vedtakssperiode begynner en helgdag skal man opprette vedtakssperiode og andeler som begynner neste mandag`() {
            val juni = YearMonth.of(2024, 6)

            val vedtaksperiode1 =
                vedtaksperiode.copy(
                    fom = juni.atDay(1),
                    tom = juni.atEndOfMonth(),
                )
            vilkårperiodeRepository.insert(aktivitet(behandling.id, fom = juni.atDay(1), tom = juni.atEndOfMonth()))
            vilkårperiodeRepository.insert(målgruppe(behandling.id, fom = juni.atDay(1), tom = juni.atEndOfMonth()))
            lagVilkårForPeriode(saksbehandling, juni, juni, 100)
            steg.utførOgReturnerNesteSteg(saksbehandling, innvilgelseDto(listOf(vedtaksperiode1)))

            with(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.single()) {
                assertThat(this.fom).isEqualTo(juni.atDay(3))
                assertThat(this.tom).isEqualTo(juni.atDay(3))
            }

            val beregningsresultat =
                vedtakService
                    .hentVedtak<InnvilgelseTilsynBarn>(behandling.id)!!
                    .data.beregningsresultat
            with(beregningsresultat!!.perioder.single()) {
                with(this.grunnlag.vedtaksperiodeGrunnlag.single()) {
                    assertThat(this.vedtaksperiode.fom).isEqualTo(juni.atDay(1))
                    assertThat(this.vedtaksperiode.tom).isEqualTo(juni.atEndOfMonth())
                }

                with(this.beløpsperioder.single()) {
                    assertThat(this.dato).isEqualTo(juni.atDay(3))
                }
            }
        }
    }

    @Nested
    inner class Opphør {
        @Test
        fun `skal lagre vedtak`() {
            val beløpsperioderJanuar =
                listOf(Beløpsperiode(dato = LocalDate.of(2023, 1, 2), beløp = 1000, målgruppe = NEDSATT_ARBEIDSEVNE))
            val beløpsperiodeFebruar =
                listOf(Beløpsperiode(dato = LocalDate.of(2023, 2, 1), beløp = 2000, målgruppe = NEDSATT_ARBEIDSEVNE))
            val beregningsresultatJanuar =
                beregningsresultatForMåned(beløpsperioder = beløpsperioderJanuar, måned = YearMonth.of(2023, 1))
            val beregningsresultatFebruar =
                beregningsresultatForMåned(beløpsperioder = beløpsperiodeFebruar, måned = YearMonth.of(2023, 2))

            val vedtakBeregningsresultatFørstegangsbehandling =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            beregningsresultatJanuar,
                            beregningsresultatFebruar,
                        ),
                )

            val vedtaksperiode =
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2023, 1, 2),
                    tom = LocalDate.of(2023, 2, 28),
                    målgruppe = NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                )

            testoppsettService.lagVedtak(
                behandling = behandling,
                beregningsresultat = vedtakBeregningsresultatFørstegangsbehandling,
                vedtaksperioder = listOf(vedtaksperiode),
            )
            testoppsettService.ferdigstillBehandling(behandling = behandling)

            val behandlingForOpphør =
                testoppsettService.opprettRevurdering(
                    revurderFra = LocalDate.of(2023, 2, 1),
                    forrigeBehandling = behandling,
                    fagsak = fagsak,
                )
            val saksbehandlingForOpphør = saksbehandling(behandling = behandlingForOpphør)
            val aktivitetForOpphør =
                aktivitet(
                    behandlingForOpphør.id,
                    fom = LocalDate.of(2023, 1, 2),
                    tom = LocalDate.of(2023, 1, 31),
                    status = Vilkårstatus.ENDRET,
                )
            val målgruppeForOpphør =
                målgruppe.copy(behandlingId = behandlingForOpphør.id, status = Vilkårstatus.UENDRET)

            vilkårperiodeRepository.insert(aktivitetForOpphør)
            vilkårperiodeRepository.insert(målgruppeForOpphør)
            lagVilkårForPeriode(saksbehandlingForOpphør, januar, februar, 100, status = VilkårStatus.UENDRET)

            val vedtakDto = opphørDto()
            steg.utførOgReturnerNesteSteg(saksbehandlingForOpphør, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandlingForOpphør.id).withTypeOrThrow<OpphørTilsynBarn>()

            val tilkjentYtelse =
                tilkjentYtelseRepository.findByBehandlingId(saksbehandlingForOpphør.id)!!.andelerTilkjentYtelse

            val forventetVedtaksperioderForOpphør = vedtaksperiode.copy(tom = LocalDate.of(2023, 1, 31))

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandlingForOpphør.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
            assertThat(vedtak.data.årsaker).containsExactly(ÅrsakOpphør.ENDRING_UTGIFTER)
            assertThat(vedtak.data.begrunnelse).isEqualTo("Endring i utgifter")
            assertThat(
                vedtak.data.beregningsresultat.perioder
                    .single(),
            ).isEqualTo(beregningsresultatJanuar)
            assertThat(tilkjentYtelse).hasSize(1)
            assertThat(vedtak.data.vedtaksperioder).isEqualTo(listOf(forventetVedtaksperioderForOpphør))
            assertThat(vedtak.gitVersjon).isEqualTo(Applikasjonsversjon.versjon)
        }
    }

    @Nested
    inner class RevurderingSkalBeholdePerioderFraForrigeBehandling {
        @Test
        fun `skal beholde perioder fra forrige behandling som er før måneden for revurderFra`() {
            innvilgPerioderForJanuarOgFebruar(behandling.id)
            assertHarPerioderForJanuarOgFebruar(behandling.id)

            testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
            val revurdering = opprettRevurdering(revurderFra = mars.atDay(1))

            innvilgPerioderForMars(revurdering)

            assertHarPerioderForJanuarTilMars(revurdering.id)
        }

        private fun opprettRevurdering(revurderFra: LocalDate?) =
            testoppsettService
                .lagre(
                    behandling(
                        fagsak = fagsak(id = behandling.fagsakId),
                        type = BehandlingType.REVURDERING,
                        revurderFra = revurderFra,
                        forrigeIverksatteBehandlingId = behandling.id,
                    ),
                    opprettGrunnlagsdata = true,
                ).let { testoppsettService.hentSaksbehandling(it.id) }

        /**
         * vedtakssperiode jan-feb
         * Aktivitet jan-april
         * Vilkår(utgifter) jan-feb
         */
        private fun innvilgPerioderForJanuarOgFebruar(behandlingId: BehandlingId) {
            val behandling = testoppsettService.hentSaksbehandling(behandlingId)

            val vedtaksperiode = vedtaksperiode.copy(fom = januar.atDay(1), tom = februar.atEndOfMonth())
            vilkårperiodeRepository.insert(aktivitet(behandlingId = behandlingId, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            vilkårperiodeRepository.insert(målgruppe(behandlingId = behandlingId, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            lagVilkårForPeriode(behandling, januar, februar, 100)
            steg.utførOgReturnerNesteSteg(behandling, innvilgelseDto(listOf(vedtaksperiode)))
        }

        /**
         * Ikke helt reellt tilfelle. Vanligvis når man oppretter en revurdering gjenbruker man vilkårperioder, vedtakssperiode og vilkår fra forrige behandling
         * Dette er mest for å vise at man faktiskt beholder beregningsresultat fra forrige behandling
         * vedtakssperiode jan-mars og mars-mars
         * Aktivitet jan-april
         * Vilkår(utgifter) jan-april
         */
        private fun innvilgPerioderForMars(behandling: Saksbehandling) {
            val vedtaksperiodeJanFeb = vedtaksperiode.copy(fom = januar.atDay(1), tom = mars.atDay(14))
            val vedtaksperiodeMars =
                VedtaksperiodeDto(
                    id = UUID.randomUUID(),
                    fom = mars.atDay(15),
                    tom = mars.atEndOfMonth(),
                    målgruppeType = NEDSATT_ARBEIDSEVNE,
                    aktivitetType = AktivitetType.TILTAK,
                )
            vilkårperiodeRepository.insert(aktivitet(behandlingId = behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            lagVilkårForPeriode(behandling, januar, april, 100)
            steg.utførOgReturnerNesteSteg(behandling, innvilgelseDto(listOf(vedtaksperiodeJanFeb, vedtaksperiodeMars)))
        }

        /**
         * For førstegangsbehandlingen opprettes det kun perioder for jan og feb
         * då det kun finnes overlapp mellom vedtakssperioder og vilkår for januar og mars
         */
        private fun assertHarPerioderForJanuarOgFebruar(behandlingId: BehandlingId) {
            val beregningsresultat = hentBeregningsresultat(behandlingId)
            with(beregningsresultat.perioder.sortedBy { it.grunnlag.måned }) {
                assertThat(this).hasSize(2)
                assertHarPerioderForJanuarOgFebruar(this)
            }
        }

        /**
         * For revurdering gjenbrukes perioder for januar og februar, samt oppretter perioder for mars
         */
        private fun assertHarPerioderForJanuarTilMars(behandlingId: BehandlingId) {
            val beregningsresultat = hentBeregningsresultat(behandlingId)
            with(beregningsresultat.perioder.sortedBy { it.grunnlag.måned }) {
                assertThat(this).hasSize(3)
                // Januar og februar beholder fra forrige behandling, selv om det ikke finnes noen perioder for de denne gangen.
                assertHarPerioderForJanuarOgFebruar(this)

                /**
                 * På grunn av at man revurder fra den 15 mars splittes vedtakssperioder i 2
                 * Det er fordi man i beregningsresultat i behandlingen kun ønsker å se
                 * beløp som blir innvilget fra datoet man revurderer fra
                 */
                assertHarPerioderForMars(this)
            }
        }

        private fun hentBeregningsresultat(behandlingId: BehandlingId): BeregningsresultatTilsynBarn =
            vedtakService
                .hentVedtak<InnvilgelseTilsynBarn>(behandlingId)!!
                .data
                .beregningsresultat

        private fun assertHarPerioderForJanuarOgFebruar(beregningsresultat: List<BeregningsresultatForMåned>) {
            with(beregningsresultat[0].grunnlag.vedtaksperiodeGrunnlag.single()) {
                assertThat(this.vedtaksperiode.fom).isEqualTo(januar.atDay(1))
                assertThat(this.vedtaksperiode.tom).isEqualTo(januar.atEndOfMonth())
            }

            with(beregningsresultat[1].grunnlag.vedtaksperiodeGrunnlag.single()) {
                assertThat(this.vedtaksperiode.fom).isEqualTo(februar.atDay(1))
                assertThat(this.vedtaksperiode.tom).isEqualTo(februar.atEndOfMonth())
            }
        }

        private fun assertHarPerioderForMars(beregningsresultatForMåneds: List<BeregningsresultatForMåned>) {
            assertThat(beregningsresultatForMåneds[2].grunnlag.vedtaksperiodeGrunnlag).hasSize(2)
            with(beregningsresultatForMåneds[2].grunnlag.vedtaksperiodeGrunnlag[0]) {
                assertThat(this.vedtaksperiode.fom).isEqualTo(mars.atDay(1))
                assertThat(this.vedtaksperiode.tom).isEqualTo(mars.atDay(14))
            }
            with(beregningsresultatForMåneds[2].grunnlag.vedtaksperiodeGrunnlag[1]) {
                assertThat(this.vedtaksperiode.fom).isEqualTo(mars.atDay(15))
                assertThat(this.vedtaksperiode.tom).isEqualTo(mars.atDay(31))
            }
        }
    }

    @Nested
    inner class MålgruppeMapping {
        val beløp1DagUtgift100 = 3

        @BeforeEach
        fun setUp() {
            val faktaOgVurderingUføretrygd = faktaOgVurderingMålgruppe(type = MålgruppeType.UFØRETRYGD)
            val faktaOgVurderingNedsattArbeidsevne = faktaOgVurderingMålgruppe(type = MålgruppeType.NEDSATT_ARBEIDSEVNE)
            vilkårperiodeRepository.insert(aktivitet(behandlingId = behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            vilkårperiodeRepository.insert(
                målgruppe(
                    behandlingId = behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingUføretrygd,
                ),
            )
            vilkårperiodeRepository.insert(
                målgruppe(
                    behandlingId = behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingNedsattArbeidsevne,
                    begrunnelse = "nedsatt arbeidsevne",
                ),
            )
            lagVilkårForPeriode(saksbehandling, januar, mars, 100)
        }

        @Test
        fun `skal mappe nedsatt arbeidsevne til riktig TypeAndel`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiode.copy(fom = januar.atDay(2), tom = januar.atDay(2), målgruppeType = NEDSATT_ARBEIDSEVNE),
                )

            val vedtakDto = innvilgelseDto(vedtaksperioder)
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventedeAndeler =
                vedtaksperioder.map {
                    andelTilkjentYtelse(
                        fom = it.fom,
                        tom = it.fom,
                        beløp = beløp1DagUtgift100,
                        kildeBehandlingId = behandling.id,
                        type = TypeAndel.TILSYN_BARN_AAP,
                    )
                }

            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(forventedeAndeler)
        }

        @Test
        fun `skal mappe overgangsstønad til riktig TypeAndel`() {
            val vedtaksperiode =
                VedtaksperiodeDto(
                    id = UUID.randomUUID(),
                    fom = januar.atDay(2),
                    tom = januar.atDay(2),
                    målgruppeType = FaktiskMålgruppe.ENSLIG_FORSØRGER,
                    aktivitetType = AktivitetType.UTDANNING,
                )

            vilkårperiodeRepository.insert(
                aktivitet(
                    behandlingId = behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.UTDANNING),
                ),
            )
            vilkårperiodeRepository.insert(
                målgruppe(
                    behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                ),
            )

            val vedtakDto = innvilgelseDto(listOf(vedtaksperiode))

            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventetAndel =
                andelTilkjentYtelse(
                    fom = vedtaksperiode.fom,
                    tom = vedtaksperiode.fom,
                    beløp = beløp1DagUtgift100,
                    kildeBehandlingId = behandling.id,
                    type = TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
                )
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(listOf(forventetAndel))
        }

        @Test
        fun `skal mappe gjenlevende til riktig TypeAndel`() {
            val vedtaksperiode =
                VedtaksperiodeDto(
                    id = UUID.randomUUID(),
                    fom = januar.atDay(2),
                    tom = januar.atDay(2),
                    målgruppeType = FaktiskMålgruppe.GJENLEVENDE,
                    aktivitetType = AktivitetType.UTDANNING,
                )

            vilkårperiodeRepository.insert(
                aktivitet(
                    behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.UTDANNING),
                ),
            )
            vilkårperiodeRepository.insert(
                målgruppe(
                    behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                    faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OMSTILLINGSSTØNAD),
                ),
            )

            val vedtakDto = innvilgelseDto(listOf(vedtaksperiode))
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventetAndel =
                andelTilkjentYtelse(
                    fom = vedtaksperiode.fom,
                    tom = vedtaksperiode.fom,
                    beløp = beløp1DagUtgift100,
                    kildeBehandlingId = behandling.id,
                    type = TypeAndel.TILSYN_BARN_ETTERLATTE,
                )
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(listOf(forventetAndel))
        }
    }

    private fun lagVilkårForPeriode(
        behandling: Saksbehandling,
        fom: YearMonth,
        tom: YearMonth,
        utgift: Int,
        status: VilkårStatus = VilkårStatus.NY,
    ) {
        vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                barnId = barn.id,
                type = VilkårType.PASS_BARN,
                resultat = Vilkårsresultat.OPPFYLT,
                fom = fom.atDay(1),
                tom = tom.atEndOfMonth(),
                utgift = utgift,
                status = status,
            ),
        )
    }

    private fun finnTotalbeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ): Int =
        dagsats
            .multiply(antallDager.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
}
