package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.StønadUtbetaling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tester innvilgelse av reise til samling for tema **TSR**.
 * Se [InnvilgelseReiseTilSamlingTsoIntegrationTest] for tilsvarende test for tema TSO.
 */
class InnvilgelseReiseTilSamlingTsrIntegrationTest : IntegrationTest() {
    @Test
    fun `kan innvilge reise til samling offentlig transport - TSR`() {
        val fomNay = 1 januar 2025
        val tomNay = 31 januar 2025

        // For at det skal opprettes sak med stønadstype REISE_TIL_SAMLING_TSR
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val behandlingContextNay =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR,
                tilSteg = StegType.SIMULERING,
            ) {
                defaultReiseTilSamlingTSRTestdata(
                    fom = fomNay,
                    tom = tomNay,
                )
            }
        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.REISE_TIL_SAMLING_TSR, behandlingContextNay.behandlingId)
                .expectOkWithBody<InnvilgelseReiseTilSamlingResponse>()

        assertThat(vedtak.gjelderFraOgMed).isEqualTo(fomNay)
        assertThat(vedtak.gjelderTilOgMed).isEqualTo(tomNay)
    }

    @Test
    fun `vedtaksperioder for TSR sender kun fom og tom - typeandel bestemmes av tiltaksvariant uavhengig av målgruppe og aktivitet`() {
        val fom = 1 januar 2025
        val tom = 31 januar 2025

        // For at det skal opprettes sak med stønadstype REISE_TIL_SAMLING_TSR
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTsrReiseTilSamling(fom, tom, tiltaksvariant = TypeAktivitet.GRUPPEAMO)
                }
            }
            målgruppe {
                opprett {
                    målgruppeTiltakspenger(fom, tom)
                }
            }
            vilkår {
                opprett {
                    offentligTransportReiseTilSamling(fom, tom, hentAktivitet = { it.single() })
                }
            }
        }

        validerTypeAndelFraTiltaksvariant()
    }

    private fun validerTypeAndelFraTiltaksvariant() {
        val utbetalingRecord =
            KafkaFake
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
                .map { it.verdiEllerFeil<IverksettingDto>() }

        assertThat(
            utbetalingRecord.flatMap { it.utbetalinger },
        ).allMatch { it.stønad == StønadUtbetaling.REISE_TIL_SAMLING_TILTAK_GRUPPE_AMO }
    }
}
