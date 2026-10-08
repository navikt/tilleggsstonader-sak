package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FlyttingAndelTilkjentYtelseMapperTest {
    @Test
    fun `mapper hver TSO-målgruppe til egen flytteandel og flytter helgedato til mandag`() {
        val forventedeTyper =
            mapOf(
                FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE to TypeAndel.FLYTTING_AAP,
                FaktiskMålgruppe.ENSLIG_FORSØRGER to TypeAndel.FLYTTING_ENSLIG_FORSØRGER,
                FaktiskMålgruppe.GJENLEVENDE to TypeAndel.FLYTTING_ETTERLATTE,
                FaktiskMålgruppe.AKTIVITETSPENGER to TypeAndel.FLYTTING_AKTIVITETSPENGER,
            )
        val lørdag = 3 januar 2026
        val mandag = 5 januar 2026

        forventedeTyper.forEach { (målgruppe, typeAndel) ->
            val vedtak = listOf(vedtaksperiode(fom = 1 januar 2026, tom = 31 januar 2026, målgruppe = målgruppe))
            val andel =
                resultat(lørdag).mapTilAndeler(Stønadstype.FLYTTING_TSO, vedtak).single()

            assertThat(andel.type).isEqualTo(typeAndel)
            assertThat(andel.satstype).isEqualTo(Satstype.DAG)
            assertThat(andel.fom).isEqualTo(mandag)
            assertThat(andel.tom).isEqualTo(mandag)
            assertThat(andel.utbetalingsdato).isEqualTo(mandag)
            assertThat(andel.beløp).isEqualTo(100)
        }
    }

    @Test
    fun `mapper TSR til tiltak-andel`() {
        val andel =
            resultat(1 januar 2026)
                .mapTilAndeler(
                    Stønadstype.FLYTTING_TSR,
                    listOf(vedtaksperiode(fom = 1 januar 2026, tom = 31 januar 2026)),
                ).single()

        assertThat(andel.type).isEqualTo(TypeAndel.FLYTTING_ARBEIDSSØKER)
    }

    @Test
    fun `ubekreftet sats venter på satsendring før iverksetting`() {
        val grunnlag =
            BeregningsgrunnlagEgenKjøring(
                avstandEnVei = 100,
                sats = BigDecimal("2.94"),
                satsBekreftet = false,
                henger = BigDecimal.ZERO,
                bompenger = BigDecimal.ZERO,
                ferge = BigDecimal.ZERO,
                parkering = BigDecimal.ZERO,
            )
        val beregningsresultat =
            resultat(1 januar 2026).let { resultat ->
                BeregningsresultatFlytting(
                    resultater =
                        listOf(
                            resultat.resultater.single().copy(
                                grunnlag = grunnlag,
                            ),
                        ),
                )
            }
        val andel =
            beregningsresultat
                .mapTilAndeler(
                    Stønadstype.FLYTTING_TSO,
                    listOf(vedtaksperiode(fom = 1 januar 2026, tom = 31 januar 2026)),
                ).single()

        assertThat(andel.statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
    }

    @Test
    fun `avviser manglende eller tvetydig TSO-målgruppe`() {
        val dato = 1 januar 2026
        assertThatThrownBy {
            resultat(dato).mapTilAndeler(Stønadstype.FLYTTING_TSO, emptyList())
        }.isInstanceOf(Feil::class.java)

        assertThatThrownBy {
            resultat(dato).mapTilAndeler(
                Stønadstype.FLYTTING_TSO,
                listOf(
                    vedtaksperiode(fom = dato, tom = 31 januar 2026, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
                    vedtaksperiode(fom = dato, tom = 31 januar 2026, målgruppe = FaktiskMålgruppe.GJENLEVENDE),
                ),
            )
        }.isInstanceOf(Feil::class.java)
    }

    @Test
    fun `udokumentert byrå gir ingen andeler for TSO eller TSR`() {
        listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR).forEach { stønadstype ->
            assertThat(
                resultat(1 januar 2026, erBetalingDokumentert = false).mapTilAndeler(stønadstype, emptyList()),
            ).isEmpty()
        }
    }

    @Test
    fun `blandet beregning gir kun andeler for dokumentert byrå og egen kjøring`() {
        val dato = 1 januar 2026
        val egenKjøring =
            resultat(dato).resultater.single().copy(
                beløp = BigDecimal("294"),
                grunnlag =
                    BeregningsgrunnlagEgenKjøring(
                        avstandEnVei = 100,
                        sats = BigDecimal("2.94"),
                        satsBekreftet = true,
                        henger = BigDecimal.ZERO,
                        bompenger = BigDecimal.ZERO,
                        ferge = BigDecimal.ZERO,
                        parkering = BigDecimal.ZERO,
                    ),
            )
        val blandet =
            BeregningsresultatFlytting(
                resultat(dato, erBetalingDokumentert = false).resultater +
                    resultat(dato, erBetalingDokumentert = true).resultater +
                    egenKjøring,
            )
        listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR).forEach { stønadstype ->
            val andeler = blandet.mapTilAndeler(stønadstype, listOf(vedtaksperiode(dato, 31 januar 2026)))
            assertThat(andeler.map { it.beløp }).containsExactly(100, 294)
            assertThat(blandet.resultater.map { it.beløp }).containsExactly(
                BigDecimal("100"),
                BigDecimal("100"),
                BigDecimal("294"),
            )
        }
    }

    private fun resultat(
        fom: java.time.LocalDate,
        erBetalingDokumentert: Boolean = true,
    ) = BeregningsresultatFlytting(
        listOf(
            BeregningsresultatFlyttevilkår(
                fom = fom,
                tom = fom.plusDays(10),
                grunnlag = BeregningsgrunnlagFlyttebyrå(BigDecimal("100"), BigDecimal("120"), erBetalingDokumentert),
                beløp = BigDecimal("100"),
            ),
        ),
    )
}
