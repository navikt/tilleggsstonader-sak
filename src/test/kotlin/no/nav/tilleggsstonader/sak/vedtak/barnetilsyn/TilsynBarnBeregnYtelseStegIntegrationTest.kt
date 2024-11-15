package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.resetMock
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus

class TilsynBarnBeregnYtelseStegIntegrationTest(
    @Autowired
    val steg: TilsynBarnBeregnYtelseSteg,
    @Autowired
    val repository: TilsynBarnVedtakRepository,
    @Autowired
    val barnRepository: BarnRepository,
    @Autowired
    val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    val stønadsperiodeRepository: StønadsperiodeRepository,
    @Autowired
    val vilkårperiodeRepository: VilkårperiodeRepository,
    @Autowired
    val vilkårRepository: VilkårRepository,
    @Autowired
    val tilsynBarnVedtakService: TilsynBarnVedtakService,
) : IntegrationTest() {

    val behandling = behandling()
    val saksbehandling = saksbehandling(behandling = behandling)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val stønadsperiode =
        stønadsperiode(behandlingId = behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))

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
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(aktivitet)
            lagVilkårForPeriode(saksbehandling, januar, januar, 100)

            val vedtakDto = innvilgelseDto()
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id)

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGELSE)
            assertThat(vedtak.vedtak).isEqualTo(
                VedtaksdataTilsynBarn(emptyMap()),
            )
        }

        @Test
        fun `skal lagre andeler for hver stønadsperiode, splittede per måned`() {
            val stønadsperiode1 = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(2),
                tom = januar.atDay(6),
            )
            val stønadsperiode2 = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(10),
                tom = januar.atDay(11),
            )
            val stønadsperiode3 = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(24),
                tom = februar.atDay(3),
            )
            val stønadsperiode4 = stønadsperiode(
                behandlingId = behandling.id,
                fom = februar.atDay(28),
                tom = april.atDay(3),
            )

            stønadsperiodeRepository.insertAll(
                listOf(
                    stønadsperiode1,
                    stønadsperiode2,
                    stønadsperiode3,
                    stønadsperiode4,
                ),
            )
            vilkårperiodeRepository.insert(aktivitet(behandling.id, fom = januar.atDay(1), tom = april.atEndOfMonth()))
            lagVilkårForPeriode(saksbehandling, januar, februar, 100)
            lagVilkårForPeriode(saksbehandling, mars, april, 200)

            steg.utførOgReturnerNesteSteg(saksbehandling, innvilgelseDto())

            val dagsatsForUtgift100 = BigDecimal("2.95")
            val dagsatsForUtgift200 = BigDecimal("5.91")

            val forventedeAndeler = listOf(
                Pair(
                    stønadsperiode1.medLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 5),
                ),
                Pair(
                    stønadsperiode2.medLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 2),
                ),
                Pair(
                    stønadsperiode3.medLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 6),
                ),
                Pair(
                    stønadsperiode3.copy(fom = februar.atDay(1), tom = februar.atDay(1)),
                    finnTotalbeløp(dagsatsForUtgift100, 3),
                ),
                Pair(
                    stønadsperiode4.medLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 1),
                ),
                Pair(
                    stønadsperiode4.copy(fom = mars.atDay(1), tom = mars.atDay(1)),
                    finnTotalbeløp(dagsatsForUtgift200, 23),
                ),
                Pair(
                    stønadsperiode4.copy(fom = april.atDay(3), tom = april.atDay(3)),
                    finnTotalbeløp(dagsatsForUtgift200, 1),
                ),
            ).map {
                andelTilkjentYtelse(
                    fom = it.first.fom,
                    tom = it.first.tom,
                    beløp = it.second,
                    kildeBehandlingId = behandling.id,
                )
            }
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(forventedeAndeler)
        }

        @Test
        fun `hvis en stønadsperiode begynner en helgdag skal man opprette stønadsperioder og andeler som begynner neste mandag`() {
            val juni = YearMonth.of(2024, 6)

            val stønadsperiode1 = stønadsperiode(
                behandlingId = behandling.id,
                fom = juni.atDay(1),
                tom = juni.atEndOfMonth(),
            )
            stønadsperiodeRepository.insert(stønadsperiode1)
            vilkårperiodeRepository.insert(aktivitet(behandling.id, fom = juni.atDay(1), tom = juni.atEndOfMonth()))
            lagVilkårForPeriode(saksbehandling, juni, juni, 100)
            steg.utførOgReturnerNesteSteg(saksbehandling, innvilgelseDto())

            with(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.single()) {
                assertThat(this.fom).isEqualTo(juni.atDay(3))
                assertThat(this.tom).isEqualTo(juni.atDay(3))
            }

            val beregningsresultat = tilsynBarnVedtakService.hentVedtak(behandling.id)!!.beregningsresultat
            with(beregningsresultat!!.perioder.single()) {
                with(this.grunnlag.stønadsperioderGrunnlag.single()) {
                    assertThat(this.stønadsperiode.fom).isEqualTo(juni.atDay(1))
                    assertThat(this.stønadsperiode.tom).isEqualTo(juni.atEndOfMonth())
                }

                with(this.beløpsperioder.single()) {
                    assertThat(this.dato).isEqualTo(juni.atDay(3))
                }
            }
        }
    }

    @Nested
    inner class Opphør {

        val aktivitetForOpphør = aktivitet.copy(status = Vilkårstatus.ENDRET)
        val saksbehandlingForOpphør = saksbehandling.copy(revurderFra = LocalDate.of(2023,2,1))

        @Test
        fun `skal lagre vedtak`() {
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(aktivitetForOpphør)
            lagVilkårForPeriode(saksbehandling, januar, januar, 100, status = VilkårStatus.ENDRET)

            val vedtakDto = opphørDto()
            steg.utførOgReturnerNesteSteg(saksbehandlingForOpphør, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id)

            val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.OPPHØR)
            assertThat(vedtak.årsakerOpphør?.årsaker).containsExactly(ÅrsakOpphør.ENDRING_UTGIFTER)
            assertThat(vedtak.opphørBegrunnelse).isEqualTo("Nye utgifter")
            assertThat(vedtak.beregningsresultat!!.perioder).isEmpty()
            assertThat(tilkjentYtelse).isEmpty()
        }
    }

    @Nested
    inner class RevurderingSkalBeholdePerioderFraForrigeBehandling {

        @Test
        fun `skal beholde perioder fra forrige behandling som er før måneden for revurderFra`() {
            innvilgPerioderForJanuarOgFebruar(behandling.id)
            assertHarPerioderForJanuarOgFebruar(behandling.id)

            testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
            val revurdering = opprettRevurdering(revurderFra = mars.atDay(15))

            innvilgPerioderForMars(revurdering)

            assertHarPerioderForJanuarTilMars(revurdering.id)
        }

        /**
         * Inget reellt tilfelle, men verifiserer at man ikke gjenbruker perioder før revurder-fra-datoet
         * I en revurdering er det egentlige ikke mulig å legge til perioder før revurder-fra
         */
        @Test
        fun `skal ikke ta med perioder før revurder-fra`() {
            innvilgPerioderForJanuarOgFebruar(behandling.id)
            assertHarPerioderForJanuarOgFebruar(behandling.id)

            testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
            val revurdering = opprettRevurdering(revurderFra = april.atDay(15))

            innvilgPerioderForMars(revurdering)

            // Har ikke med periodene for mars som ble lagt inn i revurderingen
            // Men har kopiert perioder for januar og februar fra forrige behandlingen
            assertHarPerioderForJanuarOgFebruar(revurdering.id)
        }

        private fun opprettRevurdering(revurderFra: LocalDate?) = testoppsettService.lagre(
            behandling(
                fagsak = fagsak(id = behandling.fagsakId),
                type = BehandlingType.REVURDERING,
                revurderFra = revurderFra,
                forrigeBehandlingId = behandling.id,
            ),
            opprettGrunnlagsdata = false,
        ).let { testoppsettService.hentSaksbehandling(it.id) }

        /**
         * Stønadsperioder jan-mars
         * Aktivitet jan-april
         * Vilkår(utgifter) jan-feb
         */
        private fun innvilgPerioderForJanuarOgFebruar(behandlingId: BehandlingId) {
            val behandling = testoppsettService.hentSaksbehandling(behandlingId)

            val stønadsperiode = stønadsperiode(
                behandlingId = behandlingId,
                fom = januar.atDay(1),
                tom = mars.atEndOfMonth(),
            )
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(
                aktivitet(
                    behandlingId,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                ),
            )
            lagVilkårForPeriode(behandling, januar, februar, 100)
            steg.utførOgReturnerNesteSteg(behandling, innvilgelseDto())
        }

        /**
         * Ikke helt reellt tilfelle. Vanligvis når man oppretter en revurdering gjenbruker man vilkårperioder, stønadsperioder og vilkår fra forrige behandling
         * Dette er mest for å vise at man faktiskt beholder beregningsresultat fra forrige behandling
         * Stønadsperiode mars-mars
         * Aktivitet mars-april
         * Vilkår(utgifter) mars-april
         */
        private fun innvilgPerioderForMars(behandling: Saksbehandling) {
            val stønadsperiode = stønadsperiode(
                behandlingId = behandling.id,
                fom = mars.atDay(1),
                tom = mars.atEndOfMonth(),
            )
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(
                aktivitet(
                    behandling.id,
                    fom = mars.atDay(1),
                    tom = april.atEndOfMonth(),
                ),
            )
            lagVilkårForPeriode(behandling, mars, april, 100)
            steg.utførOgReturnerNesteSteg(behandling, innvilgelseDto())
        }

        /**
         * For førstegangsbehandlingen opprettes det kun perioder for jan og feb
         * då det kun finnes overlapp mellom stønadsperioder og vilkår for januar og mars
         */
        private fun assertHarPerioderForJanuarOgFebruar(behandlingId: BehandlingId) {
            val beregningsresultat = tilsynBarnVedtakService.hentVedtak(behandlingId)!!.beregningsresultat
            with(beregningsresultat!!.perioder.sortedBy { it.grunnlag.måned }) {
                assertThat(this).hasSize(2)
                assertHarPerioderForJanuarOgFebruar(this)
            }
        }

        /**
         * For revurdering gjenbrukes perioder for januar og februar, samt oppretter perioder for mars
         */
        private fun assertHarPerioderForJanuarTilMars(behandlingId: BehandlingId) {
            val beregningsresultat = tilsynBarnVedtakService.hentVedtak(behandlingId)!!.beregningsresultat
            with(beregningsresultat!!.perioder.sortedBy { it.grunnlag.måned }) {
                assertThat(this).hasSize(3)
                // Januar og februar beholder fra forrige behandling, selv om det ikke finnes noen perioder for de denne gangen.
                assertHarPerioderForJanuarOgFebruar(this)

                /**
                 * På grunn av at man revurder fra den 15 mars splittes stønadsperioder i 2
                 * Det er fordi man i beregningsresultat i behandlingen kun ønsker å se
                 * beløp som blir innvilget fra datoet man revurderer fra
                 */
                assertHarPerioderForMars(this)
            }
        }

        private fun assertHarPerioderForJanuarOgFebruar(beregningsresultat: List<BeregningsresultatForMåned>) {
            with(beregningsresultat[0].grunnlag.stønadsperioderGrunnlag.single()) {
                assertThat(this.stønadsperiode.fom).isEqualTo(januar.atDay(1))
                assertThat(this.stønadsperiode.tom).isEqualTo(januar.atEndOfMonth())
            }

            with(beregningsresultat[1].grunnlag.stønadsperioderGrunnlag.single()) {
                assertThat(this.stønadsperiode.fom).isEqualTo(februar.atDay(1))
                assertThat(this.stønadsperiode.tom).isEqualTo(februar.atEndOfMonth())
            }
        }

        private fun assertHarPerioderForMars(beregningsresultatForMåneds: List<BeregningsresultatForMåned>) {
            assertThat(beregningsresultatForMåneds[2].grunnlag.stønadsperioderGrunnlag).hasSize(2)
            with(beregningsresultatForMåneds[2].grunnlag.stønadsperioderGrunnlag[0]) {
                assertThat(this.stønadsperiode.fom).isEqualTo(mars.atDay(1))
                assertThat(this.stønadsperiode.tom).isEqualTo(mars.atDay(14))
            }
            with(beregningsresultatForMåneds[2].grunnlag.stønadsperioderGrunnlag[1]) {
                assertThat(this.stønadsperiode.fom).isEqualTo(mars.atDay(15))
                assertThat(this.stønadsperiode.tom).isEqualTo(mars.atDay(31))
            }
        }
    }

    @Nested
    inner class MålgruppeMapping {
        val beløp1DagUtgift100 = 3
        val vedtakDto = innvilgelseDto()

        @BeforeEach
        fun setUp() {
            vilkårperiodeRepository.insert(
                aktivitet(
                    behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                ),
            )
            lagVilkårForPeriode(saksbehandling, januar, mars, 100)
        }

        @Test
        fun `skal mappe nedsatt arbeidsevne til riktig TypeAndel`() {
            val stønadsperioder = listOf(
                stønadsperiode(
                    behandlingId = behandling.id,
                    fom = januar.atDay(2),
                    tom = januar.atDay(2),
                    målgruppe = MålgruppeType.AAP,
                ),
                stønadsperiode(
                    behandlingId = behandling.id,
                    fom = februar.atDay(1),
                    tom = februar.atDay(1),
                    målgruppe = MålgruppeType.UFØRETRYGD,
                ),
                stønadsperiode(
                    behandlingId = behandling.id,
                    fom = mars.atDay(1),
                    tom = mars.atDay(1),
                    målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                ),
            )

            stønadsperiodeRepository.insertAll(stønadsperioder)

            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventedeAndeler = stønadsperioder.map {
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
            val stønadsperiode = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(2),
                tom = januar.atDay(2),
                målgruppe = MålgruppeType.OVERGANGSSTØNAD,
            )

            stønadsperiodeRepository.insert(stønadsperiode)

            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventetAndel = andelTilkjentYtelse(
                fom = stønadsperiode.fom,
                tom = stønadsperiode.fom,
                beløp = beløp1DagUtgift100,
                kildeBehandlingId = behandling.id,
                type = TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
            )
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(listOf(forventetAndel))
        }

        @Test
        fun `skal mappe omstillingsstønad til riktig TypeAndel`() {
            val stønadsperiode = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(2),
                tom = januar.atDay(2),
                målgruppe = MålgruppeType.OMSTILLINGSSTØNAD,
            )

            stønadsperiodeRepository.insert(stønadsperiode)

            steg.utførOgReturnerNesteSteg(saksbehandling, vedtakDto)

            val forventetAndel = andelTilkjentYtelse(
                fom = stønadsperiode.fom,
                tom = stønadsperiode.fom,
                beløp = beløp1DagUtgift100,
                kildeBehandlingId = behandling.id,
                type = TypeAndel.TILSYN_BARN_ETTERLATTE,
            )
            assertThat(tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)!!.andelerTilkjentYtelse.toList())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
                .containsExactlyElementsOf(listOf(forventetAndel))
        }

        @Test
        fun `skal kaste feil ved forsøk på å opprette andeler med ugyldig målgruppe`() {
            val stønadsperiode = stønadsperiode(
                behandlingId = behandling.id,
                fom = januar.atDay(2),
                tom = januar.atDay(2),
                målgruppe = MålgruppeType.DAGPENGER,
            )

            stønadsperiodeRepository.insert(stønadsperiode)

            assertThatThrownBy {
                steg.utførOgReturnerNesteSteg(
                    saksbehandling,
                    vedtakDto,
                )
            }.hasMessageContaining("Kan ikke opprette andel tilkjent ytelse for målgruppe")
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
                status = status
            ),
        )
    }

    private fun Stønadsperiode.medLikTomSomFom() = copy(tom = fom)

    private fun finnTotalbeløp(dagsats: BigDecimal, antallDager: Int): Int {
        return dagsats.multiply(antallDager.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }
}
