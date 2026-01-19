package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto

fun VilkårperiodeDto.tilLagreVilkårperiodeAktivitet(behandlingId: BehandlingId) =
    LagreVilkårperiode(
        behandlingId = behandlingId,
        type = type as AktivitetType,
        typeAktivitet = typeAktivitet?.kode?.let { TypeAktivitet.valueOf(it) },
        fom = fom,
        tom = tom,
        faktaOgSvar = faktaOgVurderinger.tilFaktaOgSvar(),
        begrunnelse = begrunnelse,
    )

fun VilkårperiodeDto.tilLagreVilkårperiodeMålgruppe(behandlingId: BehandlingId) =
    LagreVilkårperiode(
        behandlingId = behandlingId,
        type = type as MålgruppeType,
        typeAktivitet = null,
        fom = fom,
        tom = tom,
        faktaOgSvar = VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto(), // TODO
        begrunnelse = begrunnelse,
    )

// TODO gjør denne generisk
private fun FaktaOgVurderingerDto.tilFaktaOgSvar() =
    FaktaOgSvarAktivitetDagligReiseTsrDto(
        svarHarUtgifter = SvarJaNei.JA,
    )
