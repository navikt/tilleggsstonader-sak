package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TODO: Vurder å ta med fakta
 */
data class InterntVedtak(
    val behandling: Behandlinginfo,
    val søknad: Søknadsinformasjon?,
    val målgrupper: List<Vilkårperiode>,
    val aktiviteter: List<Vilkårperiode>,
    val stønadsperioder: List<Stønadsperiode>,
    val vilkår: List<VilkårInternt>,
    val vedtak: VedtakInternt?,
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
)

data class Søknadsinformasjon(
    val mottattTidspunkt: LocalDateTime,
)

data class VedtakInternt(
    val type: TypeVedtak,
    val årsakerAvslag: List<ÅrsakAvslag>?,
    val avslagBegrunnelse: String?,
    val utgifterBarn: List<UtgiftBarn>?,
)

data class UtgiftBarn(
    val fødselsdatoBarn: LocalDate,
    val utgifter: List<Utgift>,
)

data class Utgift(
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Vilkårperiode(
    val type: VilkårperiodeType,
    val fom: LocalDate,
    val tom: LocalDate,
    val delvilkår: DelvilkårVilkårperiode,
    val kilde: KildeVilkårsperiode,
    val resultat: ResultatVilkårperiode,
    val begrunnelse: String?,
    val slettetKommentar: String?,
    val aktivitetsdager: Int?,
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

data class Stønadsperiode(
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
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
