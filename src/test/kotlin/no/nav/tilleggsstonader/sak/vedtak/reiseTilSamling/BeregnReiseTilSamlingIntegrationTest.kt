package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingTsoRequest
import org.junit.jupiter.api.Test

class BeregnReiseTilSamlingIntegrationTest : IntegrationTest() {
    @Test
    fun `kan beregne reise til samling offentlig transport`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fomNay = 1 januar 2025
        val tomNay = 31 januar 2025

        val behandlingContextNay =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
                tilSteg = StegType.BEREGNE_YTELSE,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTsoReiseTilSamling(fomNay, tomNay)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fomNay, tomNay)
                    }
                }
                vilkår {
                    opprett {
                        offentligTransportReiseTilSamling(fomNay, tomNay)
                    }
                }
            }
        val vedtaksperioder =
            kall.vedtak
                .foreslåVedtaksperioder(behandlingContextNay.behandlingId)
                .map { it.tilVedtaksperiodeDto() }

        kall.beregnReiseTilSamling
            .beregn(
                behandlingContextNay.behandlingId,
                InnvilgelseReiseTilSamlingTsoRequest(
                    vedtaksperioder = vedtaksperioder,
                ),
            ).expectStatus()
            .isOk
    }
}
