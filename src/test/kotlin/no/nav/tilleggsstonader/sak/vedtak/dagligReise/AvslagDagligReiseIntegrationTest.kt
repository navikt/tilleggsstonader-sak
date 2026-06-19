package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AvslagDagligReiseIntegrationTest : IntegrationTest() {
    @Test
    fun `skal gjennomføre avslag for daglig reise tso`() {
        assertAvslag(Stønadstype.DAGLIG_REISE_TSO) {
            defaultDagligReiseTsoTestdata()
        }
    }

    @Test
    fun `skal gjennomføre avslag for daglig reise tsr`() {
        assertAvslag(Stønadstype.DAGLIG_REISE_TSR) {
            defaultDagligReiseTsrTestdata()
        }
    }

    @Test
    fun `skal innvilge daglig reise med privat bil og deretter avslå i revurdering`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata()
            }
        assertThat(kall.privatBil.hentRammevedtak(førstegangsbehandlingContext.ident)).isNotEmpty

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                vedtak {
                    avslag()
                }
            }

        val avslag =
            kall.vedtak
                .hentVedtak(Stønadstype.DAGLIG_REISE_TSO, revurderingId)
                .expectOkWithBody<AvslagDagligReiseDto>()

        assertThat(avslag.årsakerAvslag).isEqualTo(listOf(ÅrsakAvslag.ANNET))
        assertThat(avslag.begrunnelse).isEqualTo("begrunnelse")
        assertThat(avslag.type).isEqualTo(TypeVedtak.AVSLAG)
    }

    private fun assertAvslag(
        stønadstype: Stønadstype,
        testdata: no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl.() -> Unit,
    ) {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = stønadstype,
            ) {
                testdata()
                vedtak {
                    avslag()
                }
            }

        val behandling = kall.behandling.hent(behandlingContext.behandlingId)
        assertThat(behandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(behandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)

        val avslag =
            kall.vedtak
                .hentVedtak(stønadstype, behandlingContext.behandlingId)
                .expectOkWithBody<AvslagDagligReiseDto>()

        assertThat(avslag.årsakerAvslag).isEqualTo(listOf(ÅrsakAvslag.ANNET))
        assertThat(avslag.begrunnelse).isEqualTo("begrunnelse")
        assertThat(avslag.type).isEqualTo(TypeVedtak.AVSLAG)
    }
}
