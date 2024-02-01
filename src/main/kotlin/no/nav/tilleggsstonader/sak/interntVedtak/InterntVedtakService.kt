package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode as DelvilkårVilkårperiodeDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode as VilkårperiodeDomain

@Service
class InterntVedtakService(
    private val behandlingService: BehandlingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val søknadService: SøknadService,
) {

    fun lagInterntVedtak(behandlingId: UUID): InterntVedtak {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        return InterntVedtak(
            behandling = mapBehandlingsinformasjon(behandling),
            søknad = mapSøknadsinformasjon(behandling),
            målgrupper = mapVilkårperioder(vilkårsperioder.målgrupper),
            aktiviteter = mapVilkårperioder(vilkårsperioder.aktiviteter),
            stønadsperioder = mapStønadsperioder(behandlingId),
        )
    }

    private fun mapBehandlingsinformasjon(
        behandling: Saksbehandling,
    ): Behandlinginfo {
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontroll(behandling.id)
        val saksbehandler = totrinnskontroll?.saksbehandler ?: behandling.endretAv
        return Behandlinginfo(
            behandlingId = behandling.id,
            eksternFagsakId = behandling.eksternFagsakId,
            stønadstype = behandling.stønadstype,
            årsak = behandling.årsak,
            ident = behandling.ident,
            opprettetTidspunkt = behandling.opprettetTid,
            resultat = behandling.resultat,
            vedtakstidspunkt = behandling.vedtakstidspunkt ?: error("Finner ikke vedtakstidspunkt"),
            saksbehandler = saksbehandler,
            beslutter = totrinnskontroll?.beslutter,
        )
    }

    private fun mapSøknadsinformasjon(
        behandling: Saksbehandling,
    ): Søknadsinformasjon? {
        val søknad = when (behandling.stønadstype) {
            Stønadstype.BARNETILSYN -> søknadService.hentSøknadBarnetilsyn(behandling.id)
        }
        if (søknad == null) {
            return null
        }
        return Søknadsinformasjon(
            mottattTidspunkt = søknad.mottattTidspunkt,
        )
    }

    private fun mapVilkårperioder(vilkårperioder: List<VilkårperiodeDomain>): List<Vilkårperiode> {
        return vilkårperioder.map {
            Vilkårperiode(
                type = it.type,
                fom = it.fom,
                tom = it.tom,
                delvilkår = mapDelvilkår(it.delvilkår),
                kilde = it.kilde,
                resultat = it.resultat,
                begrunnelse = it.begrunnelse,
            )
        }
    }

    private fun mapDelvilkår(delvilkår: DelvilkårVilkårperiodeDomain): DelvilkårVilkårperiode {
        val medlemskap = if (delvilkår is DelvilkårMålgruppe) mapVurdering(delvilkår.medlemskap) else null
        val lønnet = if (delvilkår is DelvilkårAktivitet) mapVurdering(delvilkår.lønnet) else null
        val mottarSykepenger = if (delvilkår is DelvilkårAktivitet) mapVurdering(delvilkår.mottarSykepenger) else null
        return DelvilkårVilkårperiode(
            medlemskap = medlemskap,
            lønnet = lønnet,
            mottarSykepenger = mottarSykepenger,
        )
    }

    private fun mapVurdering(vurdering: Vurdering?): VurderingVilkårperiode? {
        if (vurdering == null) {
            return null
        }
        return VurderingVilkårperiode(
            svar = vurdering.svar?.name,
            resultat = vurdering.resultat,
            begrunnelse = vurdering.begrunnelse,
        )
    }

    private fun mapStønadsperioder(behandlingId: UUID): List<Stønadsperiode> {
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandlingId)
        return stønadsperioder.map {
            Stønadsperiode(
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
                fom = it.fom,
                tom = it.tom,
            )
        }
    }
}
