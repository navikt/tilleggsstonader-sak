package no.nav.tilleggsstonader.sak.behandling.opprettelse

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførRegistrerKjørelisteSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettRegistrerKjørelisteIntegrationTest : CleanDatabaseIntegrationTest() {
    val fom = 1 januar 2026
    val tom = 31 januar 2026

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
    }

    @Test
    fun `manuelt opprettet kjørelistebehandling med REGISTRER_KJØRELISTE_FOR_BRUKER starter i REGISTRER_KJØRELISTE steg`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(stønadstype = Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val manuellKjørelisteBehandlingId =
            opprettRevurdering(
                OpprettBehandlingDto(
                    fagsakId = behandlingContext.fagsakId,
                    årsak = BehandlingÅrsak.REGISTRER_KJØRELISTE_FOR_BRUKER,
                    nyeOpplysningerMetadata = null,
                    kravMottatt = LocalDate.now(),
                    forenkletBehandlingstype = ForenkletBehandlingstype.KJØRELISTE,
                ),
            )

        val behandling = testoppsettService.hentBehandling(manuellKjørelisteBehandlingId)
        assertThat(behandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(behandling.årsak).isEqualTo(BehandlingÅrsak.REGISTRER_KJØRELISTE_FOR_BRUKER)
        assertThat(behandling.steg).isEqualTo(StegType.REGISTRER_KJØRELISTE)
    }

    @Test
    fun `håndtering av REGISTRER_KJØRELISTE steg setter behandlingen videre til KJØRELISTE`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(stønadstype = Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val manuellKjørelisteBehandlingId =
            opprettRevurdering(
                OpprettBehandlingDto(
                    fagsakId = behandlingContext.fagsakId,
                    årsak = BehandlingÅrsak.REGISTRER_KJØRELISTE_FOR_BRUKER,
                    nyeOpplysningerMetadata = null,
                    kravMottatt = LocalDate.now(),
                    forenkletBehandlingstype = ForenkletBehandlingstype.KJØRELISTE,
                ),
            )

        val nesteSteg = gjennomførRegistrerKjørelisteSteg(manuellKjørelisteBehandlingId)
        assertThat(nesteSteg).isEqualTo(StegType.KJØRELISTE)
    }
}
