package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBeregningSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnvilgeDagligReiseIntegrationTest : CleanDatabaseIntegrationTest() {
    val fomTiltaksenheten = 1 september 2025
    val tomTiltaksenheten = 30 september 2025

    val fomNay = 15 september 2025
    val tomNay = 14 oktober 2025

    @Test
    fun `Skal ikke kunne innvilge daglig reise for både Nay og Tiltaksenheten samtidig`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

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
    fun `innvilge rammevedtak privat bil`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fomNay, tomNay)
        }
    }
}
