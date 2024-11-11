package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
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
    ) = GeneriskVilkårperiode(
        behandlingId = behandlingId,
        resultat = resultat,
        slettetKommentar = slettetKommentar,
        forrigeVilkårperiodeId = forrigeVilkårperiodeId,
        status = status,
        fom = fom,
        tom = tom,
        type = type,
        begrunnelse = begrunnelse,
        faktaOgVurdering = when (type) {
            MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
            MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
            MålgruppeType.OMSTILLINGSSTØNAD -> OmstillingsstønadTilsynBarn(
                vurderinger = VurderingOmstillingsstønad(
                    medlemskap = delvilkår.medlemskap,
                ),
            )

            MålgruppeType.OVERGANGSSTØNAD -> OvergangssstønadTilsynBarn
            MålgruppeType.AAP -> AAPTilsynBarn(
                vurderinger = VurderingAAP(dekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk),
            )

            MålgruppeType.UFØRETRYGD -> UføretrygdTilsynBarn(
                vurderinger = VurderingUføretrygd(
                    dekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk,
                    medlemskap = delvilkår.medlemskap,
                ),
            )

            MålgruppeType.NEDSATT_ARBEIDSEVNE -> NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    dekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk,
                    medlemskap = delvilkår.medlemskap,
                ),
            )

            MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        },
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
    ) = GeneriskVilkårperiode(
        behandlingId = behandlingId,
        resultat = resultat,
        slettetKommentar = slettetKommentar,
        status = status,
        fom = fom,
        tom = tom,
        type = type,
        begrunnelse = begrunnelse,
        faktaOgVurdering = when (type) {
            AktivitetType.TILTAK -> TiltakTilsynBarn(
                vurderinger = VurderingTiltakTilsynBarn(
                    lønnet = delvilkår.lønnet,
                ),
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
            )

            AktivitetType.UTDANNING -> UtdanningTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
            )

            AktivitetType.REELL_ARBEIDSSØKER -> ReellArbeidsøkerTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
            )

            AktivitetType.INGEN_AKTIVITET -> IngenAktivitetTilsynBarn
        },
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

    fun Vilkårperiode.medAktivitetsdager(
        aktivitetsdager: Int,
    ): Vilkårperiode {
        val fakta = faktaOgVurdering.fakta
        require(fakta is FaktaAktivitetTilsynBarn)
        val nyFakta = fakta.copy(aktivitetsdager = aktivitetsdager)

        return when (faktaOgVurdering) {
            is TiltakTilsynBarn -> withTypeOrThrow<TiltakTilsynBarn>()
                .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            is UtdanningTilsynBarn -> withTypeOrThrow<UtdanningTilsynBarn>()
                .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            is ReellArbeidsøkerTilsynBarn -> withTypeOrThrow<ReellArbeidsøkerTilsynBarn>()
                .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            else -> error("Har ikke aktivitetsdager på type ${faktaOgVurdering::class}")
        }
    }

    fun Vilkårperiode.medVurdering(
        delvilkår: DelvilkårVilkårperiode,
    ): Vilkårperiode {
        return when (delvilkår) {
            is DelvilkårAktivitet -> {
                val nyVurdering = VurderingTiltakTilsynBarn(lønnet = delvilkår.lønnet)

                withTypeOrThrow<TiltakTilsynBarn>()
                    .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(vurderinger = nyVurdering)) }
            }

            is DelvilkårMålgruppe -> {
                val oppdatertFaktaOgVurdering = faktaOgVurdering.let {
                    when (it) {
                        OvergangssstønadTilsynBarn,
                        IngenMålgruppeTilsynBarn,
                        SykepengerTilsynBarn,
                        -> error("${this::class.java.simpleName} har ikke vurderinger")
                        is AAPTilsynBarn -> it.copy(vurderinger = it.vurderinger.copy(dekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk))
                        is NedsattArbeidsevneTilsynBarn -> it.copy(
                            vurderinger = it.vurderinger.copy(
                                medlemskap = delvilkår.medlemskap,
                                dekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk,
                            ),
                        )
                        is OmstillingsstønadTilsynBarn -> it.copy(
                            vurderinger = it.vurderinger.copy(
                                medlemskap = delvilkår.medlemskap,
                            ),
                        )
                        is UføretrygdTilsynBarn -> it.copy(
                            vurderinger = it.vurderinger.copy(
                                medlemskap = delvilkår.medlemskap,
                            ),
                        )

                        is AktivitetFaktaOgVurdering -> error("${this::class.java.simpleName} har feil type for DelvilkårMålgruppe")
                    }
                }
                withTypeOrThrow<MålgruppeFaktaOgVurdering>().copy(faktaOgVurdering = oppdatertFaktaOgVurdering)
            }
        }
    }
}
