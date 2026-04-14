package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdRepository
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingDagligReiseOffentligTransportIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository

    private val nå = LocalDate.now()
    private val fom = nå.minusMonths(3)
    private val tom = nå.plusMonths(3)

    @Test
    fun `to andeler forrige måned, sender da én utbetaling med to perioder`() {
        val forrigeMåned = YearMonth.now().minusMonths(1)

        val reiseperiode1 = Datoperiode(forrigeMåned.atDay(1), tom = forrigeMåned.atDay(7))
        val reiseperiode2 = Datoperiode(forrigeMåned.atDay(10), tom = forrigeMåned.atDay(17))

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(reiseperiode1.fom, reiseperiode1.tom)
                        offentligTransport(reiseperiode2.fom, reiseperiode2.tom)
                    }
                }
            }

        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingContext.behandlingId)
        val utbetalinger = KafkaFake.sendteMeldinger().finnPåTopic(kafkaTopics.utbetaling)
        val utbetaling = utbetalinger.single().verdiEllerFeil<IverksettingDto>()

        assertThat(utbetaling.periodetype).isEqualTo(PeriodetypeUtbetaling.UKEDAG)
        assertThat(utbetaling.behandlingId).isEqualTo(saksbehandling.eksternId.toString())
        assertThat(utbetaling.utbetalinger).hasSize(1)
        with(utbetaling.utbetalinger.single()) {
            assertThat(perioder).hasSize(2)

            with(perioder.first()) {
                assertThat(fom).isEqualTo(tom).isEqualTo(reiseperiode1.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
            }

            with(perioder.last()) {
                assertThat(fom).isEqualTo(tom).isEqualTo(reiseperiode2.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
            }
        }

        assertUtbetalingIderErUtenReiseId(behandlingContext.fagsakId)
    }

    @Test
    fun `hvis vi har én andel nå og én andel fram i tid, skal vi bare iverksette den første andelen`() {
        val reiseperiode1 = Datoperiode(nå.minusDays(5), tom = nå)
        val reiseperiode2 = Datoperiode(nå.plusWeeks(1), tom = nå.plusWeeks(2))

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(reiseperiode1.fom, reiseperiode1.tom)
                        offentligTransport(reiseperiode2.fom, reiseperiode2.tom)
                    }
                }
            }

        val utbetaling =
            KafkaFake
                .sendteMeldinger()
                .finnPåTopic(kafkaTopics.utbetaling)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        val forventetUtbetalingsdato =
            reiseperiode1
                .fom
                .datoEllerNesteMandagHvisLørdagEllerSøndag()

        with(
            utbetaling.utbetalinger
                .single()
                .perioder
                .single(),
        ) {
            assertThat(fom).isEqualTo(tom).isEqualTo(forventetUtbetalingsdato)
        }

        assertUtbetalingIderErUtenReiseId(behandlingContext.fagsakId)
    }

    @Test
    fun `to andeler tilbake i tid med forskjellige type, skal bli to utbetalinger`() {
        val reiseperiode1 = Datoperiode(1 august 2025, 31 august 2025)
        val reiseperiode2 = Datoperiode(1 oktober 2025, 20 oktober 2025)

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(reiseperiode1.fom, reiseperiode1.tom)
                        aktivitetUtdanningDagligReiseTso(reiseperiode2.fom, reiseperiode2.tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(reiseperiode1.fom, reiseperiode1.tom)
                        målgruppeOvergangsstønad(reiseperiode2.fom, reiseperiode2.tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(reiseperiode1.fom, reiseperiode1.tom)
                        offentligTransport(reiseperiode2.fom, reiseperiode2.tom)
                    }
                }
            }

        val utbetaling =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                // Første iverksettingId skal være behandlingId
                .also { assertThat(it.key()).isEqualTo(behandlingContext.behandlingId.toString()) }
                .verdiEllerFeil<IverksettingDto>()

        assertThat(utbetaling.utbetalinger.map { it.stønad }).containsExactlyInAnyOrder(
            StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER,
            StønadUtbetaling.DAGLIG_REISE_AAP,
        )

        with(utbetaling.utbetalinger.single { it.stønad == StønadUtbetaling.DAGLIG_REISE_AAP }) {
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().fom)
                .isEqualTo(perioder.single().fom)
                .isEqualTo(reiseperiode1.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
        }

        with(utbetaling.utbetalinger.single { it.stønad == StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER }) {
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().fom)
                .isEqualTo(perioder.single().fom)
                .isEqualTo(reiseperiode2.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
        }

        assertUtbetalingIderErUtenReiseId(behandlingContext.fagsakId)
    }

    private fun assertUtbetalingIderErUtenReiseId(fagsakId: FagsakId) {
        fagsakUtbetalingIdRepository
            .findByFagsakId(fagsakId)
            .forEach {
                assertThat(it.reiseId).isNull()
            }
    }
}
