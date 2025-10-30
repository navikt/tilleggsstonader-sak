package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingIdRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
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
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.migrering.routing.SkjemaRouting
import no.nav.tilleggsstonader.sak.migrering.routing.SkjemaRoutingRepository
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.tilBehandlingResult
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Profile("integrasjonstest")
@Service
class TestoppsettService(
    private val fagsakPersonRepository: FagsakPersonRepository,
    private val fagsakRepository: FagsakRepository,
    private val fagsakService: FagsakService,
    private val eksternFagsakIdRepository: EksternFagsakIdRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksternBehandlingIdRepository: EksternBehandlingIdRepository,
    private val faktaGrunnlagService: FaktaGrunnlagService,
    private val vedtakRepository: VedtakRepository,
    private val skjemaRoutingRepository: SkjemaRoutingRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
) {
    fun hentFagsak(fagsakId: FagsakId) = fagsakService.hentFagsak(fagsakId)

    fun hentBehandling(behandlingId: BehandlingId) = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentSaksbehandling(behandlingId: BehandlingId) = behandlingRepository.finnSaksbehandling(behandlingId)

    fun opprettBehandlingMedFagsak(
        behandling: Behandling = behandling(),
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
        opprettGrunnlagsdata: Boolean = true,
        identer: Set<PersonIdent> = defaultIdenter,
    ): Behandling {
        val person = hentEllerOpprettPerson(fagsak(identer = identer))
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

    fun lagre(
        behandling: Behandling,
        opprettGrunnlagsdata: Boolean = true,
    ): Behandling {
        val dbBehandling = behandlingRepository.insert(behandling)
        eksternBehandlingIdRepository.insert(EksternBehandlingId(behandlingId = dbBehandling.id))

        if (opprettGrunnlagsdata) {
            opprettGrunnlagsdata(behandling.id)
        }

        return dbBehandling
    }

    fun oppdater(behandling: Behandling): Behandling = behandlingRepository.update(behandling)

    fun opprettGrunnlagsdata(behandlingId: BehandlingId) {
        faktaGrunnlagService.opprettGrunnlagHvisDetIkkeEksisterer(behandlingId)
    }

    fun lagreFagsak(fagsak: Fagsak): Fagsak {
        val person = hentEllerOpprettPerson(fagsak)
        val fagsak =
            fagsakRepository.insert(
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

    fun lagVedtak(
        behandling: Behandling,
        beregningsresultat: BeregningsresultatTilsynBarn = vedtakBeregningsresultat,
        vedtaksperioder: List<Vedtaksperiode> = emptyList(),
    ): GeneriskVedtak<InnvilgelseTilsynBarn> {
        val vedtak =
            innvilgetVedtak(
                behandlingId = behandling.id,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder,
            )
        vedtakRepository.insert(vedtak)
        return vedtak
    }

    fun ferdigstillBehandling(behandling: Behandling): Behandling {
        val resultat =
            vedtakRepository.findByIdOrNull(behandling.id)?.type?.tilBehandlingResult()
                ?: behandling.resultat
        return oppdater(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = resultat,
                vedtakstidspunkt =
                    when {
                        resultat != BehandlingResultat.IKKE_SATT -> LocalDateTime.now()
                        else -> null
                    },
            ),
        )
    }

    fun opprettRevurdering(
        forrigeBehandling: Behandling,
        fagsak: Fagsak,
        steg: StegType = StegType.BEREGNE_YTELSE,
    ): Behandling =
        lagre(
            behandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                forrigeIverksatteBehandlingId = forrigeBehandling.id,
                status = BehandlingStatus.UTREDES,
                steg = steg,
            ),
        )

    fun lagBehandlingOgRevurdering(): Behandling {
        val fagsak = fagsak()
        lagreFagsak(fagsak)
        val førsteBehandling =
            lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET))
        val revurdering =
            behandling(
                fagsak = fagsak,
                forrigeIverksatteBehandlingId = førsteBehandling.id,
                type = BehandlingType.REVURDERING,
            )
        return lagre(revurdering)
    }

    fun lagreSøknadRouting(skjemaRouting: SkjemaRouting) = skjemaRoutingRepository.insert(skjemaRouting)

    fun hentSøknadRouting(
        ident: String,
        type: Skjematype,
    ) = skjemaRoutingRepository.findByIdentAndType(ident, type)

    fun lagreTotrinnskontroll(totrinnskontroll: Totrinnskontroll): Totrinnskontroll = totrinnskontrollRepository.insert(totrinnskontroll)

    private fun lagreTotrinnskontroll(behandling: Behandling) {
        totrinnskontrollRepository.insert(
            totrinnskontroll(
                status = TotrinnInternStatus.GODKJENT,
                behandlingId = behandling.id,
                beslutter = "beslutter",
            ),
        )
    }

    private fun hentEllerOpprettPerson(fagsak: Fagsak): FagsakPerson =
        fagsakPersonRepository.findByIdOrNull(fagsak.fagsakPersonId)
            ?: hentPersonFraIdenter(fagsak)
            ?: opprettPerson(fagsak)

    private fun hentPersonFraIdenter(fagsak: Fagsak): FagsakPerson? =
        fagsak.personIdenter
            .map { it.ident }
            .takeIf { it.isNotEmpty() }
            ?.let { fagsakPersonRepository.findByIdent(it) }

    private fun opprettPerson(fagsak: Fagsak) =
        fagsakPersonRepository.insert(
            FagsakPerson(
                fagsak.fagsakPersonId,
                identer = fagsak.personIdenter,
            ),
        )
}
