package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadMetadata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

object Testdata {

    val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()))

    val behandlingId = BehandlingId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31")
    val behandling = saksbehandling(
        behandling = behandling(
            id = behandlingId,
            vedtakstidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
            opprettetTid = LocalDate.of(2024, 2, 5).atStartOfDay(),
            fagsak = fagsak,
            resultat = BehandlingResultat.INNVILGET,
            type = BehandlingType.REVURDERING,
            revurderFra = LocalDate.of(2024, 1, 1),
        ),
        fagsak = fagsak,
    )

    val stønadsperioder = listOf(
        StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 3, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
            status = StønadsperiodeStatus.NY,
        ),
        StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 3, 31),
            målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.REELL_ARBEIDSSØKER,
            status = StønadsperiodeStatus.NY,
        ),
    )

    val totrinnskontroll = TotrinnskontrollUtil.totrinnskontroll(TotrinnInternStatus.GODKJENT, beslutter = "saksbeh2")
    val søknadMetadata = SøknadMetadata(
        journalpostId = "journalpostId",
        mottattTidspunkt = LocalDate.of(2023, 1, 1).atStartOfDay().truncatedTo(ChronoUnit.MILLIS),
        språk = Språkkode.NB,
    )

    val barn = listOf(
        GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "1", fødselsdato = LocalDate.of(2024, 5, 15)),
        GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "2", fødselsdato = LocalDate.of(2024, 10, 15)),
    )

    val grunnlagsdata = GrunnlagsdataUtil.grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = barn))
    val behandlingBarn = listOf(
        behandlingBarn(personIdent = grunnlagsdata.grunnlag.barn.first().ident),
        behandlingBarn(personIdent = grunnlagsdata.grunnlag.barn.last().ident),
    )
    val barnId = behandlingBarn[0].id
    val barnId2 = behandlingBarn[1].id

    val vilkår = listOf(
        vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            delvilkår = oppfylteDelvilkårPassBarn(),
            barnId = barnId,
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29),
            utgift = 100,
        ).tilDto(),
        vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            delvilkår = oppfylteDelvilkårPassBarn(),
            barnId = barnId,
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29),
            utgift = 200,
        ).tilDto(),
        vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            delvilkår = oppfylteDelvilkårPassBarn(),
            barnId = barnId2,
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29),
            utgift = 200,
        ).tilDto(),
    )

    val vedtak = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data = InnvilgelseTilsynBarn(
            beregningsresultat = BeregningsresultatTilsynBarn(emptyList()),
        ),
    )

    val vilkårperioderTilsynBarn = Vilkårperioder(
        målgrupper = listOf(
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe aap",
                faktaOgVurdering = faktaOgVurderingMålgruppe(
                    type = MålgruppeType.AAP,
                    medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                    dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI),
                ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe os",
                faktaOgVurdering = faktaOgVurderingMålgruppe(
                    type = MålgruppeType.OVERGANGSSTØNAD,
                    medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                    dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
        ),
        aktiviteter = listOf(
            VilkårperiodeTestUtil.aktivitet(
                begrunnelse = "aktivitet abd",
                resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                faktaOgVurdering = faktaOgVurderingAktivitet(
                    lønnet = vurderingLønnet(SvarJaNei.JA),
                ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.aktivitet(
                resultat = ResultatVilkårperiode.SLETTET,
                slettetKommentar = "kommentar slettet",
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
        ),
    )
}
