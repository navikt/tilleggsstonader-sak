package no.nav.tilleggsstonader.sak.statistikk.behandling

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingKategori
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.ZONE_ID_OSLO
import no.nav.tilleggsstonader.sak.util.zonedNow
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
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
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling)
        val strengtFortroligAdresse = evaluerAdresseBeskyttelseStrengtFortrolig(saksbehandling.ident)
        val saksbehandlerId = finnSaksbehandler(hendelse, totrinnskontrollService, gjeldendeSaksbehandler, behandlingId)
        val beslutterId = utledBeslutterId(hendelse, behandlingId)
        val relatertEksternBehandlingId: String? =
            saksbehandling.forrigeBehandlingId?.let { behandlingService.hentSaksbehandling(it).eksternId.toString() }

        return BehandlingDVH(
            behandlingId = saksbehandling.eksternId.toString(),
            behandlingUuid = behandlingId.toString(),
            sakId = saksbehandling.eksternId.toString(),
            aktorId = saksbehandling.ident,
            registrertTid = saksbehandling.opprettetTid.atZone(ZONE_ID_OSLO)
                ?: henvendelseTidspunkt.atZone(ZONE_ID_OSLO),
            endretTid = hendelseTidspunkt.atZone(ZONE_ID_OSLO),
            tekniskTid = zonedNow(),
            behandlingStatus = hendelse.name,
            opprettetAv = maskerVerdiHvisStrengtFortrolig(
                strengtFortroligAdresse,
                saksbehandlerId,
            ),
            saksnummer = saksbehandling.eksternFagsakId.toString(),
            mottattTid = henvendelseTidspunkt.atZone(ZONE_ID_OSLO),
            saksbehandler = maskerVerdiHvisStrengtFortrolig(
                strengtFortroligAdresse,
                saksbehandlerId,
            ),
            ansvarligEnhet = maskerVerdiHvisStrengtFortrolig(
                strengtFortroligAdresse,
                sisteOppgaveForBehandling?.tildeltEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
            ),
            behandlingMetode = behandlingMetode?.name ?: "MANUELL",
            behandlingÅrsak = saksbehandling.årsak.name,
            avsender = "NAV Tilleggstønader",
            behandlingType = BehandlingType.valueOf(saksbehandling.type.name).toString(),
            sakYtelse = saksbehandling.stønadstype.name,
            behandlingResultat = saksbehandling.resultat.name,
            resultatBegrunnelse = null, // TODO er fritekstfelt, og er ikke ønsket i statistikk før enum er implementert
            ansvarligBeslutter =
            if (!beslutterId.isNullOrEmpty()) {
                maskerVerdiHvisStrengtFortrolig(
                    strengtFortroligAdresse,
                    beslutterId.toString(),
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
            totrinnsbehandling = totrinnskontrollErGodkjent(behandlingId),
            sakUtland = mapTilStreng(saksbehandling.kategori),
            relatertBehandlingId = relatertEksternBehandlingId,
            versjon = Applikasjonsversjon.versjon,
            vilkårsprøving = emptyList(), // TODO: Implementer dette i samarbeid med Team SAK. Ikke kritisk å ha med i starten.
            revurderingÅrsak = null, // TODO aktiver når revurdering er implementert
            revurderingOpplysningskilde = null, // TODO aktiver når revurdering er implementert
        )
    }

    private fun utledBeslutterId(
        hendelse: Hendelse,
        behandlingId: UUID,
    ): String? {
        return if (hendelse.erBesluttetEllerFerdig()) {
            totrinnskontrollService.hentBeslutter(behandlingId)
        } else {
            null
        }
    }

    private fun totrinnskontrollErGodkjent(behandlingId: UUID): Boolean {
        val totrinnskontrollstatus = totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId).status
        return totrinnskontrollstatus === TotrinnkontrollStatus.GODKJENT
    }


    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave? {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId)?.gsakOppgaveId

        return gsakOppgaveId?.let { oppgaveService.hentOppgave(it) }
    }

    private fun Hendelse.erBesluttetEllerFerdig() =
        this.name == Hendelse.BESLUTTET.name || this.name == Hendelse.FERDIG.name

    private fun finnSaksbehandler(
        hendelse: Hendelse,
        totrinnskontrollService: TotrinnskontrollService,
        gjeldendeSaksbehandler: String?,
        behandlingId: UUID,
    ): String {
        return when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT, Hendelse.VENTER, Hendelse.HENLAGT ->
                gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse")

            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.FERDIG ->
                totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId)
        }
    }

    private fun finnHenvendelsestidspunkt(saksbehandling: Saksbehandling): LocalDateTime {
        return when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> saksbehandling.opprettetTid
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
            return "-5"
        }
        return verdi
    }

    private fun mapTilStreng(kategori: BehandlingKategori?) = when (kategori) {
        BehandlingKategori.EØS -> "Utland"
        BehandlingKategori.NASJONAL -> "Nasjonal"
        null -> "Nasjonal"
    }
}
