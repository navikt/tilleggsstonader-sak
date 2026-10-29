package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMedMetadata
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.stereotype.Service

/**
 * Lar saksbehandler rette aktivitetspenger-markeringen på en oppgave.
 * Oppgaven i Gosys er eneste sannhet. Nye oppgaver på samme behandling arver markeringen fra forrige oppgave.
 */
@Service
class AktivitetspengerMarkeringService(
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
    private val tilgangService: TilgangService,
    private val unleashService: UnleashService,
) {
    fun endreMarkering(
        gsakOppgaveId: Long,
        gjelderAktivitetspenger: Boolean,
    ): OppgaveMedMetadata {
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvis(!unleashService.isEnabled(Toggle.SKAL_MARKERE_AKTIVITETSPENGER_OPPGAVER)) {
            "Markering av aktivitetspenger er ikke aktivert"
        }
        brukerfeilHvis(!gjelderAktivitetspenger && !unleashService.isEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER)) {
            "Du har ikke tilgang til å fjerne markering for aktivitetspenger"
        }

        val oppgave = oppgaveService.hentOppgave(gsakOppgaveId)

        validerTilgangTilOpgpaveEllerPerson(
            gsakOppgaveId = gsakOppgaveId,
            oppgave = oppgave,
        )
        validerKanEndreMarkering(oppgave)

        val behandlingstype = utledBehandlingstype(oppgave, gjelderAktivitetspenger)
        return oppgaveService.oppdaterBehandlingstype(oppgave, behandlingstype)
    }

    private fun validerTilgangTilOpgpaveEllerPerson(
        gsakOppgaveId: Long,
        oppgave: Oppgave,
    ) {
        val behandlingId = oppgaveRepository.findByGsakOppgaveId(gsakOppgaveId)?.behandlingId
        if (behandlingId != null) {
            tilgangService.validerSkrivetilgangTilBehandling(
                behandlingId = behandlingId,
                event = AuditLoggerEvent.UPDATE,
                validerTilordnetOppgave = false,
            )
        } else {
            val personIdent = oppgave.ident
            brukerfeilHvis(personIdent == null) { "Fant ikke bruker på oppgaven" }
            tilgangService.validerTilgangTilPerson(personIdent, AuditLoggerEvent.UPDATE)
        }
    }

    private fun validerKanEndreMarkering(oppgave: Oppgave) {
        brukerfeilHvis(oppgave.tema != Tema.TSO) {
            "Kan kun endre markering for aktivitetspenger på oppgaver for tilleggsstønader Nay"
        }
        brukerfeilHvis(oppgave.status == StatusEnum.FERDIGSTILT || oppgave.status == StatusEnum.FEILREGISTRERT) {
            "Kan ikke endre markering for aktivitetspenger på en lukket oppgave"
        }
        brukerfeilHvis(oppgave.behandlingstype !in støttedeBehandlingstyper) {
            "Kan ikke endre markering for aktivitetspenger på oppgave med behandlingstype ${oppgave.behandlingstype}"
        }
    }

    private fun utledBehandlingstype(
        oppgave: Oppgave,
        gjelderAktivitetspenger: Boolean,
    ): Behandlingstype? {
        val erKlage =
            oppgave.behandlingstype == Behandlingstype.Klage.value ||
                oppgave.behandlingstype == Behandlingstype.AktivitetspengerKlage.value
        return when {
            erKlage && gjelderAktivitetspenger -> Behandlingstype.AktivitetspengerKlage
            erKlage -> Behandlingstype.Klage
            gjelderAktivitetspenger -> Behandlingstype.Aktivitetspenger
            else -> null
        }
    }

    // Andre behandlingstyper, f.eks. tilbakekreving, skal ikke overskrives
    private val støttedeBehandlingstyper =
        listOf(
            null,
            Behandlingstype.Aktivitetspenger.value,
            Behandlingstype.Klage.value,
            Behandlingstype.AktivitetspengerKlage.value,
        )
}
