package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårOgFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import java.time.LocalDate
import java.util.UUID

object VilkårperiodeTestUtil {

    fun målgruppe(
        behandlingId: BehandlingId = BehandlingId.random(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        delvilkår: DelvilkårMålgruppe = delvilkårMålgruppe(),
        begrunnelse: String? = null,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
        slettetKommentar: String? = null,
        forrigeVilkårperiodeId: UUID? = null,
        status: Vilkårstatus = Vilkårstatus.NY,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        resultat = resultat,
        slettetKommentar = slettetKommentar,
        forrigeVilkårperiodeId = forrigeVilkårperiodeId,
        status = status,
        vilkårOgFakta = VilkårOgFakta(
            fom = fom,
            tom = tom,
            type = type,
            delvilkår = delvilkår,
            begrunnelse = begrunnelse,
            aktivitetsdager = null,
        ),
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
        behandlingId: BehandlingId = BehandlingId.random(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        aktivitetsdager: Int? = 5,
        delvilkår: DelvilkårAktivitet = delvilkårAktivitet(),
        begrunnelse: String? = null,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
        slettetKommentar: String? = null,
        status: Vilkårstatus = Vilkårstatus.NY,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        resultat = resultat,
        slettetKommentar = slettetKommentar,
        status = status,
        vilkårOgFakta = VilkårOgFakta(
            type = type,
            fom = fom,
            tom = tom,
            delvilkår = delvilkår,
            begrunnelse = begrunnelse,
            aktivitetsdager = aktivitetsdager,
        ),
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
        behandlingId: BehandlingId = BehandlingId.random(),
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
        behandlingId: BehandlingId = BehandlingId.random(),
        aktivitetsdager: Int? = 5,
        kildeId: String? = null,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        delvilkår = DelvilkårAktivitetDto(lønnet),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
        aktivitetsdager = aktivitetsdager,
        kildeId = kildeId,
    )

    fun Vilkårperiode.medVilkårOgFakta(
        fom: LocalDate = this.vilkårOgFakta.fom,
        tom: LocalDate = this.vilkårOgFakta.tom,
        begrunnelse: String? = this.vilkårOgFakta.begrunnelse,
        delvilkår: DelvilkårVilkårperiode = this.vilkårOgFakta.delvilkår,
        aktivitetsdager: Int? = this.vilkårOgFakta.aktivitetsdager,
    ): Vilkårperiode {
        return this.copy(
            vilkårOgFakta = this.vilkårOgFakta.copy(
                fom = fom,
                tom = tom,
                begrunnelse = begrunnelse,
                delvilkår = delvilkår,
                aktivitetsdager = aktivitetsdager,
            ),
        )
    }
}
