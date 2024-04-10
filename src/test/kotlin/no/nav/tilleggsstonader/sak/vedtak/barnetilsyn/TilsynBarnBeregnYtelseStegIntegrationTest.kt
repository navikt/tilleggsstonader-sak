package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
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
        vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                barnId = barn.id,
                type = VilkårType.PASS_BARN,
                resultat = Vilkårsresultat.OPPFYLT,
            ),
        )
    }

    @Nested
    inner class Innvilgelse {

        @Test
        fun `skal lagre vedtak`() {
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(aktivitet)

            val vedtakDto = innvilgelseDto(
                utgifter = mapOf(barn(barn.id, Utgift(januar, januar, 100))),
            )
            steg.utførSteg(saksbehandling, vedtakDto)

            val vedtak = repository.findByIdOrThrow(saksbehandling.id)

            assertThat(vedtak.behandlingId).isEqualTo(saksbehandling.id)
            assertThat(vedtak.type).isEqualTo(TypeVedtak.INNVILGET)
            assertThat(vedtak.vedtak).isEqualTo(
                VedtaksdataTilsynBarn(
                    utgifter = vedtakDto.utgifter,
                ),
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

            val vedtakDto = innvilgelseDto(
                mapOf(
                    barn(
                        barn.id,
                        Utgift(januar, februar, 100),
                        Utgift(mars, april, 200),
                    ),
                ),
            )
            steg.utførSteg(saksbehandling, vedtakDto)

            val dagsatsForUtgift100 = BigDecimal("2.95")
            val dagsatsForUtgift200 = BigDecimal("5.91")

            val forventedeAndeler = listOf(
                Pair(
                    stønadsperiode1.kopierMedLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 5),
                ),
                Pair(
                    stønadsperiode2.kopierMedLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 2),
                ),
                Pair(
                    stønadsperiode3.kopierMedLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 6),
                ),
                Pair(
                    stønadsperiode3.copy(fom = februar.atDay(1), tom = februar.atDay(1)),
                    finnTotalbeløp(dagsatsForUtgift100, 3),
                ),
                Pair(
                    stønadsperiode4.kopierMedLikTomSomFom(),
                    finnTotalbeløp(dagsatsForUtgift100, 1),
                ),
                Pair(
                    stønadsperiode4.copy(fom = mars.atDay(1), tom = mars.atDay(1)),
                    finnTotalbeløp(dagsatsForUtgift200, 23),
                ),
                Pair(
                    stønadsperiode4.copy(fom = april.atDay(1), tom = april.atDay(1)),
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
    }

    @Nested
    inner class MålgruppeMapping {
        val beløp1DagUtgift100 = 3
        val vedtakDto = innvilgelseDto(
            mapOf(
                barn(
                    barn.id,
                    Utgift(januar, mars, 100),
                ),
            ),
        )

        @BeforeEach
        fun setUp() {
            vilkårperiodeRepository.insert(
                aktivitet(
                    behandling.id,
                    fom = januar.atDay(1),
                    tom = april.atEndOfMonth(),
                ),
            )
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

            steg.utførSteg(saksbehandling, vedtakDto)

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

            steg.utførSteg(saksbehandling, vedtakDto)

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

            steg.utførSteg(saksbehandling, vedtakDto)

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
                steg.utførSteg(
                    saksbehandling,
                    vedtakDto,
                )
            }.hasMessageContaining("Kan ikke opprette andel tilkjent ytelse for målgruppe")
        }
    }

    @Nested
    inner class ValideringInnvilgelse {
        @Test
        fun `skal validere at det kun sendes inn utgifter på barn som har oppfylte vilkår`() {
            stønadsperiodeRepository.insert(stønadsperiode)
            vilkårperiodeRepository.insert(aktivitet)

            val vedtak = innvilgelseDto(
                utgifter = mapOf(barn(UUID.randomUUID(), Utgift(januar, januar, utgift))),
            )

            assertThatThrownBy {
                steg.utførSteg(
                    saksbehandling,
                    vedtak,
                )
            }.hasMessageContaining("Det finnes utgifter på barn som ikke har oppfylt vilkårsvurdering")
        }
    }

    private fun Stønadsperiode.kopierMedLikTomSomFom() = copy(tom = fom)

    private fun finnTotalbeløp(dagsats: BigDecimal, antallDager: Int): Int {
        return dagsats.multiply(antallDager.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }
}
