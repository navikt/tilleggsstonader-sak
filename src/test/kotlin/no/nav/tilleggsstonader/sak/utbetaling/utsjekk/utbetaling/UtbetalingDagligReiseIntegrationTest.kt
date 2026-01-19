package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingDagligReiseIntegrationTest : CleanDatabaseIntegrationTest() {
    private val nå = LocalDate.now()
    private val fom = nå.minusMonths(3)
    private val tom = nå.plusMonths(3)

    @Test
    fun `utbetalingsdato i fremtiden - ingen andeler skal bli utbetalt`() {
        opprettBehandlingOgGjennomførBehandlingsløp {
            aktivitet {
                opprett {
                    aktivitetTiltak(fom = fom, tom = tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom = fom, tom = tom)
                }
            }
            vilkår {
                opprett {
                    offentligTransport(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusWeeks(1))
                }
            }
        }

        val sendtMelding =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .single()

        assertThat(
            sendtMelding.utbetalinger.size,
        ).isEqualTo(0)
    }

    @Test
    fun `to andeler forrige måned, sender da én utbetaling med to perioder`() {
        val forrigeMåned = YearMonth.now().minusMonths(1)

        val reiser =
            listOf(
                lagreDagligReiseDto(fom = forrigeMåned.atDay(1), tom = forrigeMåned.atDay(7)),
                lagreDagligReiseDto(fom = forrigeMåned.atDay(10), tom = forrigeMåned.atDay(17)),
            )

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp {
                aktivitet {
                    opprett {
                        aktivitetTiltak(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
                vilkår {
                    opprett {
                        add(reiser)
                    }
                }
            }

        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingId)
        val utbetalinger = KafkaTestConfig.sendteMeldinger().finnPåTopic(kafkaTopics.utbetaling)
        val utbetaling = utbetalinger.single().verdiEllerFeil<IverksettingDto>()

        assertThat(utbetaling.periodetype).isEqualTo(PeriodetypeUtbetaling.UKEDAG)
        assertThat(utbetaling.behandlingId).isEqualTo(saksbehandling.eksternId.toString())
        assertThat(utbetaling.utbetalinger).hasSize(1)
        with(utbetaling.utbetalinger.single()) {
            assertThat(perioder).hasSize(2)

            with(perioder.first()) {
                assertThat(fom).isEqualTo(tom).isEqualTo(reiser.first().fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
            }

            with(perioder.last()) {
                assertThat(fom).isEqualTo(tom).isEqualTo(reiser.last().fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
            }
        }
    }

    @Test
    fun `hvis vi har én andel nå og én andel fram i tid, skal vi bare iverksette den første andelen`() {
        val reiser =
            listOf(
                lagreDagligReiseDto(fom = nå.minusDays(5), tom = nå),
                lagreDagligReiseDto(fom = nå.plusWeeks(1), tom = nå.plusWeeks(2)),
            )

        opprettBehandlingOgGjennomførBehandlingsløp {
            aktivitet {
                opprett {
                    aktivitetTiltak(fom = fom, tom = tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom = fom, tom = tom)
                }
            }
            vilkår {
                opprett {
                    add(reiser)
                }
            }
        }

        val utbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .finnPåTopic(kafkaTopics.utbetaling)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        val forventetUtbetalingsdato =
            reiser
                .first()
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
    }

    @Test
    fun `to andeler tilbake i tid med forskjellige type, skal bli to utbetalinger`() {
        val førstePeriode = Datoperiode(1 august 2025, 31 august 2025)
        val andrePeriode = Datoperiode(1 oktober 2025, 20 oktober 2025)

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp {
                aktivitet {
                    opprett {
                        aktivitetTiltak(førstePeriode.fom, førstePeriode.tom)
                        add { behandlingId ->
                            lagreVilkårperiodeAktivitet(
                                behandlingId = behandlingId,
                                aktivitetType = AktivitetType.UTDANNING,
                                typeAktivitet = null,
                                fom = andrePeriode.fom,
                                tom = andrePeriode.tom,
                                faktaOgSvar =
                                    FaktaOgSvarAktivitetDagligReiseTsoDto(
                                        svarLønnet = SvarJaNei.JA,
                                        svarHarUtgifter = SvarJaNei.JA,
                                    ),
                            )
                        }
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(førstePeriode.fom, førstePeriode.tom)
                        målgruppeOvergangsstønad(andrePeriode.fom, andrePeriode.tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(førstePeriode.fom, førstePeriode.tom)
                        offentligTransport(andrePeriode.fom, andrePeriode.tom)
                    }
                }
            }

        val utbetaling =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .also { assertThat(it.key()).isEqualTo(behandlingId.toString()) } // Første iverksettingId skal være behandlingId
                .verdiEllerFeil<IverksettingDto>()

        assertThat(utbetaling.utbetalinger.map { it.stønad }).containsExactlyInAnyOrder(
            StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER,
            StønadUtbetaling.DAGLIG_REISE_AAP,
        )

        with(utbetaling.utbetalinger.single { it.stønad == StønadUtbetaling.DAGLIG_REISE_AAP }) {
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().fom)
                .isEqualTo(perioder.single().fom)
                .isEqualTo(førstePeriode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
        }

        with(utbetaling.utbetalinger.single { it.stønad == StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER }) {
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().fom)
                .isEqualTo(perioder.single().fom)
                .isEqualTo(andrePeriode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag())
        }
    }
}
