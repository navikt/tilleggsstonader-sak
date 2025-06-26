package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.Behandlingshistorikk
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.tilHendelseshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
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
                    gitVersjon = Applikasjonsversjon.versjon,
                ),
            )
        val hendelseshistorikkDto = behandlingHistorikk.tilHendelseshistorikkDto(saksbehandling(fagsak, behandling))

        /** Hent */
        val innslag: BehandlingshistorikkDto =
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

        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), LocalDateTime.now().minusDays(8))
        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), LocalDateTime.now().minusDays(7))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), LocalDateTime.now().minusDays(6))
        insert(beslutteVedtak, LocalDateTime.now().minusDays(5), StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT)
        insert(behandling.copy(steg = StegType.INNGANGSVILKÅR), LocalDateTime.now().minusDays(4))
        insert(behandling.copy(steg = StegType.SEND_TIL_BESLUTTER), LocalDateTime.now().minusDays(3))
        insert(beslutteVedtak, LocalDateTime.now().minusDays(2), StegUtfall.BESLUTTE_VEDTAK_GODKJENT)

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
        insert(behandling, LocalDateTime.now().minusDays(10))
        insert(behandling, LocalDateTime.now().minusDays(8), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, LocalDateTime.now().minusDays(5), StegUtfall.TATT_AV_VENT)
        insert(behandling, LocalDateTime.now().minusDays(3), StegUtfall.SATT_PÅ_VENT)
        insert(behandling, LocalDateTime.now().minusDays(2), StegUtfall.TATT_AV_VENT)

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

        insert(behandling, "A", LocalDateTime.now().minusDays(1))
        insert(behandling, "B", LocalDateTime.now().plusDays(1))
        insert(behandling, "C", LocalDateTime.now())

        val siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)
        assertThat(siste.opprettetAvNavn).isEqualTo("B")
    }

    @Test
    internal fun `finn seneste behandlinghistorikk med type`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        insert(behandling, "A", LocalDateTime.now().minusDays(1))
        insert(behandling, "B", LocalDateTime.now().plusDays(1))
        insert(behandling, "C", LocalDateTime.now())

        var siste =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(
                behandlingId = behandling.id,
                StegType.BESLUTTE_VEDTAK,
            )
        assertThat(siste).isNull()

        siste = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id, behandling.steg)
        assertThat(siste!!.opprettetAvNavn).isEqualTo("B")
    }

    @Test
    internal fun `skal slette fritekst metadata ved ferdigstillelse`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        insert(
            behandling = behandling,
            opprettetAv = "A",
            endretTid = LocalDateTime.now(),
            steg = StegType.SEND_TIL_BESLUTTER,
        )

        insert(
            behandling = behandling,
            opprettetAv = "A",
            endretTid = LocalDateTime.now().plusDays(1),
            steg = StegType.BESLUTTE_VEDTAK,
            utfall = StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT,
            metadata = mapOf("begrunnelse" to "begrunnelse", "årsakerUnderkjent" to listOf(ÅrsakUnderkjent.INNGANGSVILKÅR)),
        )

        insert(
            behandling = behandling,
            opprettetAv = "A",
            endretTid = LocalDateTime.now().plusDays(2),
            steg = StegType.SEND_TIL_BESLUTTER,
            metadata = mapOf("kommentarTilBeslutter" to "kommentar"),
        )

        val historikkFørSletting =
            behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))

        assertThat(historikkFørSletting).anySatisfy { it.metadata?.containsKey("kommentarTilBeslutter") }
        assertThat(historikkFørSletting).anySatisfy { it.metadata?.containsKey("begrunnelse") }

        behandlingshistorikkService.slettFritekstMetadataVedFerdigstillelse(behandling.id)
        val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling(behandling = behandling))

        assertThat(historikk).hasSameSizeAs(historikkFørSletting)

        val historikkMedMetadata = historikk.mapNotNull { it.metadata }

        historikkMedMetadata.forEach { metadata ->
            assertThat(metadata).doesNotContainKeys("kommentarTilBeslutter", "begrunnelse")
        }

        historikk.find { it.hendelse == Hendelse.VEDTAK_UNDERKJENT }?.let {
            assertThat(it.metadata).containsKey("årsakerUnderkjent")
            assertThat(it.metadata?.get("årsakerUnderkjent")).isEqualTo(listOf(ÅrsakUnderkjent.INNGANGSVILKÅR.toString()))
        }
    }

    private fun insert(
        behandling: Behandling,
        endretTid: LocalDateTime,
        utfall: StegUtfall? = null,
    ) {
        insert(behandling, "opprettetAv", endretTid, utfall = utfall)
    }

    private fun insert(
        behandling: Behandling,
        opprettetAv: String,
        endretTid: LocalDateTime,
        steg: StegType = behandling.steg,
        utfall: StegUtfall? = null,
        metadata: Map<String, Any>? = null,
    ) {
        behandlingshistorikkRepository.insert(
            Behandlingshistorikk(
                behandlingId = behandling.id,
                steg = steg,
                utfall = utfall,
                opprettetAvNavn = opprettetAv,
                endretTid = endretTid,
                metadata = metadata?.let { JsonWrapper(objectMapper.writeValueAsString(metadata)) },
                gitVersjon = Applikasjonsversjon.versjon,
            ),
        )
    }
}
