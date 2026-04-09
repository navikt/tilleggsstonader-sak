package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetDagligReiseTsoFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetDagligReiseTsrFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetLæremidlerFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.MålgruppeFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.MålgruppeLæremidlerFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto

fun VilkårperiodeDto.tilLagreVilkårperiodeAktivitet(behandlingId: BehandlingId) =
    LagreVilkårperiode(
        behandlingId = behandlingId,
        type = type as AktivitetType,
        typeAktivitet = typeAktivitet?.kode?.let { TypeAktivitet.valueOf(it) },
        fom = fom,
        tom = tom,
        faktaOgSvar = faktaOgVurderinger.tilFaktaOgSvarDto(),
        begrunnelse = begrunnelse,
    )

fun VilkårperiodeDto.tilLagreVilkårperiodeMålgruppe(behandlingId: BehandlingId) =
    LagreVilkårperiode(
        behandlingId = behandlingId,
        type = type as MålgruppeType,
        typeAktivitet = null,
        fom = fom,
        tom = tom,
        faktaOgSvar = faktaOgVurderinger.tilFaktaOgSvarDto(),
        begrunnelse = begrunnelse,
    )

private fun FaktaOgVurderingerDto.tilFaktaOgSvarDto(): FaktaOgSvarDto =
    when (this) {
        is AktivitetDagligReiseTsoFaktaOgVurderingerDto -> {
            FaktaOgSvarAktivitetDagligReiseTsoDto(
                svarLønnet = this.lønnet?.svar,
                svarHarUtgifter = this.harUtgifter?.svar,
                aktivitetsdager = this.aktivitetsdager,
            )
        }

        is AktivitetDagligReiseTsrFaktaOgVurderingerDto -> {
            FaktaOgSvarAktivitetDagligReiseTsrDto(
                svarHarUtgifter = this.harUtgifter?.svar,
                aktivitetsdager = this.aktivitetsdager,
            )
        }

        is AktivitetLæremidlerFaktaOgVurderingerDto -> {
            FaktaOgSvarAktivitetLæremidlerDto(
                svarHarUtgifter = this.harUtgifter?.svar,
                studienivå = this.studienivå,
                svarHarRettTilUtstyrsstipend = this.harRettTilUtstyrsstipend?.svar,
                prosent = this.prosent,
            )
        }

        is MålgruppeFaktaOgVurderingerDto -> {
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = this.medlemskap?.svar,
                svarUtgifterDekketAvAnnetRegelverk = this.utgifterDekketAvAnnetRegelverk?.svar,
                svarMottarSykepengerForFulltidsstilling = this.mottarSykepengerForFulltidsstilling?.svar,
            )
        }

        is MålgruppeLæremidlerFaktaOgVurderingerDto ->
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = this.medlemskap?.svar,
                svarUtgifterDekketAvAnnetRegelverk = this.utgifterDekketAvAnnetRegelverk?.svar,
            )

        else -> TODO()
    }
