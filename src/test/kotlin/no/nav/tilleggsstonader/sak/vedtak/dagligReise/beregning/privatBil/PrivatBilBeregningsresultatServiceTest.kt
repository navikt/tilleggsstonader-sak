package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.satsForPeriodePrivatBil
import no.nav.tilleggsstonader.sak.util.finnMandagNesteUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatException
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class PrivatBilBeregningsresultatServiceTest {
    private val behandlingId = BehandlingId.random()
    private val reiseId = ReiseId.random()
    private val brukersNavKontor = "6767"

    val beregningService = PrivatBilBeregningsresultatService()

    @Test
    fun `dager med parkeringsutgifter blir beregnet riktig`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 8 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser =
                    listOf(
                        satsForPeriodePrivatBil(
                            fomRammevedtak,
                            tomRammevedtak,
                            2.94.toBigDecimal(),
                            dagsatsUtenParkering,
                        ),
                    ),
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak,
                        tom = fomRammevedtak.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)),
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(2 februar 2026, 50),
                        KjørelisteUtil.KjørtDag(3 februar 2026, 120),
                        KjørelisteUtil.KjørtDag(4 februar 2026, 3),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste)

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(1)

        val beregningsresultatForReise = beregningsresultat.reiser.single()
        assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
        assertThat(beregningsresultatForReise.perioder).hasSize(1)

        val beregningsresultatUke = beregningsresultatForReise.perioder.single()
        assertThat(beregningsresultatUke.fom).isEqualTo(kjøreliste.data.fom)
        assertThat(beregningsresultatUke.tom).isEqualTo(kjøreliste.data.tom)
        assertThat(beregningsresultatUke.utbetalingsdato).isEqualTo(9 februar 2026) // Første mandag etter uke
        assertThat(beregningsresultatUke.brukersNavKontor).isEqualTo(brukersNavKontor)

        val reisedager = kjøreliste.data.reisedager.filter { it.harKjørt }
        val totaleParkeringsutgifter = reisedager.mapNotNull { it.parkeringsutgift }.sum().toBigDecimal()
        assertThat(beregningsresultatUke.stønadsbeløp).isEqualTo(dagsatsUtenParkering * 3.toBigDecimal() + totaleParkeringsutgifter)

        assertThat(beregningsresultatUke.grunnlag.dager).hasSize(reisedager.size)
        beregningsresultatUke.grunnlag.dager.forEach { reisedag ->
            val reisedagFraKjøreliste = reisedager.single { it.dato == reisedag.dato }
            assertThat(reisedagFraKjøreliste.parkeringsutgift).isEqualTo(reisedag.parkeringskostnad)
        }
    }

    @Test
    fun `sendt inn kjøreliste for 1 av 2 uker med to kjørte dager, beregnes for kun en uke`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 15 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser =
                    listOf(
                        satsForPeriodePrivatBil(
                            fomRammevedtak,
                            tomRammevedtak,
                            2.94.toBigDecimal(),
                            dagsatsUtenParkering,
                        ),
                    ),
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak,
                        tom = fomRammevedtak.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)),
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(2 februar 2026),
                        KjørelisteUtil.KjørtDag(3 februar 2026),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste)

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(1)

        val beregningsresultatForReise = beregningsresultat.reiser.single()
        assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
        assertThat(beregningsresultatForReise.perioder).hasSize(1)

        val beregningsresultatUke = beregningsresultatForReise.perioder.single()
        assertThat(beregningsresultatUke.fom).isEqualTo(kjøreliste.data.fom)
        assertThat(beregningsresultatUke.tom).isEqualTo(kjøreliste.data.tom)
        assertThat(beregningsresultatUke.utbetalingsdato).isEqualTo(9 februar 2026) // Første mandag etter uke
        assertThat(beregningsresultatUke.stønadsbeløp).isEqualTo(dagsatsUtenParkering * 2.toBigDecimal())

        assertThat(beregningsresultatUke.grunnlag.dager).hasSize(2).allMatch { it.parkeringskostnad == 0 }
    }

    @Test
    fun `sendt inn kjøreliste for en uke som går over to år, blir to beregnede perioder med samme utbetalingsdato`() {
        val fomRammevedtak = 29 desember 2025 // Mandag
        val tomRammevedtak = 4 januar 2026
        val dagsats1UtenParkering = 150.toBigDecimal()
        val dagsats2UtenParkering = 175.toBigDecimal()
        val forventetUtbetalingsdato = 5 januar 2026

        val satser =
            listOf(
                satsForPeriodePrivatBil(
                    fomRammevedtak,
                    31 desember 2025,
                    2.94.toBigDecimal(),
                    dagsats1UtenParkering,
                ),
                satsForPeriodePrivatBil(
                    1 januar 2026,
                    tomRammevedtak,
                    2.94.toBigDecimal(),
                    dagsats2UtenParkering,
                ),
            )

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser = satser,
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak,
                        tom = fomRammevedtak.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)),
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(30 desember 2025),
                        KjørelisteUtil.KjørtDag(2 januar 2026),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste)

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(1)

        val beregningsresultatForReise = beregningsresultat.reiser.single()
        assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
        assertThat(beregningsresultatForReise.perioder).hasSize(2)

        val beregningsresultatUke1 = beregningsresultatForReise.perioder[0]
        val sats1 = satser[0]
        assertThat(beregningsresultatUke1.fom).isEqualTo(sats1.fom)
        assertThat(beregningsresultatUke1.tom).isEqualTo(sats1.tom)
        assertThat(beregningsresultatUke1.utbetalingsdato).isEqualTo(forventetUtbetalingsdato) // Første mandag etter uke
        assertThat(beregningsresultatUke1.stønadsbeløp).isEqualTo(dagsats1UtenParkering)
        assertThat(beregningsresultatUke1.grunnlag.dager).hasSize(1)

        val beregningsresultatUke2 = beregningsresultatForReise.perioder[1]
        val sats2 = satser[1]
        assertThat(beregningsresultatUke2.fom).isEqualTo(sats2.fom)
        assertThat(beregningsresultatUke2.tom).isEqualTo(sats2.tom)
        assertThat(beregningsresultatUke2.utbetalingsdato).isEqualTo(forventetUtbetalingsdato) // Første mandag etter uke
        assertThat(beregningsresultatUke2.stønadsbeløp).isEqualTo(dagsats2UtenParkering)
        assertThat(beregningsresultatUke2.grunnlag.dager).hasSize(1).allMatch { it.parkeringskostnad == 0 }
    }

    @Test
    fun `sender inn rammevedtak for to uker med en ukes mellomrom, blir kun opprettet perioder for uker som er innsendt`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 22 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser =
                    listOf(
                        satsForPeriodePrivatBil(
                            fomRammevedtak,
                            tomRammevedtak,
                            2.94.toBigDecimal(),
                            dagsatsUtenParkering,
                        ),
                    ),
            )

        val kjøreliste1 =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak,
                        tom = fomRammevedtak.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)),
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(fomRammevedtak),
                    ),
            )

        val kjøreliste2 =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = tomRammevedtak.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)),
                        tom = tomRammevedtak,
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(tomRammevedtak),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste1) + avklarUkerFraKjøreliste(kjøreliste2)

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(1)

        val beregningsresultatForReise = beregningsresultat.reiser.single()
        assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
        assertThat(beregningsresultatForReise.perioder).hasSize(2)

        val beregningsresultatUke1 = beregningsresultatForReise.perioder[0]
        assertThat(beregningsresultatUke1.fom).isEqualTo(kjøreliste1.data.fom)
        assertThat(beregningsresultatUke1.tom).isEqualTo(kjøreliste1.data.tom)
        assertThat(beregningsresultatUke1.utbetalingsdato).isEqualTo(kjøreliste1.data.tom.finnMandagNesteUke()) // Første mandag etter uke
        assertThat(beregningsresultatUke1.stønadsbeløp).isEqualTo(dagsatsUtenParkering)
        assertThat(beregningsresultatUke1.grunnlag.dager).hasSize(1).allMatch { it.parkeringskostnad == 0 }

        val beregningsresultatUke2 = beregningsresultatForReise.perioder[1]
        assertThat(beregningsresultatUke2.fom).isEqualTo(kjøreliste2.data.fom)
        assertThat(beregningsresultatUke2.tom).isEqualTo(kjøreliste2.data.tom)
        assertThat(beregningsresultatUke2.utbetalingsdato).isEqualTo(kjøreliste2.data.tom.finnMandagNesteUke()) // Første mandag etter uke
        assertThat(beregningsresultatUke2.stønadsbeløp).isEqualTo(dagsatsUtenParkering)
        assertThat(beregningsresultatUke2.grunnlag.dager).hasSize(1).allMatch { it.parkeringskostnad == 0 }
    }

    @Test
    fun `forventer tomt rammevedtak når det ikke finnes noen avklarte uker`() {
        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = 2 februar 2026,
                tom = 22 februar 2026,
            )

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, emptyList(), brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(1)

        val beregningsresultatForReise = beregningsresultat.reiser.single()
        assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
        assertThat(beregningsresultatForReise.perioder).isEmpty()
    }

    @Test
    fun `håndterer kjørelister sendt inn for samme perioder for forskjellige rammevedtak`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 8 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val reiseId1 = ReiseId.random()
        val reiseId2 = ReiseId.random()
        val reiseIder = listOf(reiseId1, reiseId2)

        val rammevedtakPrivatBil =
            RammevedtakPrivatBil(
                reiser =
                    reiseIder.map {
                        rammeForReiseMedPrivatBil(
                            reiseId = it,
                            fom = fomRammevedtak,
                            tom = tomRammevedtak,
                            satser =
                                listOf(
                                    satsForPeriodePrivatBil(
                                        fomRammevedtak,
                                        tomRammevedtak,
                                        2.94.toBigDecimal(),
                                        dagsatsUtenParkering,
                                    ),
                                ),
                        )
                    },
            )

        val kjørelister =
            reiseIder.map {
                KjørelisteUtil.kjøreliste(
                    reiseId = it,
                    periode =
                        Datoperiode(
                            fom = fomRammevedtak,
                            tom = tomRammevedtak,
                        ),
                    // Mandag til søndag
                    kjørteDager =
                        listOf(
                            KjørelisteUtil.KjørtDag(fomRammevedtak),
                        ),
                )
            }

        val avklarteUker = kjørelister.flatMap { avklarUkerFraKjøreliste(it) }

        val beregningsresultat = beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)

        assertThat(beregningsresultat).isNotNull
        assertThat(beregningsresultat!!.reiser).hasSize(2)

        reiseIder.forEach { reiseId ->
            val beregningsresultatForReise = beregningsresultat.reiser.single { it.reiseId == reiseId }
            assertThat(beregningsresultatForReise.reiseId).isEqualTo(reiseId)
            assertThat(beregningsresultatForReise.perioder).hasSize(1)

            val beregningsresultatUke1 = beregningsresultatForReise.perioder.single()
            assertThat(beregningsresultatUke1.fom).isEqualTo(fomRammevedtak)
            assertThat(beregningsresultatUke1.tom).isEqualTo(tomRammevedtak)
            assertThat(beregningsresultatUke1.utbetalingsdato).isEqualTo(tomRammevedtak.finnMandagNesteUke()) // Første mandag etter uke
            assertThat(beregningsresultatUke1.stønadsbeløp).isEqualTo(dagsatsUtenParkering)
            assertThat(beregningsresultatUke1.grunnlag.dager).hasSize(1).allMatch { it.parkeringskostnad == 0 }
        }
    }

    @Test
    fun `kaster feil om det finnes avklarte dager med godkjentGjennomførtKjøring=true utenfor rammevedtaket`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 8 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser =
                    listOf(
                        satsForPeriodePrivatBil(
                            fomRammevedtak,
                            tomRammevedtak,
                            2.94.toBigDecimal(),
                            dagsatsUtenParkering,
                        ),
                    ),
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak.plusDays(7),
                        tom = tomRammevedtak.plusDays(7),
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(fomRammevedtak.plusDays(7)),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste)

        assertThatException()
            .isThrownBy {
                beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)
            }.withMessageContaining("Dag ${kjøreliste.data.reisedager.single { it.harKjørt }.dato} er ikke innenfor rammevedtak")
    }

    @Test
    fun `kaster ikke feil om det finnes avklarte dager med godkjentGjennomførtKjøring=false utenfor rammevedtaket`() {
        val fomRammevedtak = 2 februar 2026 // Mandag
        val tomRammevedtak = 8 februar 2026
        val dagsatsUtenParkering = 150.toBigDecimal()

        val rammevedtakPrivatBil =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomRammevedtak,
                tom = tomRammevedtak,
                satser =
                    listOf(
                        satsForPeriodePrivatBil(
                            fomRammevedtak,
                            tomRammevedtak,
                            2.94.toBigDecimal(),
                            dagsatsUtenParkering,
                        ),
                    ),
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                reiseId = reiseId,
                periode =
                    Datoperiode(
                        fom = fomRammevedtak,
                        tom = tomRammevedtak.plusDays(1), // Sendt inn en dag utenfor rammevedtak, men skal ikke utbetales
                    ),
                // Mandag til søndag
                kjørteDager =
                    listOf(
                        KjørelisteUtil.KjørtDag(fomRammevedtak),
                    ),
            )
        val avklarteUker = avklarUkerFraKjøreliste(kjøreliste)

        assertThatNoException().isThrownBy {
            beregningService.beregn(rammevedtakPrivatBil, avklarteUker, brukersNavKontor)
        }
    }

    private fun avklarUkerFraKjøreliste(kjøreliste: Kjøreliste): List<AvklartKjørtUke> =
        kjøreliste.data.reisedager
            .groupBy { it.dato.tilUkeIÅr() }
            .map { (uke, dager) ->
                AvklartKjørtUke(
                    behandlingId = behandlingId,
                    kjørelisteId = kjøreliste.id,
                    reiseId = kjøreliste.data.reiseId,
                    fom = dager.minOf { it.dato },
                    tom = dager.maxOf { it.dato },
                    ukenummer = uke.ukenummer,
                    status = UkeStatus.OK_AUTOMATISK,
                    behandletDato = LocalDate.now(),
                    dager =
                        dager
                            .map {
                                AvklartKjørtDag(
                                    dato = it.dato,
                                    godkjentGjennomførtKjøring =
                                        if (it.harKjørt) {
                                            GodkjentGjennomførtKjøring.JA
                                        } else {
                                            GodkjentGjennomførtKjøring.NEI
                                        },
                                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                                    avvik = emptyList(),
                                    parkeringsutgift = it.parkeringsutgift,
                                )
                            }.toSet(),
                )
            }
}
