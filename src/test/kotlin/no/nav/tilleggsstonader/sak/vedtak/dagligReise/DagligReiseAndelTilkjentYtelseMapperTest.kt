package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil.defaultInnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DagligReiseAndelTilkjentYtelseMapperTest {
    val saksbehandling = saksbehandling(fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO))

    @Nested
    inner class OffentligTransport {
        @Test
        fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
            val mandag = 1 september 2025
            val beregningsresultat =
                BeregningsresultatOffentligTransport(
                    reiser = listOf(lagBeregningsresultatForReise(mandag)),
                )
            val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)
            with(andeler.single()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
            }
        }

        @Test
        fun `flere reiser med samme fom aggregeres til én andel med summert beløp`() {
            val mandag = 1 september 2025
            val beregningsresultat =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            lagBeregningsresultatForReise(mandag, beløp = 100),
                            lagBeregningsresultatForReise(mandag, beløp = 200),
                        ),
                )
            val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)
            assertThat(andeler).hasSize(1)
            assertThat(andeler.single().beløp).isEqualTo(300)
        }

        @Test
        fun `reiser med ulike fom-datoer gir en andel per dato`() {
            val mandag = 1 september 2025
            val tirsdag = 2 september 2025
            val beregningsresultat =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            lagBeregningsresultatForReise(mandag, beløp = 100),
                            lagBeregningsresultatForReise(tirsdag, beløp = 200),
                        ),
                )
            val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)
            assertThat(andeler).hasSize(2)
            with(andeler.first()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
            }
            with(andeler.last()) {
                assertThat(fom).isEqualTo(tirsdag)
                assertThat(tom).isEqualTo(tirsdag)
                assertThat(utbetalingsdato).isEqualTo(tirsdag)
            }
        }

        @Test
        fun `to reiser som starter på ulike helgedager, skal begge utbetales mandagen etter`() {
            val lørdag = 6 september 2025
            val søndag = 7 september 2025
            val mandag = 8 september 2025
            val beregningsresultat =
                BeregningsresultatOffentligTransport(
                    reiser = listOf(lagBeregningsresultatForReise(lørdag), lagBeregningsresultatForReise(søndag)),
                )

            val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)

            assertThat(andeler).hasSize(2)
            with(andeler.first()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(100)
                assertThat(utbetalingsdato).isEqualTo(mandag)
            }
            with(andeler.last()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(100)
                assertThat(utbetalingsdato).isEqualTo(mandag)
            }
        }

        @Test
        fun `to ulike målgrupper på samme dag er ikke støttet enda, og skal derfor feile`() {
            val fredag = 5 september 2025
            val vedtaksperiodeEnsligForsørger =
                listOf(lagVedtaksperiodeGrunnlag(fredag, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER))
            val vedtaksperioderNedsattArbeidsevne =
                listOf(
                    lagVedtaksperiodeGrunnlag(fredag, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
                )
            val beregningsresultat =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            lagBeregningsresultatForReise(
                                fredag,
                                beregningsgrunnlag =
                                    lagBeregningsgrunnlagOffentligTransport(
                                        fom = fredag,
                                        vedtaksperioder = vedtaksperiodeEnsligForsørger,
                                    ),
                            ),
                            lagBeregningsresultatForReise(
                                fredag,
                                beregningsgrunnlag =
                                    lagBeregningsgrunnlagOffentligTransport(
                                        fom = fredag,
                                        vedtaksperioder = vedtaksperioderNedsattArbeidsevne,
                                    ),
                            ),
                        ),
                )

            val message =
                assertThrows<ApiFeil> { beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling) }.message
            assertThat(
                message,
            ).isEqualTo(
                "Vi støtter foreløpig ikke ulike målgrupper på samme utbetaling. Ta kontakt med utvikler teamet hvis du trenger å gjøre dette.",
            )
        }

        @Nested
        inner class FinnPeriodeFraAndel {
            @Test
            fun `finner periode fra andel`() {
                val beregningsresultat = defaultInnvilgelseDagligReise.beregningsresultat
                val andeler = beregningsresultat.offentligTransport!!.mapTilAndelTilkjentYtelse(saksbehandling)

                andeler.forEachIndexed { index, andelTilkjentYtelse ->
                    val periodeFraAndel = finnPeriodeFraAndel(beregningsresultat, andelTilkjentYtelse)
                    assertThat(periodeFraAndel.fom).isEqualTo(
                        beregningsresultat.offentligTransport.reiser
                            .first()
                            .perioder[index]
                            .grunnlag.fom,
                    )
                    assertThat(periodeFraAndel.tom).isEqualTo(
                        beregningsresultat.offentligTransport.reiser
                            .first()
                            .perioder[index]
                            .grunnlag.tom,
                    )
                }
            }
        }
    }

    @Nested
    inner class PrivatBil {
        @Test
        fun `når beregningsresultat ikke har reiser returneres ingen andeler`() {
            val beregningsresultat = BeregningsresultatPrivatBil(reiser = emptyList())

            val andeler =
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    rammevedtakPrivatBil = RammevedtakPrivatBil(reiser = emptyList()),
                )

            assertThat(andeler).isEmpty()
        }

        @Test
        fun `når alle perioder har beløp 0 returneres ingen andeler`() {
            val reiseId = ReiseId.random()
            val mandag = 8 september 2025
            val beregningsresultat =
                BeregningsresultatPrivatBil(
                    reiser =
                        listOf(
                            BeregningsresultatForReisePrivatBil(
                                reiseId = reiseId,
                                perioder =
                                    listOf(
                                        lagBeregningsperiodePrivatBil(1 september 2025, 7 september 2025, 0),
                                        lagBeregningsperiodePrivatBil(8 september 2025, 14 september 2025, 0),
                                    ),
                            ),
                        ),
                )

            val andeler =
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    rammevedtakPrivatBil = lagRammevedtakPrivatBil(listOf(reiseId), 1 september 2025, 30 september 2025),
                )

            assertThat(andeler).isEmpty()
        }

        @Test
        fun `en reise med tre perioder gir tre andeler med fom og tom lik påfølgende mandag`() {
            val reiseId = ReiseId.random()
            val mandagFørsteUke = 1 september 2025
            val mandagAndreUke = 8 september 2025
            val mandagTredjeUke = 15 september 2025
            val beregningsresultat =
                BeregningsresultatPrivatBil(
                    reiser =
                        listOf(
                            BeregningsresultatForReisePrivatBil(
                                reiseId = reiseId,
                                perioder =
                                    listOf(
                                        lagBeregningsperiodePrivatBil(mandagFørsteUke, 7 september 2025, 100),
                                        lagBeregningsperiodePrivatBil(mandagAndreUke, 14 september 2025, 200),
                                        lagBeregningsperiodePrivatBil(mandagTredjeUke, 21 september 2025, 300),
                                    ),
                            ),
                        ),
                )

            val andeler =
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    rammevedtakPrivatBil = lagRammevedtakPrivatBil(listOf(reiseId), 1 september 2025, 30 september 2025),
                )

            assertThat(andeler).hasSize(3)
            with(andeler[0]) {
                assertThat(beløp).isEqualTo(100)
                assertThat(fom).isEqualTo(mandagFørsteUke)
                assertThat(tom).isEqualTo(mandagFørsteUke)
            }
            with(andeler[1]) {
                assertThat(beløp).isEqualTo(200)
                assertThat(fom).isEqualTo(mandagAndreUke)
                assertThat(tom).isEqualTo(mandagAndreUke)
            }
            with(andeler[2]) {
                assertThat(beløp).isEqualTo(300)
                assertThat(fom).isEqualTo(mandagTredjeUke)
                assertThat(tom).isEqualTo(mandagTredjeUke)
            }
        }

        @Test
        fun `to reiser med med samme periode i beregningsresultat gir to andeler`() {
            val reiseId1 = ReiseId.random()
            val reiseId2 = ReiseId.random()
            val fomUke = 1 september 2025 // mandag
            val tomUke = 7 september 2025 // søndag
            val beregningsresultat =
                BeregningsresultatPrivatBil(
                    reiser =
                        listOf(
                            BeregningsresultatForReisePrivatBil(
                                reiseId = reiseId1,
                                perioder =
                                    listOf(
                                        lagBeregningsperiodePrivatBil(fomUke, tomUke, 100),
                                    ),
                            ),
                            BeregningsresultatForReisePrivatBil(
                                reiseId = reiseId2,
                                perioder =
                                    listOf(
                                        lagBeregningsperiodePrivatBil(fomUke, tomUke, 200),
                                    ),
                            ),
                        ),
                )

            val andeler =
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    rammevedtakPrivatBil = lagRammevedtakPrivatBil(listOf(reiseId1, reiseId2), fomUke, 30 september 2025),
                )

            // TODO: bør andeler grupperes på hvilken reise de tilhører?
            assertThat(andeler).hasSize(2)
            with(andeler[0]) {
                assertThat(beløp).isEqualTo(100)
                assertThat(fom).isEqualTo(fomUke)
                assertThat(tom).isEqualTo(fomUke)
            }
            with(andeler[1]) {
                assertThat(beløp).isEqualTo(200)
                assertThat(fom).isEqualTo(fomUke)
                assertThat(tom).isEqualTo(fomUke)
            }
        }

        @Test
        fun `en reiser med periode som starter midt i uka returnerer andel med dato forrige mandag`() {
            val reiseId1 = ReiseId.random()
            val fomUke = 3 september 2025 // mandag
            val tomUke = 7 september 2025 // søndag
            val beregningsresultat =
                BeregningsresultatPrivatBil(
                    reiser =
                        listOf(
                            BeregningsresultatForReisePrivatBil(
                                reiseId = reiseId1,
                                perioder =
                                    listOf(
                                        lagBeregningsperiodePrivatBil(fomUke, tomUke, 100),
                                    ),
                            ),
                        ),
                )

            val andeler =
                beregningsresultat.mapTilAndelTilkjentYtelse(
                    saksbehandling = saksbehandling,
                    rammevedtakPrivatBil = lagRammevedtakPrivatBil(listOf(reiseId1), fomUke, 30 september 2025),
                )

            assertThat(andeler).hasSize(1)
            with(andeler[0]) {
                assertThat(beløp).isEqualTo(100)
                assertThat(fom).isEqualTo(1 september 2025)
            }
        }

        private fun lagBeregningsperiodePrivatBil(
            fom: LocalDate,
            tom: LocalDate,
            stønadsbeløp: Int,
        ) = BeregningsresultatForReisePrivatBilPeriode(
            fom = fom,
            tom = tom,
            grunnlag =
                BeregningsresultatForReisePrivatBilGrunnlag(
                    dager =
                        listOf(
                            BeregningsresultatForReisePrivatBilDag(
                                dato = fom,
                                parkeringskostnad = 0,
                                stønadsbeløpForDag = stønadsbeløp.toBigDecimal(),
                                dagsatsUtenParkering = 100.toBigDecimal(),
                            ),
                        ),
                ),
            stønadsbeløp = stønadsbeløp.toBigDecimal(),
            brukersNavKontor = null,
            fraTidligereVedtak = false,
        )

        private fun lagRammevedtakPrivatBil(
            reiseIder: List<ReiseId>,
            fom: LocalDate,
            tom: LocalDate,
        ) = RammevedtakPrivatBil(
            reiser =
                reiseIder.map { reiseId ->
                    rammeForReiseMedPrivatBil(
                        reiseId = reiseId,
                        fom = fom,
                        tom = tom,
                        vedtaksperioder = listOf(vedtaksperiode(fom = fom, tom = tom)),
                    )
                },
        )
    }
}
