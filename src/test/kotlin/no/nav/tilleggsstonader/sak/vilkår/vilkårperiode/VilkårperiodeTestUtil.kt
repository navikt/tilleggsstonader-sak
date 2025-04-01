package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AAPTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.NedsattArbeidsevneTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ReellArbeidsøkerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UføretrygdTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAP
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAAPLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevne
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingNedsattArbeidsevneLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingOmstillingsstønad
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingTiltakTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygd
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingUføretrygdLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingerUtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilFaktaOgSvarDto
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
        stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
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
            gitVersjon = Applikasjonsversjon.versjon,
        )

    fun vurderingFaktaEtterlevelseAldersvilkår(fødselsdato: LocalDate = LocalDate.of(2000, 1, 1)) =
        AldersvilkårVurdering
            .VurderingFaktaEtterlevelseAldersvilkår(
                fødselsdato = fødselsdato,
            )

    fun faktaOgVurderingMålgruppe(
        type: MålgruppeType = MålgruppeType.AAP,
        medlemskap: VurderingMedlemskap = vurderingMedlemskap(),
        dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(),
        aldersvilkår: VurderingAldersVilkår = vurderingAldersVilkår(),
        mottarSykepengerForFulltidsstilling: VurderingMottarSykepengerForFulltidsstilling = vurderingMottarSykepengerForFulltidsstilling(),
    ): MålgruppeFaktaOgVurdering =
        when (type) {
            MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeTilsynBarn
            MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerTilsynBarn
            MålgruppeType.OMSTILLINGSSTØNAD ->
                OmstillingsstønadTilsynBarn(
                    vurderinger =
                        VurderingOmstillingsstønad(
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
                        ),
                )

            MålgruppeType.OVERGANGSSTØNAD -> OvergangssstønadTilsynBarn
            MålgruppeType.AAP ->
                AAPTilsynBarn(
                    vurderinger =
                        VurderingAAP(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            aldersvilkår = aldersvilkår,
                        ),
                )

            MålgruppeType.UFØRETRYGD ->
                UføretrygdTilsynBarn(
                    vurderinger =
                        VurderingUføretrygd(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
                        ),
                )

            MålgruppeType.NEDSATT_ARBEIDSEVNE ->
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
                            mottarSykepengerForFulltidsstilling = mottarSykepengerForFulltidsstilling,
                        ),
                )

            MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger")
        }

    fun faktaOgVurderingMålgruppeLæremidler(
        type: MålgruppeType = MålgruppeType.AAP,
        medlemskap: VurderingMedlemskap = vurderingMedlemskap(),
        dekketAvAnnetRegelverk: VurderingDekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(),
        aldersvilkår: VurderingAldersVilkår = vurderingAldersVilkår(),
    ): MålgruppeFaktaOgVurdering =
        when (type) {
            MålgruppeType.INGEN_MÅLGRUPPE -> IngenMålgruppeLæremidler
            MålgruppeType.SYKEPENGER_100_PROSENT -> SykepengerLæremidler
            MålgruppeType.OMSTILLINGSSTØNAD ->
                OmstillingsstønadLæremidler(
                    vurderinger =
                        VurderingOmstillingsstønad(
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
                        ),
                )

            MålgruppeType.OVERGANGSSTØNAD -> OvergangssstønadLæremidler
            MålgruppeType.AAP ->
                AAPLæremidler(
                    vurderinger = VurderingAAPLæremidler(dekketAvAnnetRegelverk = dekketAvAnnetRegelverk, aldersvilkår = aldersvilkår),
                )

            MålgruppeType.UFØRETRYGD ->
                UføretrygdLæremidler(
                    vurderinger =
                        VurderingUføretrygdLæremidler(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
                        ),
                )

            MålgruppeType.NEDSATT_ARBEIDSEVNE ->
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingNedsattArbeidsevneLæremidler(
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverk,
                            medlemskap = medlemskap,
                            aldersvilkår = aldersvilkår,
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
        gitVersjon = Applikasjonsversjon.versjon,
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

    fun vurderingAldersVilkår(svar: SvarJaNei = SvarJaNei.JA) =
        VurderingAldersVilkår(svar = svar, vurderingFaktaEtterlevelse = vurderingFaktaEtterlevelseAldersvilkår())

    fun vurderingMottarSykepengerForFulltidsstilling(svar: SvarJaNei? = SvarJaNei.NEI) =
        VurderingMottarSykepengerForFulltidsstilling(svar = svar)

    fun dummyVilkårperiodeMålgruppe(
        type: MålgruppeType = MålgruppeType.OMSTILLINGSSTØNAD,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        medlemskap: SvarJaNei? = null,
        dekkesAvAnnetRegelverk: SvarJaNei? = null,
        mottarSykepengerForFulltidsstilling: SvarJaNei? = null,
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
                svarMottarSykepengerForFulltidsstilling = mottarSykepengerForFulltidsstilling,
            ),
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
    )

    val faktaOgSvarTilsynBarnDto =
        FaktaOgSvarAktivitetBarnetilsynDto(
            svarLønnet = SvarJaNei.NEI,
            aktivitetsdager = 5,
        )

    fun dummyVilkårperiodeAktivitet(
        type: AktivitetType = AktivitetType.TILTAK,
        fom: LocalDate = osloDateNow(),
        tom: LocalDate = osloDateNow(),
        begrunnelse: String? = null,
        behandlingId: BehandlingId = BehandlingId.random(),
        kildeId: String? = null,
        faktaOgSvar: FaktaOgSvarDto = faktaOgSvarTilsynBarnDto,
    ) = LagreVilkårperiode(
        type = type,
        fom = fom,
        tom = tom,
        faktaOgSvar = faktaOgSvar,
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

    fun Vilkårperiode.tilOppdatering() =
        LagreVilkårperiode(
            behandlingId = behandlingId,
            fom = fom,
            tom = tom,
            faktaOgSvar = faktaOgVurdering.tilFaktaOgSvarDto(),
            begrunnelse = begrunnelse,
            type = type,
        )
}
