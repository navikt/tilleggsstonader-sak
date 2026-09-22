package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Tester innvilgelse av reise til samling for tema **TSR**.
 * Se [InnvilgelseReiseTilSamlingTsoIntegrationTest] for tilsvarende test for tema TSO.
 */
class InnvilgelseReiseTilSamlingTsrIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

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

        val behandlingContext =
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

        validerTypeAndelFraTiltaksvariant(behandlingContext.behandlingId)
    }

    private fun validerTypeAndelFraTiltaksvariant(behandlingId: BehandlingId) {
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse

        assertThat(andeler).isNotEmpty
        assertThat(andeler).allMatch { it.type == TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_AMO }
    }
}
