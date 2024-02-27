package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
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
        aktivitetsdager = null,
    )

    fun delvilkårMålgruppe(vurdering: Vurdering = vurdering()) = DelvilkårMålgruppe(
        medlemskap = vurdering,
    )

    fun vurdering(
        svar: SvarJaNei = SvarJaNei.JA_IMPLISITT,
        begrunnelse: String? = null,
        resultat: ResultatDelvilkårperiode = ResultatDelvilkårperiode.OPPFYLT,
    ) = Vurdering(
        svar = svar,
        begrunnelse = begrunnelse,
        resultat = resultat,
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
        slettetKommentar: String? = null,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        delvilkår = delvilkår,
        begrunnelse = begrunnelse,
        kilde = kilde,
        resultat = resultat,
        aktivitetsdager = 5,
        slettetKommentar = slettetKommentar,
    )

    fun delvilkårAktivitet(
        lønnet: Vurdering = vurdering(
            svar = SvarJaNei.NEI,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
        mottarSykepenger: Vurdering = vurdering(
            svar = SvarJaNei.NEI,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    ) = DelvilkårAktivitet(
        lønnet = lønnet,
        mottarSykepenger = mottarSykepenger,
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
        behandlingId: UUID = UUID.randomUUID(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårMålgruppeDto(medlemskap = medlemskap),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
    )

    fun opprettVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        lønnet: VurderingDto? = null,
        mottarSykepenger: VurderingDto? = null,
        begrunnelse: String? = null,
        behandlingId: UUID = UUID.randomUUID(),
        aktivitetsdager: Int = 5,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårAktivitetDto(lønnet, mottarSykepenger),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
        aktivitetsdager = aktivitetsdager,
    )
}
