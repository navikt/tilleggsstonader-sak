package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.StønadUtbetaling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InnvilgeDagligReiseNyeMålgrupperIntegrationTest : IntegrationTest() {
    private val fom = 1 september 2025
    private val tom = 30 september 2025

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `innvilger daglig reise for ungdomsprogrammet og sender tiltaksøkonomisk utbetaling på Kafka`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_UNGDOMSPROGRAMMET) } returns true

        val behandling =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSR) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsr(fom, tom, TypeAktivitet.GRUPPEAMO)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeUngdomsprogrammet(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fom, tom)
                    }
                }
            }

        val utbetaling = hentUtbetaling()
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.behandlingId)!!.andelerTilkjentYtelse

        assertThat(andeler).isNotEmpty.allMatch { it.type == TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO }
        assertThat(utbetaling.utbetalinger.map { it.stønad })
            .containsOnly(StønadUtbetaling.DAGLIG_REISE_TILTAK_GRUPPE_AMO)
    }

    @Test
    fun `innvilger daglig reise for aktivitetspenger og sender utbetaling til AAP-konto på Kafka`() {
        every { unleashService.isEnabled(Toggle.KAN_BRUKE_MÅLGRUPPE_AKTIVITETSPENGER) } returns true

        val behandling =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAktivitetspenger(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fom, tom)
                    }
                }
            }

        val utbetaling = hentUtbetaling()
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandling.behandlingId)!!.andelerTilkjentYtelse

        assertThat(andeler).isNotEmpty.allMatch { it.type == TypeAndel.DAGLIG_REISE_AKTIVITETSPENGER }
        assertThat(utbetaling.utbetalinger.map { it.stønad }).containsOnly(StønadUtbetaling.DAGLIG_REISE_AKTIVITETSPENGER)
    }

    private fun hentUtbetaling(): IverksettingDto =
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
            .single()
            .verdiEllerFeil()
}
