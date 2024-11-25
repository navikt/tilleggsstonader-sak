package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.lønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeNy
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import java.time.LocalDate
import java.util.UUID

object VilkårperiodeTestUtil {

    fun målgruppe(
        behandlingId: BehandlingId = BehandlingId.random(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        faktaOgVurdering: MålgruppeFaktaOgVurdering = faktaOgVurderingMålgruppe(),
        begrunnelse: String? = null,
        resultat: ResultatVilkårperiode = faktaOgVurdering.utledResultat(),
        slettetKommentar: String? = null,
        forrigeVilkårperiodeId: UUID? = null,
        status: Vilkårstatus = Vilkårstatus.NY,
    ): GeneriskVilkårperiode<MålgruppeFaktaOgVurdering> {
        return GeneriskVilkårperiode(
            behandlingId = behandlingId,
            resultat = resultat,
            slettetKommentar = slettetKommentar,
            forrigeVilkårperiodeId = forrigeVilkårperiodeId,
            status = status,
            fom = fom,
            tom = tom,
            type = faktaOgVurdering.type.vilkårperiodeType,
            begrunnelse = begrunnelse,
            faktaOgVurdering = faktaOgVurdering,
        )
    }

    fun faktaOgVurderingMålgruppe(
        type: MålgruppeType = MålgruppeType.AAP,
        medlemskap: VurderingMedlemskap = vurderingMedlemskap(),
        dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(),
    ): MålgruppeFaktaOgVurdering = when (type) {
        MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
        MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
        MålgruppeType.OMSTILLINGSSTØNAD -> OmstillingsstønadTilsynBarn(
            vurderinger = VurderingOmstillingsstønad(
                medlemskap = medlemskap,
            ),
        )

        MålgruppeType.OVERGANGSSTØNAD -> OvergangssstønadTilsynBarn
        MålgruppeType.AAP -> AAPTilsynBarn(
            vurderinger = VurderingAAP(dekketAvAnnetRegelverk = dekketAvAnnetRegelverk),
        )

        MålgruppeType.UFØRETRYGD -> UføretrygdTilsynBarn(
            vurderinger = VurderingUføretrygd(
                dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                medlemskap = medlemskap,
            ),
        )

        MålgruppeType.NEDSATT_ARBEIDSEVNE -> NedsattArbeidsevneTilsynBarn(
            vurderinger = VurderingNedsattArbeidsevne(
                dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                medlemskap = medlemskap,
            ),
        )

        MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
    }

    fun delvilkårMålgruppeDto() = DelvilkårMålgruppeDto(
        medlemskap = VurderingDto(SvarJaNei.JA_IMPLISITT),
        dekketAvAnnetRegelverk = VurderingDto(SvarJaNei.NEI),
    )

    fun faktaOgVurderingerMålgruppeDto() = FaktaOgVurderingerMålgruppeDto(
        svarMedlemskap = null,
        svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.NEI,
    )

    fun aktivitet(
        behandlingId: BehandlingId = BehandlingId.random(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        faktaOgVurdering: AktivitetFaktaOgVurdering = faktaOgVurderingAktivitet(),
        begrunnelse: String? = null,
        resultat: ResultatVilkårperiode = faktaOgVurdering.utledResultat(),
        slettetKommentar: String? = null,
        status: Vilkårstatus = Vilkårstatus.NY,
    ) = GeneriskVilkårperiode(
        behandlingId = behandlingId,
        resultat = resultat,
        slettetKommentar = slettetKommentar,
        status = status,
        fom = fom,
        tom = tom,
        type = faktaOgVurdering.type.vilkårperiodeType,
        begrunnelse = begrunnelse,
        faktaOgVurdering = faktaOgVurdering,
    )

    fun faktaOgVurderingAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        aktivitetsdager: Int? = 5,
        lønnet: VurderingLønnet = vurderingLønnet(),
    ): AktivitetFaktaOgVurdering = when (type) {
        AktivitetType.TILTAK -> TiltakTilsynBarn(
            vurderinger = VurderingTiltakTilsynBarn(
                lønnet = lønnet,
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
    }

    fun vurderingLønnet(
        svar: SvarJaNei? = SvarJaNei.NEI,
    ) = VurderingLønnet(
        svar = svar,
    )

    fun vurderingMedlemskap(
        svar: SvarJaNei? = SvarJaNei.JA_IMPLISITT,
    ) = VurderingMedlemskap(
        svar = svar,
    )

    fun vurderingDekketAvAnnetRegelverk(
        svar: SvarJaNei? = SvarJaNei.NEI,
    ) = VurderingDekketAvAnnetRegelverk(
        svar = svar,
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

    fun lagreVilkårperiodeMålgruppe(
        type: MålgruppeType = MålgruppeType.OMSTILLINGSSTØNAD,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        svarMedlemskap: SvarJaNei? = null,
        svarDekkesAvAnnetRegelverk: SvarJaNei? = null,
        begrunnelse: String? = null,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = LagreVilkårperiodeNy(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgVurderinger = FaktaOgVurderingerMålgruppeDto(
            svarMedlemskap = svarMedlemskap,
            svarUtgifterDekketAvAnnetRegelverk = svarDekkesAvAnnetRegelverk,
        ),
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

    fun lagreVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        svarLønnet: SvarJaNei? = null,
        begrunnelse: String? = null,
        behandlingId: BehandlingId = BehandlingId.random(),
        aktivitetsdager: Int? = 5,
        kildeId: String? = null,
    ) = LagreVilkårperiodeNy(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgVurderinger = FaktaOgVurderingerAktivitetBarnetilsynDto(
            svarLønnet = svarLønnet,
            aktivitetsdager = aktivitetsdager,
        ),
        kildeId = kildeId,
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
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

    fun Vilkårperiode.medLønnet(
        lønnet: VurderingLønnet,
    ): Vilkårperiode {
        val faktaOgVurdering1 = this.faktaOgVurdering
        return when (faktaOgVurdering1) {
            is TiltakTilsynBarn -> withTypeOrThrow<TiltakTilsynBarn>().copy(
                faktaOgVurdering = faktaOgVurdering1.copy(
                    vurderinger = faktaOgVurdering1.vurderinger.copy(lønnet = lønnet),
                ),
            )

            else -> error("Har ikke mappet ${faktaOgVurdering1::class.simpleName}")
        }
    }
}
