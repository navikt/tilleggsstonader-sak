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
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilFaktaOgVurderingDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode as VedtaksperiodeLæremidler

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
        validerStønadstype(behandling.stønadstype)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandling.id)
        val vedtak = vedtakService.hentVedtak(behandling.id)

        val grunnlag = grunnlagsdataService.hentGrunnlagsdata(behandling.id).grunnlag
        val behandlingbarn = mapBarnPåBarnId(behandling.id, grunnlag)

        return InterntVedtak(
            behandling = mapBehandlingsinformasjon(behandling),
            søknad = mapSøknadsinformasjon(behandling),
            målgrupper = mapVilkårperioder(vilkårsperioder.målgrupper),
            aktiviteter = mapVilkårperioder(vilkårsperioder.aktiviteter),
            stønadsperioder = mapStønadsperioder(behandling.id),
            vedtaksperioder = mapVedtaksperioder(vedtak),
            vilkår = mapVilkår(behandling.id, behandlingbarn),
            vedtak = mapVedtak(vedtak),
            beregningsresultat = mapBeregningsresultatForStønadstype(vedtak, behandling),
        )
    }

    private fun validerStønadstype(stønadstype: Stønadstype) {
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> {}
            Stønadstype.LÆREMIDLER -> {}
            else -> error("Internt vedtak håndterer ikke stønadstype=$stønadstype ennå")
        }
    }

    private fun mapBeregningsresultatForStønadstype(
        vedtak: Vedtak?,
        behandling: Saksbehandling,
    ): BeregningsresultatInterntVedtakDto? =
        vedtak?.data?.let { data ->
            when (data) {
                is InnvilgelseTilsynBarn ->
                    BeregningsresultatInterntVedtakDto(
                        tilsynBarn = data.beregningsresultat.tilDto(behandling.revurderFra).perioder,
                    )

                is InnvilgelseLæremidler ->
                    BeregningsresultatInterntVedtakDto(
                        læremidler =
                            data.beregningsresultat
                                .tilDto(behandling.revurderFra)
                                .perioder,
                    )

                else -> null
            }
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

    private fun mapBehandlingsinformasjon(behandling: Saksbehandling): Behandlinginfo {
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

    private fun mapSøknadsinformasjon(behandling: Saksbehandling): Søknadsinformasjon? {
        val søknad = søknadService.hentSøknadMetadata(behandling.id)
        return søknad?.let {
            Søknadsinformasjon(
                mottattTidspunkt = it.mottattTidspunkt,
            )
        }
    }

    private fun mapVilkårperioder(vilkårperioder: List<Vilkårperiode>): List<VilkårperiodeInterntVedtak> =
        vilkårperioder.map {
            VilkårperiodeInterntVedtak(
                type = it.type,
                fom = it.fom,
                tom = it.tom,
                faktaOgVurderinger = it.faktaOgVurdering.tilFaktaOgVurderingDto(),
                kilde = it.kilde,
                resultat = it.resultat,
                begrunnelse = it.begrunnelse,
                slettetKommentar = it.slettetKommentar,
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

    private fun mapVedtaksperioder(vedtak: Vedtak?): List<VedtaksperiodeInterntVedtak> =
        when (vedtak?.data) {
            is InnvilgelseTilsynBarn -> mapVedtaksperioderTilsynBarn(vedtak.data.vedtaksperioder)

            is InnvilgelseLæremidler -> mapVedtaksperioderLæremidler(vedtak.data.vedtaksperioder)

            is Avslag, is Opphør -> emptyList()

            else -> {
                error("Kan ikke mappe vedtaksperioder for type ${vedtak?.data?.javaClass?.simpleName}")
            }
        }

    private fun mapVedtaksperioderTilsynBarn(vedtaksperioder: List<Vedtaksperiode>?): List<VedtaksperiodeInterntVedtak> =
        vedtaksperioder?.map {
            VedtaksperiodeInterntVedtak(
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
                fom = it.fom,
                tom = it.tom,
            )
        } ?: emptyList()

    private fun mapVedtaksperioderLæremidler(vedtaksperioder: List<VedtaksperiodeLæremidler>?): List<VedtaksperiodeInterntVedtak> =
        vedtaksperioder?.map {
            VedtaksperiodeInterntVedtak(
                målgruppe = null,
                aktivitet = null,
                fom = it.fom,
                tom = it.tom,
            )
        } ?: emptyList()

    private fun mapVilkår(
        behandlingId: BehandlingId,
        behandlingBarn: Map<BarnId, GrunnlagBarn>,
    ): List<VilkårInternt> =
        vilkårService
            .hentVilkår(behandlingId)
            .map { vilkår ->
                VilkårInternt(
                    type = vilkår.type,
                    resultat = vilkår.resultat,
                    fødselsdatoBarn = vilkår.barnId?.let { behandlingBarn.finnFødselsdato(it) },
                    delvilkår = vilkår.delvilkårsett.map { mapDelvilkår(it) },
                    fom = vilkår.fom,
                    tom = vilkår.tom,
                    utgift = vilkår.utgift,
                )
            }.sortedWith(compareBy<VilkårInternt> { it.type }.thenBy { it.fødselsdatoBarn }.thenBy { it.fom })

    private fun mapDelvilkår(delvilkår: Delvilkår) =
        DelvilkårInternt(
            resultat = delvilkår.resultat,
            vurderinger =
                delvilkår.vurderinger.map { vurdering ->
                    VurderingInternt(
                        regel = vurdering.regelId.beskrivelse,
                        svar = vurdering.svar?.beskrivelse,
                        begrunnelse = vurdering.begrunnelse,
                    )
                },
        )

    private fun mapVedtak(vedtak: Vedtak?): VedtakInternt? =
        vedtak?.let {
            when (vedtak.data) {
                is VedtakTilsynBarn -> mapVedtakTilsynBarn(vedtak.data)

                is VedtakLæremidler -> mapVedtakLæremidler(vedtak.data)
            }
        }

    private fun mapVedtakTilsynBarn(vedtak: VedtakTilsynBarn) =
        when (vedtak) {
            is InnvilgelseTilsynBarn -> VedtakInnvilgelseInternt(innvilgelseBegrunnelse = vedtak.begrunnelse)

            is AvslagTilsynBarn ->
                VedtakAvslagInternt(
                    årsakerAvslag = vedtak.årsaker,
                    avslagBegrunnelse = vedtak.begrunnelse,
                )

            is OpphørTilsynBarn ->
                VedtakOpphørInternt(
                    årsakerOpphør = vedtak.årsaker,
                    opphørBegrunnelse = vedtak.begrunnelse,
                )
        }

    private fun mapVedtakLæremidler(vedtak: VedtakLæremidler) =
        when (vedtak) {
            is InnvilgelseLæremidler -> VedtakInnvilgelseInternt(innvilgelseBegrunnelse = vedtak.begrunnelse)

            is AvslagLæremidler ->
                VedtakAvslagInternt(
                    årsakerAvslag = vedtak.årsaker,
                    avslagBegrunnelse = vedtak.begrunnelse,
                )

            is OpphørLæremidler ->
                VedtakOpphørInternt(
                    årsakerOpphør = vedtak.årsaker,
                    opphørBegrunnelse = vedtak.begrunnelse,
                )
        }

    private fun Map<BarnId, GrunnlagBarn>.finnFødselsdato(barnId: BarnId): LocalDate {
        val barn = this[barnId] ?: error("Finner ikke barn=$barnId")
        return barn.fødselsdato ?: error("Mangler fødselsdato for barn=$barnId")
    }
}
