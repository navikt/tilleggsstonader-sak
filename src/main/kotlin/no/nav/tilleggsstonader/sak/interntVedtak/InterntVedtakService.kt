package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode as DelvilkårVilkårperiodeDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode as VilkårperiodeDomain

@Service
class InterntVedtakService(
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val stønadsperiodeService: StønadsperiodeService,
    private val søknadService: SøknadService,
    private val vilkårService: VilkårService,
    private val tilsynBarnVedtakService: TilsynBarnVedtakService,
) {

    fun lagInterntVedtak(behandlingId: BehandlingId): InterntVedtak {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val grunnlag = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlag
        val behandlingbarn = mapBarnPåBarnId(behandlingId, grunnlag)
        val vedtak = tilsynBarnVedtakService.hentVedtak(behandlingId)
        return InterntVedtak(
            behandling = mapBehandlingsinformasjon(behandling),
            søknad = mapSøknadsinformasjon(behandling),
            målgrupper = mapVilkårperioder(vilkårsperioder.målgrupper),
            aktiviteter = mapVilkårperioder(vilkårsperioder.aktiviteter),
            stønadsperioder = mapStønadsperioder(behandlingId),
            vilkår = mapVilkår(behandlingId, behandlingbarn),
            vedtak = mapVedtak(vedtak, behandlingbarn),
        )
    }

    private fun mapBarnPåBarnId(
        behandlingId: BehandlingId,
        grunnlag: Grunnlag,
    ): Map<UUID, GrunnlagBarn> {
        val behandlingbarn = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.ident }
        return grunnlag.barn.associateBy {
            val barn =
                behandlingbarn[it.ident] ?: error("Finner ikke barn med ident=${it.ident} på behandling=$behandlingId")
            barn.id
        }
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
            else -> error("Kan ikke hente søknad for stønadstype ${behandling.stønadstype}")
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
                slettetKommentar = it.slettetKommentar,
                aktivitetsdager = it.aktivitetsdager,
            )
        }
    }

    private fun mapDelvilkår(delvilkår: DelvilkårVilkårperiodeDomain): DelvilkårVilkårperiode {
        val medlemskap = if (delvilkår is DelvilkårMålgruppe) mapVurdering(delvilkår.medlemskap) else null
        val dekketAvAnnetRegelverk =
            if (delvilkår is DelvilkårMålgruppe) mapVurdering(delvilkår.dekketAvAnnetRegelverk) else null
        val lønnet = if (delvilkår is DelvilkårAktivitet) mapVurdering(delvilkår.lønnet) else null
        return DelvilkårVilkårperiode(
            medlemskap = medlemskap,
            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
            lønnet = lønnet,
        )
    }

    private fun mapVurdering(vurdering: Vurdering?): VurderingVilkårperiode? {
        if (vurdering == null) {
            return null
        }
        return VurderingVilkårperiode(
            svar = vurdering.svar?.name,
            resultat = vurdering.resultat,
        )
    }

    private fun mapStønadsperioder(behandlingId: BehandlingId): List<Stønadsperiode> {
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

    private fun mapVilkår(behandlingId: BehandlingId, behandlingBarn: Map<UUID, GrunnlagBarn>): List<VilkårInternt> {
        return vilkårService.hentVilkårsett(behandlingId)
            .map { vilkår ->
                VilkårInternt(
                    type = vilkår.vilkårType,
                    resultat = vilkår.resultat,
                    fødselsdatoBarn = vilkår.barnId?.let { behandlingBarn.finnFødselsdato(it) },
                    delvilkår = vilkår.delvilkårsett.map { mapDelvilkår(it) },
                    fom = vilkår.fom,
                    tom = vilkår.tom,
                    utgift = vilkår.utgift,
                )
            }
            .sortedWith(compareBy<VilkårInternt> { it.type }.thenBy { it.fødselsdatoBarn }.thenBy { it.fom })
    }

    private fun mapDelvilkår(delvilkår: DelvilkårDto) =
        DelvilkårInternt(
            resultat = delvilkår.resultat,
            vurderinger = delvilkår.vurderinger.map { vurdering ->
                VurderingInternt(
                    regel = vurdering.regelId.beskrivelse,
                    svar = vurdering.svar?.beskrivelse,
                    begrunnelse = vurdering.begrunnelse,
                )
            },
        )

    private fun mapVedtak(vedtak: VedtakTilsynBarn?, behandlingbarn: Map<UUID, GrunnlagBarn>): VedtakInternt? {
        return vedtak?.let {
            VedtakInternt(
                type = it.type,
                årsakerAvslag = it.årsakerAvslag?.årsaker,
                avslagBegrunnelse = it.avslagBegrunnelse,
                utgifterBarn = it.vedtak?.utgifter?.entries?.map { (barnId, utgifter) ->
                    UtgiftBarn(
                        fødselsdatoBarn = behandlingbarn.finnFødselsdato(barnId),
                        utgifter = utgifter.map {
                            Utgift(
                                beløp = it.utgift,
                                fom = it.fom.atDay(1),
                                tom = it.fom.atEndOfMonth(),
                            )
                        },
                    )
                },
            )
        }
    }

    private fun Map<UUID, GrunnlagBarn>.finnFødselsdato(barnId: UUID): LocalDate {
        val barn = this[barnId] ?: error("Finner ikke barn=$barnId")
        return barn.fødselsdato ?: error("Mangler fødselsdato for barn=$barnId")
    }
}
