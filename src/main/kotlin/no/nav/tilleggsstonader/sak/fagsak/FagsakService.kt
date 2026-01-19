package no.nav.tilleggsstonader.sak.fagsak

import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingTilJournalføringDto
import no.nav.tilleggsstonader.sak.behandling.dto.tilBehandlingJournalDto
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakIdRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsaker
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FagsakService(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakRepository: FagsakRepository,
    private val eksternFagsakIdRepository: EksternFagsakIdRepository,
    private val personService: PersonService,
    private val behandlingService: BehandlingService,
) {
    init {
        // Initialiserer counters ved oppstart, slik at de starter på verdi 0
        Stønadstype.entries.forEach { stønadstypeOpprettetCounter(it) }
    }

    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        stønadstype: Stønadstype,
    ): Fagsak {
        val personIdenter = personService.hentFolkeregisterIdenter(personIdent)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = oppdatertPerson(person, gjeldendePersonIdent)
        val fagsak =
            fagsakRepository.findByFagsakPersonIdAndStønadstype(oppdatertPerson.id, stønadstype)
                ?: opprettFagsak(stønadstype, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun hentBehandlingerForPersonOgStønadstype(
        personIdent: String,
        stønadstype: Stønadstype,
    ): List<BehandlingTilJournalføringDto> =
        finnFagsak(personService.hentFolkeregisterIdenter(personIdent).identer(), stønadstype)?.let { fagsak ->
            behandlingService.hentBehandlinger(fagsak.id).map {
                it.tilBehandlingJournalDto()
            }
        } ?: emptyList()

    fun finnFagsak(
        personIdenter: Set<String>,
        stønadstype: Stønadstype,
    ): Fagsak? {
        if (personIdenter.isEmpty()) {
            return null
        }
        return fagsakRepository.findBySøkerIdent(personIdenter, stønadstype)?.tilFagsakMedPerson()
    }

    fun finnFagsaker(personIdenter: Set<String>): List<Fagsak> {
        if (personIdenter.isEmpty()) {
            return emptyList()
        }
        return fagsakRepository.findBySøkerIdent(personIdenter).map { it.tilFagsakMedPerson() }
    }

    fun finnFagsakerForFagsakPersonId(fagsakPersonId: FagsakPersonId): Fagsaker {
        val fagsaker =
            fagsakRepository
                .findByFagsakPersonId(fagsakPersonId)
                .map { it.tilFagsakMedPerson() }
                .associateBy { it.stønadstype }
        return Fagsaker(
            barnetilsyn = fagsaker[Stønadstype.BARNETILSYN],
            læremidler = fagsaker[Stønadstype.LÆREMIDLER],
            boutgifter = fagsaker[Stønadstype.BOUTGIFTER],
            dagligReiseTso = fagsaker[Stønadstype.DAGLIG_REISE_TSO],
            dagligReiseTsr = fagsaker[Stønadstype.DAGLIG_REISE_TSR],
        )
    }

    fun erLøpende(fagsakId: FagsakId): Boolean = fagsakRepository.harLøpendeUtbetaling(fagsakId)

    fun hentFagsak(fagsakId: FagsakId): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId).tilFagsakMedPerson()

    fun fagsakMedOppdatertPersonIdent(fagsakId: FagsakId): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val person = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val gjeldendeIdent = personService.hentFolkeregisterIdenter(person.hentAktivIdent()).gjeldende()
        val oppdatertPerson = oppdatertPerson(person, gjeldendeIdent)
        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    private fun oppdatertPerson(
        person: FagsakPerson,
        gjeldendePersonIdent: PdlIdent,
    ) = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)

    fun hentFagsakForBehandling(behandlingId: BehandlingId): Fagsak =
        fagsakRepository.finnFagsakTilBehandling(behandlingId)?.tilFagsakMedPerson()
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")

    fun hentEksternId(fagsakId: FagsakId): Long = eksternFagsakIdRepository.findByFagsakId(fagsakId).id

    fun hentFagsakPåEksternId(eksternFagsakId: Long): Fagsak =
        hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId)
            ?: error("Finner ikke fagsak til eksternFagsakId=$eksternFagsakId")

    fun hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId: Long): Fagsak? =
        fagsakRepository.finnMedEksternId(eksternFagsakId)?.tilFagsakMedPerson()

    fun hentAktivIdent(fagsakId: FagsakId): String = fagsakRepository.finnAktivIdent(fagsakId)

    fun hentMetadata(fagsakIder: Collection<FagsakId>): Map<FagsakId, FagsakMetadata> =
        fagsakRepository.hentFagsakMetadata(fagsakIder.toSet()).associateBy {
            it.id
        }

    private fun opprettFagsak(
        stønadstype: Stønadstype,
        fagsakPerson: FagsakPerson,
    ): FagsakDomain {
        val fagsak =
            fagsakRepository.insert(
                FagsakDomain(
                    stønadstype = stønadstype,
                    fagsakPersonId = fagsakPerson.id,
                ),
            )
        eksternFagsakIdRepository.insert(EksternFagsakId(fagsakId = fagsak.id))
        inkrementerStønadstypeOpprettetCounter(stønadstype)
        return fagsak
    }

    private fun FagsakDomain.tilFagsakMedPerson(): Fagsak {
        val personIdenter = fagsakPersonService.hentIdenter(this.fagsakPersonId)
        return this.tilFagsakMedPerson(personIdenter)
    }

    private fun FagsakDomain.tilFagsakMedPerson(personIdenter: Set<PersonIdent>): Fagsak {
        val eksternId = eksternFagsakIdRepository.findByFagsakId(this.id)
        return this.tilFagsakMedPerson(personIdenter, eksternId)
    }

    private fun inkrementerStønadstypeOpprettetCounter(stønadstype: Stønadstype) {
        stønadstypeOpprettetCounter(stønadstype).increment()
    }

    private fun stønadstypeOpprettetCounter(stønadstype: Stønadstype) =
        Metrics.counter("fagsak.stonadstype.opprettet", "type", stønadstype.name)
}
