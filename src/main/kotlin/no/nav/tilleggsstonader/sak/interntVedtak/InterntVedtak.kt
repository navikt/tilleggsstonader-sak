package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatForMånedDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerDto
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.BeregningsresultatForPeriodeDto as BeregningsresultatDtoBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto as BeregningsresultatDtoLæremidler

data class InterntVedtak(
    val behandling: Behandlinginfo,
    val søknad: Søknadsinformasjon?,
    val målgrupper: List<VilkårperiodeInterntVedtak>,
    val aktiviteter: List<VilkårperiodeInterntVedtak>,
    val vedtaksperioder: List<VedtaksperiodeInterntVedtak>,
    val vilkår: List<VilkårInternt>,
    val vedtak: VedtakInternt?,
    val beregningsresultat: BeregningsresultatInterntVedtakDto?,
)

data class BeregningsresultatInterntVedtakDto(
    val tilsynBarn: List<BeregningsresultatForMånedDto>? = null,
    val læremidler: List<BeregningsresultatDtoLæremidler>? = null,
    val boutgifter: List<BeregningsresultatDtoBoutgifter>? = null,
)

data class Behandlinginfo(
    val behandlingId: BehandlingId,
    val eksternFagsakId: Long,
    val stønadstype: Stønadstype,
    val årsak: BehandlingÅrsak,
    val ident: String,
    val opprettetTidspunkt: LocalDateTime,
    val resultat: BehandlingResultat,
    val vedtakstidspunkt: LocalDateTime,
    val saksbehandler: String,
    val beslutter: String?,
    val revurderFra: LocalDate?,
)

data class Søknadsinformasjon(
    val mottattTidspunkt: LocalDateTime,
)

sealed class VedtakInternt(
    val type: TypeVedtak,
)

data class VedtakInnvilgelseInternt(
    val innvilgelseBegrunnelse: String?,
) : VedtakInternt(TypeVedtak.INNVILGELSE)

data class VedtakAvslagInternt(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val avslagBegrunnelse: String,
) : VedtakInternt(TypeVedtak.AVSLAG)

data class VedtakOpphørInternt(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val opphørBegrunnelse: String,
) : VedtakInternt(TypeVedtak.OPPHØR)

data class Utgift(
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class VilkårperiodeInterntVedtak(
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val faktaOgVurderinger: FaktaOgVurderingerDto,
    val kilde: KildeVilkårsperiode,
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val slettetKommentar: String?,
)

data class DelvilkårVilkårperiode(
    val medlemskap: VurderingVilkårperiode?,
    val dekketAvAnnetRegelverk: VurderingVilkårperiode?,
    val lønnet: VurderingVilkårperiode?,
)

data class VurderingVilkårperiode(
    val svar: String?,
    val resultat: ResultatDelvilkårperiode,
)

data class VilkårInternt(
    val type: VilkårType,
    val resultat: Vilkårsresultat,
    val delvilkår: List<DelvilkårInternt>,
    val fødselsdatoBarn: LocalDate?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val utgift: Int?,
)

data class DelvilkårInternt(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingInternt>,
)

data class VurderingInternt(
    val regel: String,
    val svar: String?,
    val begrunnelse: String?,
)

data class VedtaksperiodeInterntVedtak(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
)
