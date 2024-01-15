package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårMålgruppeDto
import java.time.LocalDate
import java.util.UUID

object VilkårperiodeDomainUtil {

    fun målgruppe(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: MålgruppeType = MålgruppeType.AAP,
        detaljer: DelvilkårMålgruppe = delvilkårMålgruppe(),
        begrunnelse: String? = null,
        kilde: KildeVilkårsperiode = KildeVilkårsperiode.SYSTEM,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        delvilkår = detaljer,
        begrunnelse = begrunnelse,
        kilde = kilde,
        resultat = resultat,
    )

    fun delvilkårMålgruppe() = DelvilkårMålgruppe(
        medlemskap = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.JA_IMPLISITT,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    )

    fun delvilkårMålgruppeDto() = DelvilkårMålgruppeDto(
        medlemskap = SvarJaNei.JA_IMPLISITT,
    )

    fun aktivitet(
        behandlingId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now().plusDays(5),
        type: AktivitetType = AktivitetType.TILTAK,
        detaljer: DelvilkårAktivitet = delvilkårAktivitet(),
        begrunnelse: String? = null,
        kilde: KildeVilkårsperiode = KildeVilkårsperiode.SYSTEM,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) = Vilkårperiode(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        type = type,
        delvilkår = detaljer,
        begrunnelse = begrunnelse,
        kilde = kilde,
        resultat = resultat,
    )

    fun delvilkårAktivitet() = DelvilkårAktivitet(
        lønnet = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.NEI,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
        mottarSykepenger = DelvilkårVilkårperiode.Vurdering(
            svar = SvarJaNei.NEI,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        ),
    )

    fun delvilkårAktivitetDto() = DelvilkårAktivitetDto(
        lønnet = SvarJaNei.NEI,
        mottarSykepenger = SvarJaNei.NEI,
    )
}
