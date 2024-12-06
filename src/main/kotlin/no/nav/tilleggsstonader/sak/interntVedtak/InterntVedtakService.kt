package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering
import org.springframework.stereotype.Service
import java.time.LocalDate

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
    private val vedtakService: VedtakService,
) {

    fun lagInterntVedtak(behandlingId: BehandlingId): InterntVedtak {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandling.id)
        val vedtak = vedtakService.hentVedtak(behandling.id)

        return when (behandling.stønadstype) {
            Stønadstype.BARNETILSYN -> lagInterntVedtakTilsynBarn(behandling, vilkårsperioder, vedtak)
            Stønadstype.LÆREMIDLER -> TODO()
        }
    }

    fun lagInterntVedtakTilsynBarn(
        behandling: Saksbehandling,
        vilkårperioder: Vilkårperioder,
        vedtak: Vedtak?,
    ): InterntVedtak {
        val grunnlag = grunnlagsdataService.hentGrunnlagsdata(behandling.id).grunnlag
        val behandlingbarn = mapBarnPåBarnId(behandling.id, grunnlag)
        return InterntVedtak(
            behandling = mapBehandlingsinformasjon(behandling),
            søknad = mapSøknadsinformasjon(behandling),
            målgrupper = mapVilkårperioder(vilkårperioder.målgrupper), // TODO læremidler
            aktiviteter = mapVilkårperioder(vilkårperioder.aktiviteter), // TODO læremidler
            stønadsperioder = mapStønadsperioder(behandling.id),
            vilkår = mapVilkår(behandling.id, behandlingbarn), // TODO læremidler
            vedtak = mapVedtak(vedtak),
        )
    }

    private fun mapBarnPåBarnId(
        behandlingId: BehandlingId,
        grunnlag: Grunnlag,
    ): Map<BarnId, GrunnlagBarn> {
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
            revurderFra = behandling.revurderFra,
        )
    }

    private fun mapSøknadsinformasjon(
        behandling: Saksbehandling,
    ): Søknadsinformasjon? {
        val søknad = søknadService.hentSøknadMetadata(behandling.id)
        return søknad?.let {
            Søknadsinformasjon(
                mottattTidspunkt = it.mottattTidspunkt,
            )
        }
    }

    private fun mapVilkårperioder(vilkårperioder: List<Vilkårperiode>): List<VilkårperiodeInterntVedtak> {
        return vilkårperioder.map {
            VilkårperiodeInterntVedtak(
                type = it.type,
                fom = it.fom,
                tom = it.tom,
                delvilkår = mapDelvilkår(it.faktaOgVurdering),
                kilde = it.kilde,
                resultat = it.resultat,
                begrunnelse = it.begrunnelse,
                slettetKommentar = it.slettetKommentar,
                aktivitetsdager = it.faktaOgVurdering.fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
            )
        }
    }

    private fun mapDelvilkår(faktaOgVurdering: FaktaOgVurdering): DelvilkårVilkårperiode {
        val vurderinger = faktaOgVurdering.vurderinger
        return DelvilkårVilkårperiode(
            medlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.let { mapVurdering(it) },
            dekketAvAnnetRegelverk = vurderinger.takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()
                ?.dekketAvAnnetRegelverk?.let {
                    mapVurdering(it)
                },
            lønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.let { mapVurdering(it) },
        )
    }

    private fun mapVurdering(vurdering: Vurdering?): VurderingVilkårperiode? {
        if (vurdering == null || vurdering.resultat == ResultatDelvilkårperiode.IKKE_AKTUELT) {
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

    private fun mapVilkår(behandlingId: BehandlingId, behandlingBarn: Map<BarnId, GrunnlagBarn>): List<VilkårInternt> {
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

    private fun mapVedtak(vedtak: Vedtak?): VedtakInternt? {
        return vedtak?.let {
            when (vedtak.data) {
                is VedtakTilsynBarn -> mapVedtakTilsynBarn(vedtak.data)

                is VedtakLæremidler -> mapVedtakLæremidler(vedtak.data)
            }
        }
    }

    private fun mapVedtakTilsynBarn(
        vedtak: VedtakTilsynBarn,
    ) = when (vedtak) {
        is InnvilgelseTilsynBarn -> VedtakInnvilgelseInternt
        is AvslagTilsynBarn -> VedtakAvslagInternt(
            årsakerAvslag = vedtak.årsaker,
            avslagBegrunnelse = vedtak.begrunnelse,
        )

        is OpphørTilsynBarn -> VedtakOpphørInternt(
            årsakerOpphør = vedtak.årsaker,
            opphørBegrunnelse = vedtak.begrunnelse,
        )
    }

    private fun mapVedtakLæremidler(vedtak: VedtakLæremidler) = when (vedtak) {
        is AvslagLæremidler -> VedtakAvslagInternt(
            årsakerAvslag = vedtak.årsaker,
            avslagBegrunnelse = vedtak.begrunnelse,
        )
    }

    private fun Map<BarnId, GrunnlagBarn>.finnFødselsdato(barnId: BarnId): LocalDate {
        val barn = this[barnId] ?: error("Finner ikke barn=$barnId")
        return barn.fødselsdato ?: error("Mangler fødselsdato for barn=$barnId")
    }
}
