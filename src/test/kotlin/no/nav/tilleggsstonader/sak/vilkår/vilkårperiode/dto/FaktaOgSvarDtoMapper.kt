package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfVurderinger
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaProsent
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarRettTilUtstyrsstipendVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.HarUtgifterVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MottarSykepengerForFulltidsstillingVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering

fun FaktaOgVurdering.tilFaktaOgSvarDto(): FaktaOgSvarDto =
    when (this) {
        is MålgruppeFaktaOgVurdering ->
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = vurderinger.takeIfVurderinger<MedlemskapVurdering>()?.medlemskap?.svar,
                svarUtgifterDekketAvAnnetRegelverk =
                    vurderinger
                        .takeIfVurderinger<DekketAvAnnetRegelverkVurdering>()
                        ?.dekketAvAnnetRegelverk
                        ?.svar,
                svarMottarSykepengerForFulltidsstilling =
                    vurderinger
                        .takeIfVurderinger<MottarSykepengerForFulltidsstillingVurdering>()
                        ?.mottarSykepengerForFulltidsstilling
                        ?.svar,
            )

        is AktivitetFaktaOgVurdering -> {
            when (this) {
                is FaktaOgVurderingTilsynBarn ->
                    FaktaOgSvarAktivitetBarnetilsynDto(
                        aktivitetsdager = fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
                        svarLønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.svar,
                    )

                is FaktaOgVurderingLæremidler ->
                    FaktaOgSvarAktivitetLæremidlerDto(
                        prosent = fakta.takeIfFakta<FaktaProsent>()?.prosent,
                        svarHarUtgifter = vurderinger.takeIfVurderinger<HarUtgifterVurdering>()?.harUtgifter?.svar,
                        svarHarRettTilUtstyrsstipend =
                            vurderinger
                                .takeIfVurderinger<HarRettTilUtstyrsstipendVurdering>()
                                ?.harRettTilUtstyrsstipend
                                ?.svar,
                    )
                is FaktaOgVurderingBoutgifter ->
                    FaktaOgSvarAktivitetBoutgifterDto(
                        svarLønnet = vurderinger.takeIfVurderinger<LønnetVurdering>()?.lønnet?.svar,
                    )
            }
        }
    }
