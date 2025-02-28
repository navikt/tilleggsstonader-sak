package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.SakYtelseDvh
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
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
    @Test
    fun `mapping med hendelse MOTTATT`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 17L,
                eksternFagId = 29L,
                kategori = BehandlingKategori.NASJONAL,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
                hendelse = Hendelse.MOTTATT,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "17",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "29",
                sakId = "29",
                aktorId = aktørId,
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
                saksbehandler = saksbehandlerId,
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
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(behandlingId = behandlingId, ident = aktørId, eksternId = 1337L, eksternFagId = 8080L)

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
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
                aktorId = aktørId,
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
                saksbehandler = saksbehandlerId,
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
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 1999L,
                eksternFagId = 5150L,
                kategori = BehandlingKategori.EØS,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
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
                aktorId = aktørId,
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
                saksbehandler = saksbehandlerId,
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
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 33L,
                eksternFagId = 99L,
                kategori = BehandlingKategori.NASJONAL,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
                søkerHarStrengtFortroligAdresse = true,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "33",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "99",
                sakId = "99",
                aktorId = aktørId,
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

    @Test
    fun `mapping ved revurdering`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val forrigeBehandlingId = BehandlingId(UUID.randomUUID())
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 24L,
                eksternFagId = 48L,
                forrigeBehandlingId = forrigeBehandlingId,
                type = BehandlingType.REVURDERING,
                kategori = BehandlingKategori.NASJONAL,
                resultat = BehandlingResultat.INNVILGET,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "24",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "48",
                sakId = "48",
                aktorId = aktørId,
                mottattTid = henvendelseTidspunkt,
                registrertTid = henvendelseTidspunkt,
                ferdigBehandletTid = null,
                endretTid = henvendelseTidspunkt,
                tekniskTid = tekniskTid,
                sakYtelse = SakYtelseDvh.TILLEGG_BARNETILSYN,
                sakUtland = "Nasjonal",
                behandlingType = "REVURDERING",
                behandlingStatus = "MOTTATT",
                behandlingMetode = "AUTOMATISK",
                kravMottatt = null,
                opprettetAv = "VL",
                saksbehandler = saksbehandlerId,
                ansvarligEnhet = ArbeidsfordelingService.MASKINELL_JOURNALFOERENDE_ENHET,
                behandlingResultat = "INNVILGET",
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
    fun `mapping ved avslag`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 10L,
                eksternFagId = 20L,
                kategori = BehandlingKategori.NASJONAL,
                resultat = BehandlingResultat.AVSLÅTT,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "10",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "20",
                sakId = "20",
                aktorId = aktørId,
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
                saksbehandler = saksbehandlerId,
                ansvarligEnhet = ArbeidsfordelingService.MASKINELL_JOURNALFOERENDE_ENHET,
                behandlingResultat = "AVSLÅTT",
                resultatBegrunnelse = "UKJENT",
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
    fun `mapping ved henleggelse`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val aktørId = "9876543210127"
        val saksbehandlerId = "7873486250023"

        val henvendelseTidspunkt = osloNow()
        val hendelseTidspunkt = osloNow()
        val tekniskTid = osloNow()

        val saksbehandling =
            saksbehandling(
                behandlingId = behandlingId,
                ident = aktørId,
                eksternId = 7878L,
                eksternFagId = 8989L,
                kategori = BehandlingKategori.NASJONAL,
                resultat = BehandlingResultat.HENLAGT,
                henlagtÅrsak = HenlagtÅrsak.TRUKKET_TILBAKE,
            )

        val actual =
            map(
                saksbehandling = saksbehandling,
                saksbehandlerId = saksbehandlerId,
                henvendelseTidspunkt = henvendelseTidspunkt,
                hendelseTidspunkt = hendelseTidspunkt,
                tekniskTid = tekniskTid,
            )

        val expected =
            BehandlingDVH(
                behandlingId = "7878",
                behandlingUuid = behandlingId.id.toString(),
                saksnummer = "8989",
                sakId = "8989",
                aktorId = aktørId,
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
                saksbehandler = saksbehandlerId,
                ansvarligEnhet = ArbeidsfordelingService.MASKINELL_JOURNALFOERENDE_ENHET,
                behandlingResultat = "HENLAGT",
                resultatBegrunnelse = "TRUKKET_TILBAKE",
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

    fun saksbehandling(
        behandlingId: BehandlingId,
        ident: String,
        eksternId: Long,
        eksternFagId: Long,
        forrigeBehandlingId: BehandlingId? = null,
        type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
        resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
        henlagtÅrsak: HenlagtÅrsak? = null,
    ) = Saksbehandling(
        id = behandlingId,
        eksternId = eksternId,
        forrigeBehandlingId = forrigeBehandlingId,
        type = type,
        status = BehandlingStatus.OPPRETTET,
        steg = StegType.INNGANGSVILKÅR,
        kategori = kategori,
        årsak = BehandlingÅrsak.SØKNAD,
        kravMottatt = null,
        resultat = resultat,
        vedtakstidspunkt = null,
        henlagtÅrsak = henlagtÅrsak,
        henlagtBegrunnelse = null,
        ident = ident,
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
        saksbehandling: Saksbehandling,
        saksbehandlerId: String,
        henvendelseTidspunkt: LocalDateTime,
        hendelseTidspunkt: LocalDateTime,
        tekniskTid: LocalDateTime,
        søkerHarStrengtFortroligAdresse: Boolean = false,
        hendelse: Hendelse = Hendelse.MOTTATT,
        behandlingMetode: BehandlingMetode = BehandlingMetode.AUTOMATISK,
    ) = BehandlingsstatistikkService.mapTilBehandlingDVH(
        saksbehandling,
        behandlingId = saksbehandling.id,
        henvendelseTidspunkt = henvendelseTidspunkt,
        hendelse = hendelse,
        hendelseTidspunkt = hendelseTidspunkt,
        søkerHarStrengtFortroligAdresse = søkerHarStrengtFortroligAdresse,
        saksbehandlerId = saksbehandlerId,
        sisteOppgaveForBehandling = null,
        behandlingMetode = behandlingMetode,
        beslutterId = null,
        tekniskTid = tekniskTid,
        relatertBehandlingId = null,
    )
}
