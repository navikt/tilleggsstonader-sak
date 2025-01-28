package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.tilHendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class BehandlingshistorikkServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Test
    fun `lagre behandling og hent historikk`() {
        /** Lagre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val behandlingHistorikk =
            behandlingshistorikkRepository.insert(
                Behandlingshistorikk(
                    behandlingId = behandling.id,
                    steg = behandling.steg,
                    opprettetAvNavn = "Saksbehandlernavn",
                    opprettetAv = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                ),
            )
        val hendelseshistorikkDto = behandlingHistorikk.tilHendelseshistorikkDto(saksbehandling(fagsak, behandling))

        /** Hent */
        val innslag: HendelseshistorikkDto =
            behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(fagsak, behandling))[0]

        assertThat(innslag).isEqualTo(hendelseshistorikkDto)
    }

    @Test
    fun `Finn hendelseshistorikk på behandling uten historikk`() {
        /** Lagre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        /** Hent */
        val list = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(fagsak, behandling))

        assertThat(list.isEmpty()).isTrue
    }

    @Test
    internal fun `skal slå sammen hendelser av typen opprettet`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val beslutteVedtak = behandling.copy(steg = StegType.BESLUTTE_VEDTAK)

        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), osloNow().minusDays(8))
        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), osloNow().minusDays(7))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), osloNow().minusDays(6))
        insert(beslutteVedtak, osloNow().minusDays(5), StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT)
        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), osloNow().minusDays(4))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), osloNow().minusDays(3))
        insert(beslutteVedtak, osloNow().minusDays(2), StegUtfall.BESLUTTE_VEDTAK_GODKJENT)

        val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))
        assertThat(historikk.map { it.hendelse }).containsExactly(
            Hendelse.VEDTAK_GODKJENT,
            Hendelse.SENDT_TIL_BESLUTTER,
            Hendelse.VEDTAK_UNDERKJENT,
            Hendelse.SENDT_TIL_BESLUTTER,
            Hendelse.OPPRETTET,
        )
    }

    @Test
    internal fun `flere sett og av vent skal ikke slåes sammen`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, steg = StegType.INNGANGSVILKÅR))
        insert(behandling, osloNow().minusDays(10))
        insert(behandling, osloNow().minusDays(8), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, osloNow().minusDays(5), StegUtfall.TATT_AV_VENT)
        insert(behandling, osloNow().minusDays(3), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, osloNow().minusDays(2), StegUtfall.TATT_AV_VENT)

        val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))
        assertThat(historikk.map { it.hendelse }).containsExactly(
            Hendelse.TATT_AV_VENT,
            Hendelse.SATT_PÅ_VENT,
            Hendelse.TATT_AV_VENT,
            Hendelse.SATT_PÅ_VENT,
            Hendelse.OPPRETTET,
        )
    }

    @Test
    internal fun `finn seneste behandlinghistorikk`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        insert(behandling, "A", osloNow().minusDays(1))
        insert(behandling, "B", osloNow().plusDays(1))
        insert(behandling, "C", osloNow())

        val siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)
        assertThat(siste.opprettetAvNavn).isEqualTo("B")
    }

    @Test
    internal fun `finn seneste behandlinghistorikk med type`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        insert(behandling, "A", osloNow().minusDays(1))
        insert(behandling, "B", osloNow().plusDays(1))
        insert(behandling, "C", osloNow())

        var siste =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(
                behandlingId = behandling.id,
                StegType.BESLUTTE_VEDTAK,
            )
        assertThat(siste).isNull()

        siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id, behandling.steg)
        assertThat(siste!!.opprettetAvNavn).isEqualTo("B")
    }

    private fun insert(
        behandling: Behandling,
        endretTid: LocalDateTime,
        utfall: StegUtfall? = null,
    ) {
        insert(behandling, "opprettetAv", endretTid, utfall)
    }

    private fun insert(
        behandling: Behandling,
        opprettetAv: String,
        endretTid: LocalDateTime,
        utfall: StegUtfall? = null,
    ) {
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = behandling.steg,
                utfall = utfall,
                opprettetAvNavn = opprettetAv,
                endretTid = endretTid,
            ),
        )
    }
}
