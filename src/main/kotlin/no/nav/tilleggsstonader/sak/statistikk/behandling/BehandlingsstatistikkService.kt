package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.TotrinnsbehandlingStatusDvh
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.ZONE_ID_OSLO
import no.nav.tilleggsstonader.sak.util.zonedNow
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingsstatistikkService(
    private val behandlingsstatistikkProducer: BehandlingKafkaProducer,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val personService: PersonService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val søknadService: SøknadService,
) {

    @Transactional
    fun sendBehandlingstatistikk(
        behandlingId: UUID,
        hendelse: Hendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?,
        oppgaveId: Long?,
        behandlingMetode: BehandlingMetode?,
    ) {
        val behandlingDVH = mapTilBehandlingDVH(
            behandlingId,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler,
            oppgaveId,
            behandlingMetode,
        )
        behandlingsstatistikkProducer.sendBehandling(behandlingDVH)
    }

    private fun mapTilBehandlingDVH(
        behandlingId: UUID,
        hendelse: Hendelse,
        hendelseTidspunkt: LocalDateTime,
        gjeldendeSaksbehandler: String?,
        oppgaveId: Long?,
        behandlingMetode: BehandlingMetode?,
    ): BehandlingDVH {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling).atZone(ZONE_ID_OSLO)
        val søkerHarStrengtFortroligAdresse = evaluerAdresseBeskyttelseStrengtFortrolig(saksbehandling.ident)
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontroll(behandlingId)
        val saksbehandlerId = finnSaksbehandler(hendelse, gjeldendeSaksbehandler, totrinnskontroll)
        val beslutterId = totrinnskontroll?.beslutter
        val relatertEksternBehandlingId: String? =
            saksbehandling.forrigeBehandlingId?.let { behandlingService.hentEksternBehandlingId(it).id.toString() }

        return BehandlingDVH(
            behandlingId = saksbehandling.eksternId.toString(),
            behandlingUuid = behandlingId.toString(),
            sakId = saksbehandling.eksternFagsakId.toString(),
            aktorId = saksbehandling.ident,
            registrertTid = henvendelseTidspunkt,
            endretTid = if (Hendelse.MOTTATT == hendelse) henvendelseTidspunkt else hendelseTidspunkt.atZone(
                ZONE_ID_OSLO
            ),
            tekniskTid = zonedNow(),
            behandlingStatus = hendelse.name,
            opprettetAv = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig = søkerHarStrengtFortroligAdresse,
                verdi = saksbehandling.opprettetAv,
            ),
            saksnummer = saksbehandling.eksternFagsakId.toString(),
            mottattTid = henvendelseTidspunkt,
            kravMottatt = null, // TODO ?
            saksbehandler = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig = søkerHarStrengtFortroligAdresse,
                verdi = saksbehandlerId,
            ),
            ansvarligEnhet = maskerVerdiHvisStrengtFortrolig(
                erStrengtFortrolig = søkerHarStrengtFortroligAdresse,
                verdi = sisteOppgaveForBehandling?.tildeltEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
            ),
            behandlingMetode = behandlingMetode?.name ?: "MANUELL",
            behandlingÅrsak = saksbehandling.årsak.name,
            avsender = "NAV Tilleggstønader",
            behandlingType = saksbehandling.type.name,
            sakYtelse = saksbehandling.stønadstype.name,
            behandlingResultat = saksbehandling.resultat.name,
            resultatBegrunnelse = utledResultatBegrunnelse(saksbehandling),
            ansvarligBeslutter =
            if (!beslutterId.isNullOrEmpty()) {
                maskerVerdiHvisStrengtFortrolig(
                    erStrengtFortrolig = søkerHarStrengtFortroligAdresse,
                    verdi = beslutterId.toString(),
                )
            } else {
                null
            },
            vedtakTid = if (Hendelse.VEDTATT == hendelse) {
                hendelseTidspunkt.atZone(ZONE_ID_OSLO)
            } else {
                null
            },
            ferdigBehandletTid = if (Hendelse.FERDIG == hendelse) {
                hendelseTidspunkt.atZone(ZONE_ID_OSLO)
            } else {
                null
            },
            totrinnsbehandling = utledTotrinnskontrollStatus(totrinnskontroll),
            sakUtland = mapTilStreng(saksbehandling.kategori),
            relatertBehandlingId = relatertEksternBehandlingId,
            versjon = Applikasjonsversjon.versjon,
            vilkårsprøving = emptyList(), // TODO: Implementer dette i samarbeid med Team SAK. Ikke kritisk å ha med i starten.
            revurderingÅrsak = null, // TODO aktiver når revurdering er implementert
            revurderingOpplysningskilde = null, // TODO aktiver når revurdering er implementert
            venteAarsak = null, // TODO?
            papirSøknad = null, // TODO?
        )
    }

    private fun utledTotrinnskontrollStatus(totrinnskontroll: Totrinnskontroll?): TotrinnsbehandlingStatusDvh {
        return when (totrinnskontroll?.status) {
            TotrinnInternStatus.UNDERKJENT -> TotrinnsbehandlingStatusDvh.UNDERKJENT
            TotrinnInternStatus.GODKJENT -> TotrinnsbehandlingStatusDvh.GODKJENT

            TotrinnInternStatus.ANGRET, TotrinnInternStatus.KAN_FATTE_VEDTAK, null,
            -> TotrinnsbehandlingStatusDvh.IKKE_GJENNOMFØRT
        }
    }

    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave? {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId)?.gsakOppgaveId

        return gsakOppgaveId?.let { oppgaveService.hentOppgave(it) }
    }

    private fun finnSaksbehandler(
        hendelse: Hendelse,
        gjeldendeSaksbehandler: String?,
        totrinnskontroll: Totrinnskontroll?,
    ): String {
        return when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT, Hendelse.VENTER ->
                gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse=$hendelse")

            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.FERDIG ->
                totrinnskontroll?.saksbehandler ?: gjeldendeSaksbehandler
                ?: error("Mangler totrinnskontroll for hendelse=$hendelse")
        }
    }

    private fun finnHenvendelsestidspunkt(saksbehandling: Saksbehandling): LocalDateTime {
        return when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING ->
                søknadService.hentSøknadBarnetilsyn(saksbehandling.id)?.mottattTidspunkt
                    ?: saksbehandling.opprettetTid

            BehandlingType.REVURDERING -> saksbehandling.opprettetTid
        }
    }

    private fun evaluerAdresseBeskyttelseStrengtFortrolig(personIdent: String): Boolean {
        val adresseStatus =
            personService.hentPersonKortBolk(listOf(personIdent)).values.single().adressebeskyttelse.gradering()
        return when (adresseStatus) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
            AdressebeskyttelseGradering.FORTROLIG, AdressebeskyttelseGradering.UGRADERT -> false
        }
    }

    private fun maskerVerdiHvisStrengtFortrolig(
        erStrengtFortrolig: Boolean,
        verdi: String,
    ): String {
        if (erStrengtFortrolig) {
            return "-5" // -5 er ein kode som dvh forstår som maskert med årsak i strengtfortrolig, og behandler datasettet deretter.
        }
        return verdi
    }

    private fun mapTilStreng(kategori: BehandlingKategori?) = when (kategori) {
        BehandlingKategori.EØS -> "Utland"
        BehandlingKategori.NASJONAL -> "Nasjonal"
        null -> "Nasjonal"
    }

    private fun utledResultatBegrunnelse(behandling: Saksbehandling): String? =
        when (behandling.resultat) {
            BehandlingResultat.HENLAGT -> behandling.henlagtÅrsak?.name
            BehandlingResultat.AVSLÅTT -> TODO() // Implementer når vi har lagt inn støtte for avslag

            else -> null
        }
}
