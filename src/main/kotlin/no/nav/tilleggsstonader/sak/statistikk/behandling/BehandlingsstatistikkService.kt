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
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.TotrinnkontrollStatus
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class BehandlingsstatistikkService(
    private val behandlingsstatistikkProducer: BehandlingKafkaProducer,
    private val behandlingService: BehandlingService,
    private val applikasjonsversjon: Applikasjonsversjon,
    private val oppgaveService: OppgaveService,
    private val personService: PersonService,
    private val totrinnskontrollService: TotrinnskontrollService,
) {
    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    @Transactional
    fun sendBehandlingstatistikk(behandlingId: UUID, hendelse: Hendelse, hendelseTidspunkt: LocalDateTime, gjeldendeSaksbehandler: String?, oppgaveId: Long?, behandlingMetode: BehandlingMetode?) {
        val behandlingDVH = mapTilBehandlingDVH(behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode)
        behandlingsstatistikkProducer.sendBehandling(behandlingDVH)
    }

    private fun mapTilBehandlingDVH(behandlingId: UUID, hendelse: Hendelse, hendelseTidspunkt: LocalDateTime, gjeldendeSaksbehandler: String?, oppgaveId: Long?, behandlingMetode: BehandlingMetode?): BehandlingDVH {
        val tekniskTid = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling)
        val strengtFortroligAdresse = evalerAdresseBeskyttelseStrengtFortrolig(saksbehandling.ident)
        val saksbehandlerId = finnSaksbehandler(hendelse, totrinnskontrollService, gjeldendeSaksbehandler, behandlingId)
        val beslutterId = settBeslutterId(hendelse, behandlingId)
        val relatertEksternBehandlingId: Long? = saksbehandling.forrigeBehandlingId?.let { behandlingService.hentSaksbehandling(it).eksternId }

        return BehandlingDVH(
            behandlingId = saksbehandling.eksternId,
            sakId = saksbehandling.eksternId,
            personIdent = saksbehandling.ident,
            registrertTid = saksbehandling.opprettetTid.atZone(zoneIdOslo)
                ?: henvendelseTidspunkt.atZone(zoneIdOslo),
            endretTid = hendelseTidspunkt.atZone(zoneIdOslo),
            tekniskTid = tekniskTid,
            behandlingStatus = hendelse.name,
            opprettetAv = maskerVerdiHvisStrengtFortrolig(
                strengtFortroligAdresse,
                saksbehandlerId,
            ),
            saksnummer = saksbehandling.eksternFagsakId,
            mottattTid = henvendelseTidspunkt.atZone(zoneIdOslo),
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
            resultatBegrunnelse = null, // TODO er fritekstfelt og er ikke ønsket i statestikk før enum er implementert
            ansvarligBeslutter =
            if (Hendelse.BESLUTTET == hendelse && beslutterId.isNotNullOrEmpty()) {
                maskerVerdiHvisStrengtFortrolig(
                    strengtFortroligAdresse,
                    beslutterId.toString(),
                )
            } else {
                null
            },
            vedtakTid = if (Hendelse.VEDTATT == hendelse) {
                hendelseTidspunkt.atZone(zoneIdOslo)
            } else {
                null
            },
            ferdigBehandletTid = if (Hendelse.FERDIG == hendelse) {
                hendelseTidspunkt.atZone(zoneIdOslo)
            } else {
                null
            },
            totrinnsbehandling = sjekkSatusforToTrinn(behandlingId),
            sakUtland = mapTilStreng(saksbehandling.kategori),
            relatertBehandlingId = relatertEksternBehandlingId,
            versjon = applikasjonsversjon,
            revurderingÅrsak = null, // TODO aktivere når revurdering er implementert
            revurderingOpplysningskilde = null, // TODO aktivere når revurdering er implementert
        )
    }

    private fun settBeslutterId(
        hendelse: Hendelse,
        behandlingId: UUID,
    ): String? {
        val beslutterId = if (hendelse.erBesluttetEllerFerdig()) {
            totrinnskontrollService.hentBeslutter(behandlingId)
        } else {
            null
        }
        return beslutterId
    }

    private fun sjekkSatusforToTrinn(behandlingId: UUID): Boolean {
        val status = totrinnskontrollService.hentTotrinnskontrollStatus(behandlingId).status
        return when (status) {
            TotrinnkontrollStatus.GODKJENT -> true
            else -> false
        }
    }

    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave? {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId)?.gsakOppgaveId

        return gsakOppgaveId?.let { oppgaveService.hentOppgave(it) }
    }

    private fun Hendelse.erBesluttetEllerFerdig() = this.name == Hendelse.BESLUTTET.name || this.name == Hendelse.FERDIG.name

    private fun finnSaksbehandler(hendelse: Hendelse, totrinnskontrollService: TotrinnskontrollService, gjeldendeSaksbehandler: String?, behandlingId: UUID): String {
        return when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT, Hendelse.VENTER, Hendelse.HENLAGT ->
                gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse")
            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.FERDIG -> totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId)
        }
    }

    private fun finnHenvendelsestidspunkt(saksbehandling: Saksbehandling): LocalDateTime {
        return when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> saksbehandling.opprettetTid
            BehandlingType.REVURDERING -> saksbehandling.opprettetTid
        }
    }
    private fun evalerAdresseBeskyttelseStrengtFortrolig(personIdent: String): Boolean {
        val adresseStatus = personService.hentPersonKortBolk(listOf(personIdent)).values.single().adressebeskyttelse.gradering()
        return when (adresseStatus) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
            AdressebeskyttelseGradering.FORTROLIG, AdressebeskyttelseGradering.UGRADERT -> false
        }
    }

    fun String?.isNotNullOrEmpty() = this != null && this.isNotEmpty()

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
