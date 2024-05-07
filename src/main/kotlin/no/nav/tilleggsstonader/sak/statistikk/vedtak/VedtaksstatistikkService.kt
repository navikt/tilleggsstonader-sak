package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepository: VedtakstatistikkRepository,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val vedtakService: TilsynBarnVedtakService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val behandlingBarnService: BarnService,
    private val iverksettService: IverksettService,
    private val stønadsperiodeService: StønadsperiodeService,

    ) {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: LocalDateTime) {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkårsvurderinger = vilkårService.hentVilkårsett(behandlingId)
        val andelTilkjentYtelse = iverksettService.hentAndelTilkjentYtelse(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val avslagÅrsak = utledAvslagÅrsak(behandlingId)
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)

        vedtaksstatistikkRepository.insert(
            Vedtaksstatistikk(
                fagsakId = fagsakId,
                behandlingId = behandlingId,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                eksternBehandlingId = saksbehandling.eksternId,
                relatertBehandlingId = hentRelatertBehandlingId(behandling),
                adressebeskyttelse = hentAdressebeskyttelse(personIdent),
                tidspunktVedtak = hendelseTidspunkt,
                målgruppe = MålgruppeDvh.fraDomene(vilkårsperioder.målgrupper),
                aktivitet = AktivitetDvh.fraDomene(vilkårsperioder.aktiviteter),
                vilkårsvurderinger = VilkårsvurderingDvh.fraDomene(vilkårsvurderinger),
                person = personIdent,
                barn = BarnDvh.fraDomene(behandlingBarnService.finnBarnPåBehandling(behandlingId)),
                behandlingType = BehandlingTypeDvh.fraDomene(behandling.type),
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandling.årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandling.resultat),
                vedtaksperioder = VedtaksperioderDvh.fraDomene(stønadsperioder),
                utbetalinger = UtbetalingerDvh.fraDomene(andelTilkjentYtelse),
                kravMottatt = behandling.kravMottatt,
                årsakRevurdering = null, // TODO implementer når revurdering er på plass.
                avslagÅrsak = avslagÅrsak,
            ),
        )
    }

    private fun utledAvslagÅrsak(behandlingId: UUID) = vedtakService.hentVedtak(behandlingId).let {
        when (it) {
            is VedtakTilsynBarn -> it.avslagBegrunnelse
            else -> {
                throw NotImplementedError("Vi har bare pass av barn")
            }
        }
    }

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraDomene(
        personService.hentPersonKortBolk(
            listOf(personIdent),
        ).values.single().adressebeskyttelse.gradering(),
    )

    private fun hentRelatertBehandlingId(behandling: Behandling) = behandling.forrigeBehandlingId?.let {
        behandlingService.hentEksternBehandlingId(it)
    }?.id
}
