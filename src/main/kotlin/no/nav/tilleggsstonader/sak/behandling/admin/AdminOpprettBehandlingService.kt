package no.nav.tilleggsstonader.sak.behandling.admin

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gjeldende
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.visningsnavn
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminOpprettBehandlingService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val unleashService: UnleashService,
) {

    @Transactional
    fun opprettFørstegangsbehandling(ident: String, valgteBarn: Set<String>): BehandlingId {
        validerOpprettelseAvBehandling(ident, valgteBarn)

        val fagsak = fagsakService.hentEllerOpprettFagsak(ident, Stønadstype.BARNETILSYN)
        val behandling = behandlingService.opprettBehandling(
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET,
        )

        val behandlingBarn = valgteBarn.map { BehandlingBarn(behandlingId = behandling.id, ident = it) }
        barnService.opprettBarn(behandlingBarn)

        opprettBehandleSakOppgave(behandling)

        return behandling.id
    }

    private fun validerOpprettelseAvBehandling(ident: String, barn: Set<String>) {
        brukerfeilHvisIkke(unleashService.isEnabled(Toggle.ADMIN_KAN_OPPRETTE_BEHANDLING)) {
            "Feature toggle for å kunne opprette behandling er slått av"
        }
        feilHvis(barn.isEmpty()) {
            "Må velge minimum 1 barn"
        }

        val person = personService.hentPersonMedBarn(ident)

        validerAtBarnFinnesPåPerson(person, barn)
        validerAtDetIkkeFinnesBehandlingFraFør(ident)
    }

    fun hentPerson(ident: String): PersoninfoDto {
        validerAtDetIkkeFinnesBehandlingFraFør(ident)

        val person = personService.hentPersonMedBarn(ident)
        return PersoninfoDto(
            barn = person.barn.map {
                Barn(
                    ident = it.key,
                    navn = it.value.navn.gjeldende().visningsnavn(),
                )
            },
        )
    }

    private fun opprettBehandleSakOppgave(behandling: Behandling) {
        val task = OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
            OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                behandlingId = behandling.id,
                saksbehandler = SikkerhetContext.SYSTEM_FORKORTELSE,
                beskrivelse = "Manuelt opprettet sak fra journalpost. Skal saksbehandles i ny løsning.",
            ),
        )
        taskService.save(task)
    }

    private fun validerAtBarnFinnesPåPerson(person: SøkerMedBarn, barn: Set<String>) {
        feilHvis(barn.any { !person.barn.contains(it) }) {
            "Barn finnes ikke på person"
        }
    }

    private fun validerAtDetIkkeFinnesBehandlingFraFør(ident: String) {
        val personIdenter = personService.hentPersonIdenter(ident)
        val fagsak = fagsakService.finnFagsak(personIdenter.identer(), Stønadstype.BARNETILSYN)
        val behandlinger = fagsak?.let { behandlingService.hentBehandlinger(it.id) } ?: emptyList()
        brukerfeilHvis(behandlinger.isNotEmpty()) {
            "Det finnes allerede en behandling på personen. Gå til behandlingsoversikten og opprett endring derifra."
        }
    }
}
