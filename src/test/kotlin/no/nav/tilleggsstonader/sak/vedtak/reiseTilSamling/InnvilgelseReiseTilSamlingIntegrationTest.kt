package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InnvilgelseReiseTilSamlingIntegrationTest : IntegrationTest() {
    @Test
    fun `kan innvilge reise til samling offentlig transport`() {
        val fomNay = 1 januar 2025
        val tomNay = 31 januar 2025

        val behandlingContextNay =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO,
                tilSteg = StegType.SIMULERING,
            ) {
                defaultReiseTilSamlingTSOTestdata(
                    fom = fomNay,
                    tom = tomNay,
                )
            }
        val vedtak =
            kall.vedtak
                .hentVedtak(Stønadstype.REISE_TIL_SAMLING_TSO, behandlingContextNay.behandlingId)
                .expectOkWithBody<InnvilgelseReiseTilSamlingResponse>()

        assertThat(vedtak.gjelderFraOgMed).isEqualTo(fomNay)
        assertThat(vedtak.gjelderTilOgMed).isEqualTo(tomNay)
    }
}
