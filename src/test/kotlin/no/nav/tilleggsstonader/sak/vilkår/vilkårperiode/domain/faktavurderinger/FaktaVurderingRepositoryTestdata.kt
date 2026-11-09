package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagFødselFaktaGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mapFaktaOgSvarDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBoutgifterDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetPassAvBarnDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseTilSamlingTsoDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetReiseTilSamlingTsrDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode

object FaktaVurderingRepositoryTestdata {
    private val behandlingId = BehandlingId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31")
    private val fraOgMed = 1 januar 2025
    private val tilOgMed = 31 januar 2025

    fun forType(type: TypeFaktaOgVurdering): FaktaOgVurdering {
        val stønadstype = alleEnumTyperFaktaOgVurdering.single { it.second == type }.first
        val faktaOgSvar = faktaOgSvar(type, stønadstype)
        val vilkårperiode =
            LagreVilkårperiode(
                behandlingId = behandlingId,
                type = type.vilkårperiodeType,
                fom = fraOgMed,
                tom = tilOgMed,
                faktaOgSvar = faktaOgSvar,
            )

        return mapFaktaOgSvarDto(
            vilkårperiode = vilkårperiode,
            stønadstype = stønadstype,
            fødselFaktaGrunnlag = lagFødselFaktaGrunnlag(fødselsdato = 1 januar 2000),
        )
    }

    private fun faktaOgSvar(
        type: TypeFaktaOgVurdering,
        stønadstype: Stønadstype,
    ) = when {
        type is TypeAktivitetOgVurdering ->
            when (stønadstype) {
                Stønadstype.BARNETILSYN ->
                    FaktaOgSvarAktivitetPassAvBarnDto(
                        aktivitetsdager =
                            if (type.vilkårperiodeType ==
                                AktivitetType.UTDANNING
                            ) {
                                5
                            } else if (type.vilkårperiodeType == AktivitetType.INGEN_AKTIVITET) {
                                null
                            } else {
                                3
                            },
                        svarLønnet = SvarJaNei.NEI,
                    )

                Stønadstype.LÆREMIDLER ->
                    FaktaOgSvarAktivitetLæremidlerDto(
                        prosent = 70,
                        studienivå = Studienivå.HØYERE_UTDANNING,
                        svarHarUtgifter = SvarJaNei.JA,
                        svarHarRettTilUtstyrsstipend = SvarJaNei.NEI,
                    )

                Stønadstype.BOUTGIFTER -> FaktaOgSvarAktivitetBoutgifterDto(svarLønnet = SvarJaNei.NEI)
                Stønadstype.DAGLIG_REISE_TSO ->
                    FaktaOgSvarAktivitetDagligReiseTsoDto(
                        svarLønnet = SvarJaNei.NEI,
                        svarHarUtgifter = null,
                        aktivitetsdager = 3,
                    )

                Stønadstype.DAGLIG_REISE_TSR ->
                    FaktaOgSvarAktivitetDagligReiseTsrDto(
                        svarHarUtgifter = SvarJaNei.JA,
                        aktivitetsdager = 3,
                    )

                Stønadstype.REISE_TIL_SAMLING_TSO -> FaktaOgSvarAktivitetReiseTilSamlingTsoDto(svarLønnet = SvarJaNei.NEI)
                Stønadstype.REISE_TIL_SAMLING_TSR -> FaktaOgSvarAktivitetReiseTilSamlingTsrDto
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO ->
                    FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsoDto(svarLønnet = SvarJaNei.NEI)

                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR -> FaktaOgSvarAktivitetReiseOppstartAvslutningHjemreiseTsrDto
                Stønadstype.FLYTTING_TSO,
                Stønadstype.FLYTTING_TSR,
                -> error("Flytting har ikke aktivitetstype: $type")
            }

        else ->
            FaktaOgSvarMålgruppeDto(
                svarMedlemskap = SvarJaNei.JA_IMPLISITT,
                svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.NEI,
                svarMottarSykepengerForFulltidsstilling = SvarJaNei.NEI_IMPLISITT,
                svarLønnet = SvarJaNei.NEI,
            )
    }
}
