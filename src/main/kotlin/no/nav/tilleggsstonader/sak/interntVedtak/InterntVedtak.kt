package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.OppsummertBeregningForReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatOffentligTransportDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytting
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.BeregningsresultatForMånedDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.BeregningsresultatReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerDto
import java.math.BigDecimal
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
    val vilkår: List<VilkårInterntVedtak>,
    val vedtak: VedtakInternt?,
    val beregningsresultat: BeregningsresultatInterntVedtakDto?,
    val rammevedtakPrivatBil: RammevedtakPrivatBil?,
)

data class BeregningsresultatInterntVedtakDto(
    // TODO TilsynBarn bør være PassAvBarn, men dette brukes eksternt
    val tilsynBarn: List<BeregningsresultatForMånedDto>? = null,
    val læremidler: List<BeregningsresultatDtoLæremidler>? = null,
    val boutgifter: List<BeregningsresultatDtoBoutgifter>? = null,
    val dagligReise: BeregningsresultatDagligReiseInterntVedtak? = null,
    val reiseTilSamling: BeregningsresultatReiseTilSamlingDto? = null,
    val flytting: BeregningsresultatFlyttingInterntVedtak? = null,
)

data class BeregningsresultatFlyttingInterntVedtak(
    val resultater: List<BeregningsresultatFlyttevilkårInterntVedtak>,
)

data class BeregningsresultatFlyttevilkårInterntVedtak(
    val fraTidligereVedtak: Boolean,
    val fom: LocalDate,
    val tom: LocalDate,
    val grunnlag: BeregningsgrunnlagFlytting,
    val beløp: BigDecimal,
    val målgruppe: FaktiskMålgruppe,
)

data class BeregningsresultatDagligReiseInterntVedtak(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val privatBil: List<OppsummertBeregningForReiseDto>? = null,
    val beregningsplan: Beregningsplan,
    val tidligsteEndring: LocalDate? = beregningsplan.legacyTidligsteEndring(),
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
    val tiltaksvariant: String?,
)

data class VilkårInterntVedtak(
    val type: VilkårType,
    val resultat: Vilkårsresultat,
    val delvilkår: List<DelvilkårInterntVedtak>,
    val fødselsdatoBarn: LocalDate?,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val utgift: Int?,
    val slettetKommentar: String?,
    val fakta: VilkårFaktaInterntVedtak?,
)

data class DelvilkårInterntVedtak(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingInterntVedtak>,
)

sealed interface VilkårFaktaInterntVedtak {
    val type: TypeVilkårFakta
}

sealed interface ReiseVilkårFaktaInterntVedtak : VilkårFaktaInterntVedtak {
    val reiseId: ReiseId
    val adresse: String?
}

data class VilkårReiseFaktaUbestemtInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.DAGLIG_REISE_UBESTEMT
}

data class VilkårFaktaOffentligTransportInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.DAGLIG_REISE_OFFENTLIG_TRANSPORT
}

data class VilkårFaktaPrivatBilInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reiseavstandEnVei: BigDecimal,
    val faktaDelperioder: List<FaktaDelperiodePrivatBil>,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.DAGLIG_REISE_PRIVAT_BIL
}

data class VilkårFaktaReiseTilSamlingUbestemtInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.REISE_TIL_SAMLING_UBESTEMT
}

data class VilkårFaktaReiseTilSamlingOffentligTransportInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val nødvendigUtgift: NødvendigUtgift,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String?,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT
}

data class VilkårFaktaReiseTilSamlingDrosjeInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val nødvendigUtgift: NødvendigUtgift,
    val utgifterDrosje: BigDecimal,
    val begrunnelse: String,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : VilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.REISE_TIL_SAMLING_DROSJE
}

data class VilkårFaktaReiseTilSamlingPrivatBilInterntVedtak(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val nødvendigUtgift: NødvendigUtgift,
    val reiseAvstand: BigDecimal,
    val begrunnelse: String?,
    val aktivitetId: VilkårperiodeGlobalId? = null,
    val bompenger: BigDecimal?,
    val fergekostnad: BigDecimal?,
    val parkering: BigDecimal?,
    val piggdekkavgift: BigDecimal?,
) : ReiseVilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.REISE_TIL_SAMLING_PRIVAT_BIL
}

data class VilkårFaktaFlyttebyråInterntVedtak(
    val tilbud: List<FlyttebyråTilbud>,
    val erBetalingDokumentert: Boolean,
    val nødvendigUtgift: NødvendigUtgift,
    val adresse: String,
) : VilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.FLYTTING_FLYTTEBYRÅ
}

data class VilkårFaktaFlytteSelvInterntVedtak(
    val avstandEnVei: Int?,
    val henger: Int?,
    val bompenger: Int?,
    val ferge: Int?,
    val parkering: Int?,
    val nødvendigUtgift: NødvendigUtgift,
    val adresse: String,
) : VilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.FLYTTING_FLYTTE_SELV
}

data class VilkårFaktaFlyttingUbestemtInterntVedtak(
    val adresse: String,
) : VilkårFaktaInterntVedtak {
    override val type = TypeVilkårFakta.FLYTTING_UBESTEMT
}

data class VurderingInterntVedtak(
    val regel: String,
    val svar: String?,
    val begrunnelse: String?,
)

data class VedtaksperiodeInterntVedtak(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType?,
)
