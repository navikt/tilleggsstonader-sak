package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.SakYtelseDvh
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingsstatistikkMappingTest {
    @Test
    fun `mappy map`() {
        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling = saksbehandling()
        val behandlingId = BehandlingId(UUID.randomUUID())

        val actual =
            BehandlingsstatistikkService.mapTilBehandlingDVH(
                saksbehandling,
                behandlingId = behandlingId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
                søkerHarStrengtFortroligAdresse = false,
                saksbehandlerId = "<saksbehandler-test>",
                sisteOppgaveForBehandling = null,
                behandlingMetode = BehandlingMetode.MANUELL,
                beslutterId = null,
                tekniskTid = tekniskTid,
                relatertBehandlingId = null,
            )

        val expected =
            BehandlingDVH(
                behandlingId = saksbehandling.eksternId.toString(),
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "0",
                sakId = saksbehandling.eksternFagsakId.toString(),
                aktorId = saksbehandling.ident,
                mottattTid = henvendelseTidspunkt,
                registrertTid = henvendelseTidspunkt,
                ferdigBehandletTid = null,
                endretTid = henvendelseTidspunkt,
                tekniskTid = tekniskTid,
                sakYtelse = SakYtelseDvh.TILLEGG_BARNETILSYN,
                sakUtland = "Nasjonal",
                behandlingType = "FØRSTEGANGSBEHANDLING",
                behandlingStatus = "MOTTATT",
                behandlingMetode = "MANUELL",
                kravMottatt = null,
                opprettetAv = "VL",
                saksbehandler = "<saksbehandler-test>",
                ansvarligEnhet = ArbeidsfordelingService.MASKINELL_JOURNALFOERENDE_ENHET,
                behandlingResultat = "IKKE_SATT",
                resultatBegrunnelse = null,
                avsender = "Nav Tilleggstønader",
                versjon = Applikasjonsversjon.versjon,
                relatertBehandlingId = null,
                vedtakTid = null,
                utbetaltTid = null,
                forventetOppstartTid = null,
                papirSøknad = null,
                ansvarligBeslutter = null,
                totrinnsbehandling = false,
                vilkårsprøving = emptyList(),
                venteAarsak = null,
                behandlingBegrunnelse = null,
                revurderingOpplysningskilde = null,
                revurderingÅrsak = null,
                behandlingÅrsak = "SØKNAD",
            )

        assertThat(actual).isEqualTo(expected)
    }
}
