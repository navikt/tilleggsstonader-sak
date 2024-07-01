package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
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
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        delvilkår: DelvilkårMålgruppe = delvilkårMålgruppe(),
        begrunnelse: String? = null,
        kilde: KildeVilkårsperiode = KildeVilkårsperiode.SYSTEM,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
        slettetKommentar: String? = null,
        forrigeVilkårperiodeId: UUID? = null,
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
        slettetKommentar = slettetKommentar,
        forrigeVilkårperiodeId = forrigeVilkårperiodeId,
    )

    fun delvilkårMålgruppe(
        medlemskap: Vurdering = vurdering(),
        dekkesAvAnnetRegelverk: Vurdering = vurdering(svar = SvarJaNei.NEI),
    ) =
        DelvilkårMålgruppe(
            medlemskap = medlemskap,
            dekketAvAnnetRegelverk = dekkesAvAnnetRegelverk,
        )

    fun vurdering(
        svar: SvarJaNei? = SvarJaNei.JA_IMPLISITT,
        resultat: ResultatDelvilkårperiode = ResultatDelvilkårperiode.OPPFYLT,
    ) = Vurdering(
        svar = svar,
        resultat = resultat,
    )

    fun delvilkårMålgruppeDto() = DelvilkårMålgruppeDto(
        medlemskap = VurderingDto(SvarJaNei.JA_IMPLISITT),
        dekketAvAnnetRegelverk = VurderingDto(SvarJaNei.NEI),
    )

    fun aktivitet(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        aktivitetsdager: Int? = 5,
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
        aktivitetsdager = aktivitetsdager,
        slettetKommentar = slettetKommentar,
    )

    fun delvilkårAktivitet(
        lønnet: Vurdering = vurdering(
            svar = SvarJaNei.NEI,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    ) = DelvilkårAktivitet(
        lønnet = lønnet,
    )

    fun delvilkårAktivitetDto() = DelvilkårAktivitetDto(
        lønnet = VurderingDto(SvarJaNei.NEI),
    )

    fun opprettVilkårperiodeMålgruppe(
        type: MålgruppeType = MålgruppeType.OMSTILLINGSSTØNAD,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        medlemskap: VurderingDto? = null,
        dekkesAvAnnetRegelverk: VurderingDto? = null,
        begrunnelse: String? = null,
        behandlingId: UUID = UUID.randomUUID(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårMålgruppeDto(medlemskap = medlemskap, dekketAvAnnetRegelverk = dekkesAvAnnetRegelverk),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
    )

    fun opprettVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        lønnet: VurderingDto? = null,
        begrunnelse: String? = null,
        behandlingId: UUID = UUID.randomUUID(),
        aktivitetsdager: Int? = 5,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårAktivitetDto(lønnet),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
        aktivitetsdager = aktivitetsdager,
    )
}
