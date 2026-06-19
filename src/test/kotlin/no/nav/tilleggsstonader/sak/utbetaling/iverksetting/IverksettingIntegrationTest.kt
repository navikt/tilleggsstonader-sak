package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class IverksettingIntegrationTest(
    @Autowired private val fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository,
) : IntegrationTest() {
    @Test
    fun `skal ikke sende noe på kafka hvis vi bare har andeler fram i tid`() {
        val omTrettiDager = LocalDate.now().plusDays(30)
        val omSekstiDager = LocalDate.now().plusDays(60)
        val omNittiDager = LocalDate.now().plusDays(90)

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(fom = omTrettiDager, tom = omSekstiDager)
            }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingContext.behandlingId) {
            defaultDagligReiseTsoTestdata(fom = omSekstiDager.plusDays(1), tom = omNittiDager)
        }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }

    @Test
    fun `skal ikke sende noe til økonomi ved innvilgelse frem i tid og opphør av dette`() {
        val omToUker = LocalDate.now().plusWeeks(2)
        val omFireUker = LocalDate.now().plusWeeks(4)

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReiseTsoTestdata(fom = omToUker, tom = omFireUker)
            }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingContext.behandlingId) {
            vedtak {
                opphør(opphørsdato = omToUker)
            }
        }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)
    }

    @Test
    fun `skal ikke sende noe på kafka for utbetalingId'er som ikke har noen andeler sendt på seg ved opphør`() {
        val toUkerSiden = LocalDate.now().minusWeeks(2)
        val omToUker = LocalDate.now().plusWeeks(2)
        val omFireUker = LocalDate.now().plusWeeks(4)

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                aktivitet {
                    opprett {
                        aktivitetUtdanningDagligReiseTso(toUkerSiden, omFireUker)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(toUkerSiden, omToUker)
                        målgruppeOvergangsstønad(omToUker.plusDays(1), omFireUker)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fom = toUkerSiden, tom = omToUker)
                        offentligTransport(fom = omToUker.plusDays(1), tom = omFireUker)
                    }
                }
            }

        val utbetalingIderPåFagsak = fagsakUtbetalingIdRepository.findByFagsakId(førstegangsbehandlingContext.fagsakId)
        assertThat(utbetalingIderPåFagsak).hasSize(2)
        assertThat(utbetalingIderPåFagsak.map { it.typeAndel }).containsExactlyInAnyOrder(
            TypeAndel.DAGLIG_REISE_AAP,
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
        )

        val iverksettingFørstegangsbehandling =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .single()
                .verdiEllerFeil<IverksettingDto>()

        assertThat(iverksettingFørstegangsbehandling.utbetalinger).hasSize(1)
        assertThat(iverksettingFørstegangsbehandling.utbetalinger.single().id).isEqualTo(
            utbetalingIderPåFagsak
                .single {
                    it.typeAndel ==
                        TypeAndel.DAGLIG_REISE_AAP
                }.utbetalingId,
        )
        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        opprettRevurderingOgGjennomførBehandlingsløp(førstegangsbehandlingContext.behandlingId) {
            vedtak {
                opphør(opphørsdato = toUkerSiden)
            }
        }

        val iverksettingOpphør =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                .map { it.verdiEllerFeil<IverksettingDto>() }
                .maxBy { it.vedtakstidspunkt }

        assertThat(iverksettingOpphør.utbetalinger).hasSize(1)
        assertThat(iverksettingOpphør.utbetalinger.single().id).isEqualTo(
            utbetalingIderPåFagsak
                .single {
                    it.typeAndel ==
                        TypeAndel.DAGLIG_REISE_AAP
                }.utbetalingId,
        )
    }
}
