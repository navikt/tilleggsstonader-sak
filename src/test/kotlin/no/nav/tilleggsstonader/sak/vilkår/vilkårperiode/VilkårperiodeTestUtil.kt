package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingerUtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
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
    ): GeneriskVilkårperiode<MålgruppeFaktaOgVurdering> =
        GeneriskVilkårperiode(
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

    fun faktaOgVurderingMålgruppe(
        type: MålgruppeType = MålgruppeType.AAP,
        medlemskap: VurderingMedlemskap = vurderingMedlemskap(),
        dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(),
    ): MålgruppeFaktaOgVurdering =
        when (type) {
            MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
            MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
            MålgruppeType.OMSTILLINGSSTØNAD ->
                OmstillingsstønadTilsynBarn(
                    vurderinger =
                        VurderingOmstillingsstønad(
                            medlemskap = medlemskap,
                        ),
                )

            MålgruppeType.OVERGANGSSTØNAD -> OvergangssstønadTilsynBarn
            MålgruppeType.AAP ->
                AAPTilsynBarn(
                    vurderinger = VurderingAAP(dekketAvAnnetRegelverk = dekketAvAnnetRegelverk),
                )

            MålgruppeType.UFØRETRYGD ->
                UføretrygdTilsynBarn(
                    vurderinger =
                        VurderingUføretrygd(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                        ),
                )

            MålgruppeType.NEDSATT_ARBEIDSEVNE ->
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                        ),
                )

            MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        }

    fun faktaOgVurderingerMålgruppeDto() =
        FaktaOgSvarMålgruppeDto(
            svarMedlemskap = null,
            svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.NEI,
        )

    fun aktivitet(
        behandlingId: BehandlingId = BehandlingId.random(),
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow().plusDays(5),
        faktaOgVurdering: AktivitetFaktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(),
        begrunnelse: String? = null,
        resultat: ResultatVilkårperiode = faktaOgVurdering.utledResultat(),
        slettetKommentar: String? = null,
        status: Vilkårstatus = Vilkårstatus.NY,
        kildeId: String? = null,
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
        kildeId = kildeId,
    )

    fun faktaOgVurderingAktivitetTilsynBarn(
        type: AktivitetType = AktivitetType.TILTAK,
        aktivitetsdager: Int? = 5,
        lønnet: VurderingLønnet = vurderingLønnet(),
    ): AktivitetFaktaOgVurdering =
        when (type) {
            AktivitetType.TILTAK ->
                TiltakTilsynBarn(
                    vurderinger =
                        VurderingTiltakTilsynBarn(
                            lønnet = lønnet,
                        ),
                    fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
                )

            AktivitetType.UTDANNING ->
                UtdanningTilsynBarn(
                    fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
                )

            AktivitetType.REELL_ARBEIDSSØKER ->
                ReellArbeidsøkerTilsynBarn(
                    fakta = FaktaAktivitetTilsynBarn(aktivitetsdager = aktivitetsdager!!),
                )

            AktivitetType.INGEN_AKTIVITET -> IngenAktivitetTilsynBarn
        }

    fun faktaOgVurderingAktivitetLæremidler(
        type: AktivitetType = AktivitetType.TILTAK,
        prosent: Int = 80,
        studienivå: Studienivå = Studienivå.HØYERE_UTDANNING,
        harUtgifter: VurderingHarUtgifter = vurderingHarUtgifter(),
        harRettTilUtstyrsstipend: VurderingHarRettTilUtstyrsstipend = vurderingHarRettTilUtstyrsstipend(),
    ): AktivitetFaktaOgVurdering =
        when (type) {
            AktivitetType.TILTAK ->
                TiltakLæremidler(
                    vurderinger =
                        VurderingTiltakLæremidler(
                            harUtgifter = harUtgifter,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipend,
                        ),
                    fakta = FaktaAktivitetLæremidler(prosent, studienivå),
                )

            AktivitetType.UTDANNING ->
                UtdanningLæremidler(
                    fakta = FaktaAktivitetLæremidler(prosent, studienivå),
                    vurderinger =
                        VurderingerUtdanningLæremidler(
                            harUtgifter = harUtgifter,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipend,
                        ),
                )

            AktivitetType.INGEN_AKTIVITET -> IngenAktivitetTilsynBarn
            else -> {
                throw IllegalArgumentException("$type er ikke en gyldig aktivitetstype for læremidler")
            }
        }

    fun vurderingLønnet(svar: SvarJaNei? = SvarJaNei.NEI) = VurderingLønnet(svar = svar)

    fun vurderingHarRettTilUtstyrsstipend(svar: SvarJaNei? = SvarJaNei.NEI) = VurderingHarRettTilUtstyrsstipend(svar = svar)

    fun vurderingMedlemskap(svar: SvarJaNei? = SvarJaNei.JA_IMPLISITT) = VurderingMedlemskap(svar = svar)

    fun vurderingDekketAvAnnetRegelverk(svar: SvarJaNei? = SvarJaNei.NEI) = VurderingDekketAvAnnetRegelverk(svar = svar)

    fun vurderingHarUtgifter(svar: SvarJaNei? = SvarJaNei.JA) = VurderingHarUtgifter(svar = svar)

    fun dummyVilkårperiodeMålgruppe(
        type: MålgruppeType = MålgruppeType.OMSTILLINGSSTØNAD,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        medlemskap: SvarJaNei? = null,
        dekkesAvAnnetRegelverk: SvarJaNei? = null,
        begrunnelse: String? = null,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgSvar =
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = medlemskap,
                svarUtgifterDekketAvAnnetRegelverk = dekkesAvAnnetRegelverk,
            ),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
    )

    fun dummyVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        svarLønnet: SvarJaNei? = null,
        begrunnelse: String? = null,
        behandlingId: BehandlingId = BehandlingId.random(),
        aktivitetsdager: Int? = 5,
        kildeId: String? = null,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgSvar =
            FaktaOgSvarAktivitetBarnetilsynDto(
                svarLønnet = svarLønnet,
                aktivitetsdager = aktivitetsdager,
            ),
        kildeId = kildeId,
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
    )

    fun Vilkårperiode.medAktivitetsdager(aktivitetsdager: Int): Vilkårperiode {
        val fakta = faktaOgVurdering.fakta
        require(fakta is FaktaAktivitetTilsynBarn)
        val nyFakta = fakta.copy(aktivitetsdager = aktivitetsdager)

        return when (faktaOgVurdering) {
            is TiltakTilsynBarn ->
                withTypeOrThrow<TiltakTilsynBarn>()
                    .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            is UtdanningTilsynBarn ->
                withTypeOrThrow<UtdanningTilsynBarn>()
                    .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            is ReellArbeidsøkerTilsynBarn ->
                withTypeOrThrow<ReellArbeidsøkerTilsynBarn>()
                    .let { it.copy(faktaOgVurdering = it.faktaOgVurdering.copy(fakta = nyFakta)) }

            else -> error("Har ikke aktivitetsdager på type ${faktaOgVurdering::class}")
        }
    }

    fun Vilkårperiode.medLønnet(lønnet: VurderingLønnet): Vilkårperiode {
        val faktaOgVurdering1 = this.faktaOgVurdering
        return when (faktaOgVurdering1) {
            is TiltakTilsynBarn ->
                withTypeOrThrow<TiltakTilsynBarn>().copy(
                    faktaOgVurdering =
                        faktaOgVurdering1.copy(
                            vurderinger = faktaOgVurdering1.vurderinger.copy(lønnet = lønnet),
                        ),
                )

            else -> error("Har ikke mappet ${faktaOgVurdering1::class.simpleName}")
        }
    }
}
