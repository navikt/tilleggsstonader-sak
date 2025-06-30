package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Innvilgelse
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
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
    private val faktaGrunnlagService: FaktaGrunnlagService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val søknadService: SøknadService,
    private val vilkårService: VilkårService,
    private val vedtakService: VedtakService,
) {
    fun lagInterntVedtak(behandlingId: BehandlingId): InterntVedtak {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val stønadstype = behandling.stønadstype
        if (stønadstype !in
            setOf(
                Stønadstype.BARNETILSYN,
                Stønadstype.LÆREMIDLER,
                Stønadstype.BOUTGIFTER,
            )
        ) {
            throw IllegalArgumentException("Internt vedtak håndterer ikke stønadstype=$stønadstype")
        }
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandling.id)
        val vedtak = vedtakService.hentVedtak(behandling.id)

        val grunnlag = faktaGrunnlagService.hentGrunnlagsdata(behandling.id)
        val behandlingbarn = mapBarnPåBarnId(behandling.id, grunnlag.personopplysninger.barn)

        return InterntVedtak(
            behandling = mapBehandlingsinformasjon(behandling),
            søknad = mapSøknadsinformasjon(behandling),
            målgrupper = mapVilkårperioder(vilkårsperioder.målgrupper),
            aktiviteter = mapVilkårperioder(vilkårsperioder.aktiviteter),
            vedtaksperioder = mapVedtaksperioder(vedtak),
            vilkår = mapVilkår(behandling.id, behandlingbarn),
            vedtak = mapVedtak(vedtak),
            beregningsresultat = mapBeregningsresultatForStønadstype(vedtak, behandling),
        )
    }

    private fun mapBeregningsresultatForStønadstype(
        vedtak: Vedtak?,
        behandling: Saksbehandling,
    ): BeregningsresultatInterntVedtakDto? =
        vedtak?.data?.let { data ->
            when (data) {
                is InnvilgelseTilsynBarn ->
                    BeregningsresultatInterntVedtakDto(
                        tilsynBarn = data.beregningsresultat.tilDto(vedtak.tidligsteEndring ?: behandling.revurderFra).perioder,
                    )

                is InnvilgelseLæremidler ->
                    BeregningsresultatInterntVedtakDto(
                        læremidler =
                            data.beregningsresultat
                                .tilDto(vedtak.tidligsteEndring ?: behandling.revurderFra)
                                .perioder,
                    )

                is InnvilgelseBoutgifter ->
                    BeregningsresultatInterntVedtakDto(
                        boutgifter = data.beregningsresultat.tilDto(vedtak.tidligsteEndring ?: behandling.revurderFra).perioder,
                    )

                is Innvilgelse -> error("Mangler mapping av beregningsresultat for ${data.type}")

                else -> null
            }
        }

    private fun mapBarnPåBarnId(
        behandlingId: BehandlingId,
        grunnlag: List<GrunnlagBarn>,
    ): Map<BarnId, GrunnlagBarn> {
        val behandlingbarn = barnService.finnBarnPåBehandling(behandlingId).associateBy { it.ident }
        return grunnlag.associateBy {
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

    private fun mapVedtaksperioder(vedtak: Vedtak?): List<VedtaksperiodeInterntVedtak> =
        when (vedtak?.data) {
            is InnvilgelseTilsynBarn -> mapVedtaksperioderTilsynBarnOgBoutgifter(vedtak.data.vedtaksperioder)

            is InnvilgelseLæremidler -> mapVedtaksperioderLæremidler(vedtak.data.vedtaksperioder)

            is InnvilgelseBoutgifter -> mapVedtaksperioderTilsynBarnOgBoutgifter(vedtak.data.vedtaksperioder)

            is Avslag, is Opphør -> emptyList()

            else -> {
                error("Kan ikke mappe vedtaksperioder for type ${vedtak?.data?.javaClass?.simpleName}")
            }
        }

    private fun mapVedtaksperioderTilsynBarnOgBoutgifter(vedtaksperioder: List<Vedtaksperiode>): List<VedtaksperiodeInterntVedtak> =
        vedtaksperioder.map {
            VedtaksperiodeInterntVedtak(
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
                fom = it.fom,
                tom = it.tom,
            )
        }

    private fun mapVedtaksperioderLæremidler(vedtaksperioder: List<VedtaksperiodeLæremidler>?): List<VedtaksperiodeInterntVedtak> =
        vedtaksperioder?.map {
            VedtaksperiodeInterntVedtak(
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
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

                is VedtakBoutgifter -> mapVedtakBoutgifter(vedtak.data)
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

    private fun mapVedtakBoutgifter(vedtak: VedtakBoutgifter) =
        when (vedtak) {
            is InnvilgelseBoutgifter -> VedtakInnvilgelseInternt(innvilgelseBegrunnelse = vedtak.begrunnelse)

            is AvslagBoutgifter ->
                VedtakAvslagInternt(
                    årsakerAvslag = vedtak.årsaker,
                    avslagBegrunnelse = vedtak.begrunnelse,
                )

            is OpphørBoutgifter ->
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
