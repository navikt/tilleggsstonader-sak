package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AdressebeskyttelseDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.BehandlingÅrsakDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.StønadstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtakResultatDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvhV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepository: VedtaksstatistikkRepository,
    private val vedtaksstatistikkRepositoryV2: VedtaksstatistikkRepositoryV2,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val behandlingBarnService: BarnService,
    private val iverksettService: IverksettService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vedtakService: VedtakService,
    private val barnRepository: BarnRepository,

) {
    @Deprecated(message = "Slettes når team Spenn og Familie har tatt i bruk VedtaksstatstikkV2")
    fun lagreVedtaksstatistikk(behandlingId: BehandlingId, fagsakId: FagsakId, hendelseTidspunkt: LocalDateTime) {
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkårsvurderinger = vilkårService.hentVilkårsett(behandlingId)
        val andelTilkjentYtelse = iverksettService.hentAndelTilkjentYtelse(behandlingId)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)

        vedtaksstatistikkRepository.insert(
            Vedtaksstatistikk(
                fagsakId = fagsakId,
                stønadstype = StønadstypeDvh.fraDomene(behandling.stønadstype),
                behandlingId = behandlingId,
                eksternFagsakId = behandling.eksternFagsakId,
                eksternBehandlingId = behandling.eksternId,
                relatertBehandlingId = hentRelatertBehandlingId(behandling),
                adressebeskyttelse = hentAdressebeskyttelse(personIdent),
                tidspunktVedtak = hendelseTidspunkt,
                målgrupper = MålgrupperDvh.fraDomene(vilkårsperioder.målgrupper),
                aktiviteter = AktiviteterDvh.fraDomene(vilkårsperioder.aktiviteter),
                vilkårsvurderinger = VilkårsvurderingerDvh.fraDomene(vilkårsvurderinger),
                person = personIdent,
                barn = BarnDvh.fraDomene(behandlingBarnService.finnBarnPåBehandling(behandlingId)),
                behandlingType = BehandlingTypeDvh.fraDomene(behandling.type),
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandling.årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandling.resultat),
                vedtaksperioder = VedtaksperioderDvh.fraDomene(stønadsperioder),
                utbetalinger = UtbetalingerDvh.fraDomene(andelTilkjentYtelse),
                kravMottatt = behandling.kravMottatt,
                årsakerAvslag = ÅrsakAvslagDvh.fraDomene(vedtak?.takeIfType<Avslag>()?.data?.årsaker),
                årsakerOpphør = ÅrsakOpphørDvh.fraDomene(vedtak?.takeIfType<Opphør>()?.data?.årsaker),
            ),
        )
    }

    fun lagreVedtaksstatistikkV2(behandlingId: BehandlingId, fagsakId: FagsakId) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)
            ?: throw IllegalStateException("Kan ikke sende vedtaksstatistikk uten vedtak")
        val vedtakstidspunkt = behandling.vedtakstidspunkt
            ?: throw IllegalStateException("Behandlingen må ha et vedtakstidspunkt for å sende vedtaksstatistikk")
        val søkerIdent = behandlingService.hentAktivIdent(behandlingId)
        val andelTilkjentYtelse = iverksettService.hentAndelTilkjentYtelse(behandlingId)
        val vilkår = vilkårService.hentOppfyltePassBarnVilkår(behandlingId)
        val barn = barnRepository.findByBehandlingId(behandlingId)

        vedtaksstatistikkRepositoryV2.insert(
            VedtaksstatistikkV2(
                fagsakId = fagsakId,
                stønadstype = StønadstypeDvh.fraDomene(behandling.stønadstype),
                behandlingId = behandlingId,
                eksternFagsakId = behandling.eksternFagsakId,
                eksternBehandlingId = behandling.eksternId,
                relatertBehandlingId = hentRelatertBehandlingId(behandling),
                adressebeskyttelse = hentAdressebeskyttelse(søkerIdent),
                tidspunktVedtak = vedtakstidspunkt,
                søkerIdent = søkerIdent,
                behandlingType = BehandlingTypeDvh.fraDomene(behandling.type),
                behandlingÅrsak = BehandlingÅrsakDvh.fraDomene(behandling.årsak),
                vedtakResultat = VedtakResultatDvh.fraDomene(behandling.resultat),
                vedtaksperioder = VedtaksperioderDvhV2.fraDomene(vedtak, vilkår, barn),
                utbetalinger = UtbetalingerDvhV2.fraDomene(andelTilkjentYtelse, vedtak),
                kravMottatt = behandling.kravMottatt,
                årsakerAvslag = ÅrsakAvslagDvh.fraDomene(vedtak.takeIfType<Avslag>()?.data?.årsaker),
                årsakerOpphør = ÅrsakOpphørDvh.fraDomene(vedtak.takeIfType<Opphør>()?.data?.årsaker),
            ),
        )
    }

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraDomene(
        personService.hentPersonKortBolk(
            listOf(personIdent),
        ).values.single().adressebeskyttelse.gradering(),
    )

    private fun hentRelatertBehandlingId(behandling: Saksbehandling) = behandling.forrigeBehandlingId?.let {
        behandlingService.hentEksternBehandlingId(it)
    }?.id
}
