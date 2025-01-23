package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
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


//For tilsyn barn:
//Vedtaksperioder kan finnes med å summere opp betalingsperioder fra og til
//Målgruppe og aktivitet ligger på vedtaksperiodeDto som ligger på beregnigsresultatDto
//Antall barn ligger på vedtaksperiodeDto
//Faktisk barn Fnr ligger på utgifter og må legges til i betalingsperioder etc. for å kunne brukes

//Ligger nok likk jobb i å skrive om til vedtaksperiode dto funksjoner til å kunne gjenbrukes


//For læremidler:
//Fom og tom ligger på Beregningsgrunnlag
//Målgruppe ligger på beregningsgrunnlag
//Aktivitet må legges til på beregningsgrunnlag (bør være greit)
//Sats ligger på beregningsgrunnlag

@Service
class VedtaksstatistikkService(
    private val vedtaksstatistikkRepository: VedtakstatistikkRepository,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val behandlingBarnService: BarnService,
    private val iverksettService: IverksettService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vedtakService: VedtakService,

) {
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

    private fun hentAdressebeskyttelse(personIdent: String) = AdressebeskyttelseDvh.fraDomene(
        personService.hentPersonKortBolk(
            listOf(personIdent),
        ).values.single().adressebeskyttelse.gradering(),
    )

    private fun hentRelatertBehandlingId(behandling: Saksbehandling) = behandling.forrigeBehandlingId?.let {
        behandlingService.hentEksternBehandlingId(it)
    }?.id
}
