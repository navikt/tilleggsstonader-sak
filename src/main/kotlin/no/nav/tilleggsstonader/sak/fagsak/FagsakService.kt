package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakDto
import no.nav.tilleggsstonader.sak.fagsak.dto.tilDto
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakRepository: FagsakRepository,
    private val personService: PersonService,
) {
    fun hentEllerOpprettFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        return fagsakTilDto(hentEllerOpprettFagsak(personIdent, stønadstype))
    }

    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        stønadstype: Stønadstype,
    ): Fagsak {
        val personIdenter = personService.hentPersonIdenter(personIdent)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = oppdatertPerson(person, gjeldendePersonIdent)
        val fagsak = fagsakRepository.findByFagsakPersonIdAndStønadstype(oppdatertPerson.id, stønadstype)
            ?: opprettFagsak(stønadstype, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun harFagsak(personIdenter: Set<String>) = fagsakRepository.findBySøkerIdent(personIdenter).isNotEmpty()

    fun finnFagsak(personIdenter: Set<String>, stønadstype: Stønadstype): Fagsak? =
        fagsakRepository.findBySøkerIdent(personIdenter, stønadstype)?.tilFagsakMedPerson()

    fun finnFagsaker(personIdenter: Set<String>): List<Fagsak> =
        fagsakRepository.findBySøkerIdent(personIdenter).map { it.tilFagsakMedPerson() }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto {
        return fagsakTilDto(hentFagsak(fagsakId))
    }

    fun fagsakTilDto(fagsak: Fagsak): FagsakDto {
        // val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(fagsak.id)
        val erLøpende = erLøpende(fagsak)
        return fagsak.tilDto(
            erLøpende = erLøpende,
        )
    }
    /*
        fun finnFagsakerForFagsakPersonId(fagsakPersonId: UUID): Fagsaker {
            val fagsaker = fagsakRepository.findByFagsakPersonId(fagsakPersonId)
                .map { it.tilFagsakMedPerson() }
                .associateBy { it.stønadstype }
            return Fagsaker(
                overgangsstønad = fagsaker[Stønadstype.OVERGANGSSTØNAD],
                barnetilsyn = fagsaker[Stønadstype.BARNETILSYN],
                skolepenger = fagsaker[Stønadstype.SKOLEPENGER],
            )
        }

     */

    fun erLøpende(fagsak: Fagsak): Boolean {
        return fagsakRepository.harLøpendeUtbetaling(fagsak.id)
    }

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId).tilFagsakMedPerson()

    fun fagsakMedOppdatertPersonIdent(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val person = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val gjeldendeIdent = personService.hentPersonIdenter(person.hentAktivIdent()).gjeldende()
        val oppdatertPerson = oppdatertPerson(person, gjeldendeIdent)
        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun fagsakerMedOppdatertePersonIdenter(fagsakId: List<UUID>): List<Fagsak> {
        val fagsaker = fagsakRepository.findAllById(fagsakId)
        val personer = fagsakPersonService.hentPersoner(fagsaker.map { it.fagsakPersonId }).associateBy { it.id }

        val gjeldendeIdenter = personService.hentIdenterBolk(personer.values.map { it.hentAktivIdent() })

        return fagsaker.map {
            val person = personer[it.fagsakPersonId]!!
            val gjeldendeIdent = gjeldendeIdenter[person.hentAktivIdent()]
            val oppdatertPerson = gjeldendeIdent?.let { oppdatertPerson(person, gjeldendeIdent) } ?: person
            it.tilFagsakMedPerson(oppdatertPerson.identer)
        }
    }

    private fun oppdatertPerson(
        person: FagsakPerson,
        gjeldendePersonIdent: PdlIdent,
    ) = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        return fagsakRepository.finnFagsakTilBehandling(behandlingId)?.tilFagsakMedPerson()
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentFagsakPåEksternId(eksternFagsakId: Long): Fagsak =
        fagsakRepository.finnMedEksternId(eksternFagsakId)
            ?.tilFagsakMedPerson()
            ?: error("Finner ikke fagsak til eksternFagsakId=$eksternFagsakId")

    fun hentFagsakDtoPåEksternId(eksternFagsakId: Long): FagsakDto {
        return hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId)
            ?: error("Kan ikke finne fagsak med eksternId=$eksternFagsakId")
    }

    fun hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId: Long): FagsakDto? {
        return fagsakRepository.finnMedEksternId(eksternFagsakId)
            ?.tilFagsakMedPerson()
            ?.let { fagsakTilDto(it) }
    }

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

    fun hentAktiveIdenter(fagsakId: Set<UUID>): Map<UUID, String> {
        if (fagsakId.isEmpty()) return emptyMap()

        val aktiveIdenter = fagsakRepository.finnAktivIdenter(fagsakId)
        feilHvis(!aktiveIdenter.map { it.first }.containsAll(fagsakId)) {
            "Finner ikke ident til fagsaker ${aktiveIdenter.map { it.first }.filterNot(fagsakId::contains)}"
        }
        return aktiveIdenter.associateBy({ it.first }, { it.second })
    }

    private fun opprettFagsak(stønadstype: Stønadstype, fagsakPerson: FagsakPerson): FagsakDomain {
        return fagsakRepository.insert(
            FagsakDomain(
                stønadstype = stønadstype,
                fagsakPersonId = fagsakPerson.id,
            ),
        )
    }

    fun FagsakDomain.tilFagsakMedPerson(): Fagsak {
        val personIdenter = fagsakPersonService.hentIdenter(this.fagsakPersonId)
        return this.tilFagsakMedPerson(personIdenter)
    }
}
