package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepository: error.NonExistentClass,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val vedtakService: TilsynBarnVedtakService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val behandlingBarnService: BarnService,
    private val iverksettService: IverksettService,


    ) {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: LocalDateTime) {

        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        val vilkårsvurderinger = vilkårService.hentVilkårsett(behandlingId)



        vedtaksstatistikkRepository.lagreVedtaksstatistikk(
            VedtaksstatistikkDvh(
                fagsakId = fagsakId,
                behandlingId = behandlingId,
                eksternBehandlingId = behandlingService.hentEksternBehandlingId(behandlingId).id,
                relatertBehandlingId = hentRelatertBehandlingId(behandlingId),
                adressebeskyttelse = hentAdressebeskyttelse(personIdent),
                tidspunktVedtak = hendelseTidspunkt,
                målgruppe = MålgruppeDvh.fraDomene(vilkårsperioder.målgrupper),
                aktivitet = AktivitetDvh.fraDomene(vilkårsperioder.aktiviteter),
                vilkårsvurderinger = vilkårsvurderinger.map {
                    VilkårsvurderingDvh.fraDomene(
                        resultat = it.resultat,
                        delvilkår = it.delvilkårsett,
                    )
                },
                person = personIdent,
                barn = BarnDvh.fraDomene(behandlingBarnService.finnBarnPåBehandling(behandlingId)) ,
                behandlingType = BehandlingTypeDvh.FØRSTEGANGSBEHANDLING, //TODO legge til revurdering når den er klar
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandlingService.hentBehandling(behandlingId).årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandlingService.hentBehandling(behandlingId).resultat),
                vedtaksperioder = TODO(),
            )

    }

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraDomene(
        personService.hentPersonKortBolk(
            listOf(personIdent)
        ).values.single().adressebeskyttelse.gradering()
    )

    private fun hentRelatertBehandlingId(behandlingId: UUID) =
        behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId?.let {
            behandlingService.hentEksternBehandlingId(it)
        }?.id

}
