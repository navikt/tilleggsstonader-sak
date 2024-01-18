package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import java.time.LocalDate
import java.util.UUID

object VilkårperiodeTestUtil {

    fun målgruppe(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        delvilkår: DelvilkårMålgruppe = delvilkårMålgruppe(),
        begrunnelse: String? = null,
        kilde: KildeVilkårsperiode = KildeVilkårsperiode.SYSTEM,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        delvilkår = delvilkår,
        begrunnelse = begrunnelse,
        kilde = kilde,
        resultat = resultat,
    )

    fun delvilkårMålgruppe() = DelvilkårMålgruppe(
        medlemskap = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.JA_IMPLISITT,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    )

    fun delvilkårMålgruppeDto() = DelvilkårMålgruppeDto(
        medlemskap = VurderingDto(SvarJaNei.JA_IMPLISITT),
    )

    fun aktivitet(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        delvilkår: DelvilkårAktivitet = delvilkårAktivitet(),
        begrunnelse: String? = null,
        kilde: KildeVilkårsperiode = KildeVilkårsperiode.SYSTEM,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        delvilkår = delvilkår,
        begrunnelse = begrunnelse,
        kilde = kilde,
        resultat = resultat,
    )

    fun delvilkårAktivitet() = DelvilkårAktivitet(
        lønnet = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.NEI,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
        mottarSykepenger = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.NEI,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    )

    fun delvilkårAktivitetDto() = DelvilkårAktivitetDto(
        lønnet = VurderingDto(SvarJaNei.NEI),
        mottarSykepenger = VurderingDto(SvarJaNei.NEI),
    )

    fun opprettVilkårperiodeMålgruppe(
        type: MålgruppeType = MålgruppeType.OMSTILLINGSSTØNAD,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        medlemskap: VurderingDto? = null,
        begrunnelse: String? = null,
    ) = OpprettVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårMålgruppeDto(medlemskap = medlemskap),
        begrunnelse = begrunnelse,
    )

    fun opprettVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        lønnet: VurderingDto? = null,
        mottarSykepenger: VurderingDto? = null,
        begrunnelse: String? = null,
    ) = OpprettVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårAktivitetDto(lønnet, mottarSykepenger),
        begrunnelse = begrunnelse,
    )

    val Vilkårperiode.medlemskap: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårMålgruppe).medlemskap

    val Vilkårperiode.lønnet: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).lønnet

    val Vilkårperiode.mottarSykepenger: DelvilkårVilkårperiode.Vurdering
        get() = (this.delvilkår as DelvilkårAktivitet).mottarSykepenger
}
