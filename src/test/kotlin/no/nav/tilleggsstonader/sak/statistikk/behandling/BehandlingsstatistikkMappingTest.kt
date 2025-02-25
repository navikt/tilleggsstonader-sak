package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.SakYtelseDvh
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.*
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingsstatistikkMappingTest {
    fun saksbehandling(
        behandlingId: BehandlingId,
        eksternId: Long,
        eksternFagId: Long,
        kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    ) = Saksbehandling(
        id = behandlingId,
        eksternId = eksternId,
        forrigeBehandlingId = null,
        type = BehandlingType.FØRSTEGANGSBEHANDLING,
        status = BehandlingStatus.OPPRETTET,
        steg = StegType.INNGANGSVILKÅR,
        kategori = kategori,
        årsak = BehandlingÅrsak.SØKNAD,
        kravMottatt = null,
        resultat = BehandlingResultat.IKKE_SATT,
        vedtakstidspunkt = null,
        henlagtÅrsak = null,
        henlagtBegrunnelse = null,
        ident = "<ident-test>",
        fagsakId = FagsakId(UUID.randomUUID()),
        fagsakPersonId = FagsakPersonId(UUID.randomUUID()),
        eksternFagsakId = eksternFagId,
        stønadstype = Stønadstype.BARNETILSYN,
        revurderFra = null,
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
        endretAv = "<endret-av-test>",
        endretTid = LocalDateTime.now(),
    )

    fun map(
        behandlingId: BehandlingId,
        saksbehandling: Saksbehandling,
        henvendelseTidspunkt: LocalDateTime,
        hendelseTidspunkt: LocalDateTime,
        tekniskTid: LocalDateTime,
        søkerHarStrengtFortroligAdresse: Boolean = false,
        hendelse: Hendelse = Hendelse.MOTTATT,
        behandlingMetode: BehandlingMetode = BehandlingMetode.AUTOMATISK,
    ) = BehandlingsstatistikkService.mapTilBehandlingDVH(
        saksbehandling,
        behandlingId = behandlingId,
        henvendelseTidspunkt = henvendelseTidspunkt,
        hendelse = hendelse,
        hendelseTidspunkt = hendelseTidspunkt,
        søkerHarStrengtFortroligAdresse = søkerHarStrengtFortroligAdresse,
        saksbehandlerId = "<saksbehandler-test>",
        sisteOppgaveForBehandling = null,
        behandlingMetode = behandlingMetode,
        beslutterId = null,
        tekniskTid = tekniskTid,
        relatertBehandlingId = null,
    )

    @Test
    fun `mapping med hendelse MOTTATT`() {
        val behandlingId = BehandlingId(UUID.randomUUID())

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(behandlingId = behandlingId, eksternId = 17L, eksternFagId = 29L, kategori = BehandlingKategori.NASJONAL)

        val actual =
            map(
                behandlingId = behandlingId,
                saksbehandling = saksbehandling,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "17",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "29",
                sakId = "29",
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
                behandlingMetode = "AUTOMATISK",
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

    @Test
    fun `mapping med hendelse FERDIG`() {
        val behandlingId = BehandlingId(UUID.randomUUID())

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(behandlingId = behandlingId, eksternId = 1337L, eksternFagId = 8080L)

        val actual =
            map(
                behandlingId = behandlingId,
                saksbehandling = saksbehandling,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
                hendelse = Hendelse.FERDIG,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "1337",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "8080",
                sakId = "8080",
                aktorId = saksbehandling.ident,
                mottattTid = henvendelseTidspunkt,
                registrertTid = henvendelseTidspunkt,
                ferdigBehandletTid = hendelseTidspunkt,
                endretTid = hendelseTidspunkt,
                tekniskTid = tekniskTid,
                sakYtelse = SakYtelseDvh.TILLEGG_BARNETILSYN,
                sakUtland = "Nasjonal",
                behandlingType = "FØRSTEGANGSBEHANDLING",
                behandlingStatus = "FERDIG",
                behandlingMetode = "AUTOMATISK",
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

    @Test
    fun `mapping med kategori EØS`() {
        val behandlingId = BehandlingId(UUID.randomUUID())

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(behandlingId = behandlingId, eksternId = 1999L, eksternFagId = 5150L, kategori = BehandlingKategori.EØS)

        val actual =
            map(
                behandlingId = behandlingId,
                saksbehandling = saksbehandling,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "1999",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "5150",
                sakId = "5150",
                aktorId = saksbehandling.ident,
                mottattTid = henvendelseTidspunkt,
                registrertTid = henvendelseTidspunkt,
                ferdigBehandletTid = null,
                endretTid = henvendelseTidspunkt,
                tekniskTid = tekniskTid,
                sakYtelse = SakYtelseDvh.TILLEGG_BARNETILSYN,
                sakUtland = "Utland",
                behandlingType = "FØRSTEGANGSBEHANDLING",
                behandlingStatus = "MOTTATT",
                behandlingMetode = "AUTOMATISK",
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

    @Test
    fun `mapping med strengt fortrolig adresse`() {
        val behandlingId = BehandlingId(UUID.randomUUID())

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling = saksbehandling(behandlingId, 33L, 99L, kategori = BehandlingKategori.NASJONAL)

        val actual =
            map(
                behandlingId = behandlingId,
                saksbehandling = saksbehandling,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
                søkerHarStrengtFortroligAdresse = true,
                behandlingMetode = BehandlingMetode.AUTOMATISK,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "33",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "99",
                sakId = "99",
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
                behandlingMetode = "AUTOMATISK",
                kravMottatt = null,
                opprettetAv = "-5",
                saksbehandler = "-5",
                ansvarligEnhet = "-5",
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
