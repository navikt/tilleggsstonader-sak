package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakIdRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakDomain
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtakTilsynBarn
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val eksternFagsakIdRepository: EksternFagsakIdRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val repository: TilsynBarnVedtakRepository,
) {

    fun hentBehandling(behandlingId: BehandlingId) = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentSaksbehandling(behandlingId: BehandlingId) = behandlingRepository.finnSaksbehandling(behandlingId)

    fun opprettBehandlingMedFagsak(
        behandling: Behandling,
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
        opprettGrunnlagsdata: Boolean = true,
        identer: Set<PersonIdent> = defaultIdenter,
    ): Behandling {
        val person = opprettPerson(fagsak(identer = identer))
        lagreFagsak(
            fagsak(
                id = behandling.fagsakId,
                stønadstype = stønadstype,
                fagsakPersonId = person.id,
            ),
        )
        return lagre(behandling, opprettGrunnlagsdata)
    }

    fun opprettPerson(ident: String) = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(PersonIdent(ident))))

    fun opprettPerson(person: FagsakPerson) = fagsakPersonRepository.insert(person)

    fun lagre(behandling: List<Behandling>) {
        behandling.forEach(this::lagre)
    }

    fun lagre(behandling: Behandling, opprettGrunnlagsdata: Boolean = true): Behandling {
        val dbBehandling = behandlingRepository.insert(behandling)
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = dbBehandling.id))

        if (opprettGrunnlagsdata) {
            opprettGrunnlagsdata(behandling.id)
        }

        return dbBehandling
    }

    fun oppdater(behandling: Behandling): Behandling {
        return behandlingRepository.update(behandling)
    }

    fun opprettGrunnlagsdata(behandlingId: BehandlingId) {
        grunnlagsdataService.opprettGrunnlagsdataHvisDetIkkeEksisterer(behandlingId)
    }

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        val fagsak = fagsakRepository.insert(
            FagsakDomain(
                id = fagsak.id,
                fagsakPersonId = person.id,
                stønadstype = fagsak.stønadstype,
                sporbar = fagsak.sporbar,
            ),
        )
        val eksternFagsakId = eksternFagsakIdRepository.insert(EksternFagsakId(fagsakId = fagsak.id))
        return fagsak.tilFagsakMedPerson(person.identer, eksternFagsakId)
    }

    fun opprettRevurdering(revurderFra: LocalDate, behandling: Behandling, fagsak: Fagsak): Behandling {
        oppdater(behandling.copy(status = BehandlingStatus.FERDIGSTILT))
        val forrgieVedtak = VedtakTilsynBarn(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            beregningsresultat = vedtakBeregningsresultat,
            vedtak = vedtaksdata,
        )
        repository.insert(forrgieVedtak)
        val revurdering =
            behandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                revurderFra = revurderFra,
                forrigeBehandlingId = behandling.id,
                status = BehandlingStatus.UTREDES,
                steg = StegType.BEREGNE_YTELSE,
            )
        lagre(revurdering)
        return hentBehandling(revurdering.id)
    }

    fun lagBehandlingOgRevurdering(): Behandling {
        val fagsak = fagsak()
        lagreFagsak(fagsak)
        val førsteBehandling = lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET))
        val revurdering =
            behandling(fagsak = fagsak, forrigeBehandlingId = førsteBehandling.id, type = BehandlingType.REVURDERING)
        return lagre(revurdering)
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson {
        return fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
            ?: hentPersonFraIdenter(fagsak)
            ?: opprettPerson(fagsak)
    }

    private fun hentPersonFraIdenter(fagsak: Fagsak): FagsakPerson? =
        fagsak.personIdenter.map { it.ident }
            .takeIf { it.isNotEmpty() }
            ?.let { fagsakPersonRepository.findByIdent(it) }

    private fun opprettPerson(fagsak: Fagsak) = fagsakPersonRepository.insert(
        FagsakPerson(
            fagsak.fagsakPersonId,
            identer = fagsak.personIdenter,
        ),
    )
}
