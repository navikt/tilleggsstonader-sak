package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InnvilgeDagligReiseIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `Skal ikke kunne innvilge daglig reise for både Nay og Tiltaksenheten samtidig`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fomTiltaksenheten = 1 september 2025
        val tomTiltaksenheten = 30 september 2025

        val fomNay = 15 september 2025
        val tomNay = 14 oktober 2025

        // Gjennomfører behandling for Tiltaksenheten
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
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.BEREGNE_YTELSE,
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

        gjennomførBeregningSteg(behandlingId, Stønadstype.DAGLIG_REISE_TSO)
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

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        val rammevedtak = kall.privatBil.hentRammevedtak("12345678910")

        assertThat(rammevedtak).isNotEmpty()
    }

    @Test
    fun `Skal kaste feil hvis prøver å innvilge uten aktivitet med relevant TypeAktivitet hele vedtakserioden`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fom = 1 januar 2025
        val tom = 28 februar 2025

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsr(fom, 31 januar 2025, typeAktivitet = TypeAktivitet.GRUPPEAMO)
                    }
                    opprett {
                        aktivitetTiltakTsr(1 februar 2025, tom, typeAktivitet = TypeAktivitet.ARBTREN)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeTiltakspenger(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransport(fom, tom)
                    }
                }
            }

        gjennomførBeregningSteg(behandlingId, Stønadstype.DAGLIG_REISE_TSO)
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo(
                "Finner ingen oppfylte vilkår med GRUPPEAMO for hele perioden 01.01.2025–28.02.2025",
            )
    }
}
