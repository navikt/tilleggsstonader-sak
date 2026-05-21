package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningStegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InnvilgeDagligReiseIntegrationTest : IntegrationTest() {
    @Test
    fun `Skal ikke kunne innvilge daglig reise for både Nay og Tiltaksenheten samtidig`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fomTiltaksenheten = 1 september 2025
        val tomTiltaksenheten = 30 september 2025

        val fomNay = 15 september 2025
        val tomNay = 14 oktober 2025

        // Gjennomfører behandling for Tiltaksenheten
        val behandlingContextTiltaksenheten =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsr(fomTiltaksenheten, tomTiltaksenheten, typeAktivitet = TypeAktivitet.GRUPPEAMO)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeTiltakspenger(fomTiltaksenheten, tomTiltaksenheten)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fomTiltaksenheten, tomTiltaksenheten)
                    }
                }
            }

        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()

        // Gjennomfører behandling for Nay
        val behandlingContextNay =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.BEREGNE_YTELSE,
                ident = behandlingContextTiltaksenheten.ident,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fomNay, tomNay)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fomNay, tomNay)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fomNay, tomNay)
                    }
                }
            }

        gjennomførBeregningStegKall(behandlingContextNay.behandlingId, Stønadstype.DAGLIG_REISE_TSO)
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo(
                "Kan ikke ha overlappende vedtaksperioder for Nay og Tiltaksenheten. Se oversikt øverst på siden for å finne overlappende vedtaksperiode.",
            )
    }

    @Test
    fun `innvilge rammevedtak privat bil og henter ut rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        val fom = 15 september 2025
        val tom = 14 oktober 2025

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)

        assertThat(rammevedtak).isNotEmpty()
    }
}
