package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvslagTilsynBarnIntegrationTest : IntegrationTest() {
    @Test
    fun `skal gjennomføre avslag for tilsyn barn`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BARNETILSYN,
            ) {
                defaultTilsynBarnTestdata()
                vedtak {
                    avslag()
                }
            }

        val behandling = kall.behandling.hent(behandlingContext.behandlingId)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(behandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val avslag =
            kall.vedtak
                .hentVedtak(Stønadstype.BARNETILSYN, behandlingContext.behandlingId)
                .expectOkWithBody<AvslagTilsynBarnDto>()

        assertThat(avslag.årsakerAvslag).isEqualTo(listOf(ÅrsakAvslag.ANNET))
        assertThat(avslag.begrunnelse).isEqualTo("begrunnelse")
        assertThat(avslag.type).isEqualTo(TypeVedtak.AVSLAG)
    }
}
