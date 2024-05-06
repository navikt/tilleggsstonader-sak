package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakService
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

) {
    fun lagreVedtaksstatistikk(behandlingId: UUID, fagsakId: UUID, hendelseTidspunkt: LocalDateTime) {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkårsvurderinger = vilkårService.hentVilkårsett(behandlingId)
        val andelTilkjentYtelse = iverksettService.hentAndelTilkjentYtelse(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

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
                barn = BarnDvh.fraDomene(behandlingBarnService.finnBarnPåBehandling(behandlingId)),
                behandlingType = BehandlingTypeDvh.FØRSTEGANGSBEHANDLING, // TODO legge til revurdering når den er klar
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandling.årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandling.resultat),
                vedtaksperioder = VedtaksperiodeDvh.fraDomene(andelTilkjentYtelse),
                utbetalinger = UtbetalingDvh.fraDomene(andelTilkjentYtelse),
                kravMottatt = behandling.kravMottatt,
                årsakRevurdering = null, // TODO implementer når revurdering er på plass.
                avslagÅrsak = null, // TODO implementert når avslag er satt opp i saksbehandling
            ),
        )
    }

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraDomene(
        personService.hentPersonKortBolk(
            listOf(personIdent),
        ).values.single().adressebeskyttelse.gradering(),
    )

    private fun hentRelatertBehandlingId(behandlingId: UUID) =
        behandlingService.hentSaksbehandling(behandlingId).forrigeBehandlingId?.let {
            behandlingService.hentEksternBehandlingId(it)
        }?.id
}
