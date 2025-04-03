package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadMetadata
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.time.LocalDate
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning as VedtaksperiodeBeregningsgrunnlag

object Testdata {
    val behandlingId = BehandlingId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31")

    val stønadsperioder =
        listOf(
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

    val søknadMetadata =
        SøknadMetadata(
            journalpostId = "journalpostId",
            mottattTidspunkt = LocalDate.of(2023, 1, 1).atStartOfDay().truncatedTo(ChronoUnit.MILLIS),
            språk = Språkkode.NB,
        )

    private val målgrupper: List<VilkårperiodeMålgruppe> =
        listOf(
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe aap",
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = MålgruppeType.AAP,
                        medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                        dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI),
                        aldersvilkår = vurderingAldersVilkår(),
                    ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe os",
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = MålgruppeType.OVERGANGSSTØNAD,
                        medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                        dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                    ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe na",
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                        medlemskap = vurderingMedlemskap(SvarJaNei.JA),
                        dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                        mottarSykepengerForFulltidsstilling = vurderingMottarSykepengerForFulltidsstilling(SvarJaNei.NEI),
                    ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
        )

    object TilsynBarn {
        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()))

        val behandling =
            saksbehandling(
                behandling =
                    behandling(
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

        val barn =
            listOf(
                GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "1", fødselsdato = LocalDate.of(2024, 5, 15)),
                GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "2", fødselsdato = LocalDate.of(2024, 10, 15)),
            )

        val grunnlagsdata = GrunnlagsdataUtil.grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = barn))

        val behandlingBarn =
            listOf(
                behandlingBarn(
                    personIdent =
                        grunnlagsdata.grunnlag.barn
                            .first()
                            .ident,
                ),
                behandlingBarn(
                    personIdent =
                        grunnlagsdata.grunnlag.barn
                            .last()
                            .ident,
                ),
            )
        val barnId = behandlingBarn[0].id
        val barnId2 = behandlingBarn[1].id

        val vilkår =
            listOf(
                vilkår(
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = oppfylteDelvilkårPassBarn(),
                    barnId = barnId,
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 2, 29),
                    utgift = 100,
                ),
                vilkår(
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = oppfylteDelvilkårPassBarn(),
                    barnId = barnId,
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 2, 29),
                    utgift = 200,
                ),
                vilkår(
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = oppfylteDelvilkårPassBarn(),
                    barnId = barnId2,
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 2, 29),
                    utgift = 200,
                ),
            )

        val vedtaksperiodeBeregningsgrunnlag =
            VedtaksperiodeBeregningsgrunnlag(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 1),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )

        val vedtaksperiode =
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 1),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )

        val vedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseTilsynBarn(
                        beregningsresultat =
                            BeregningsresultatTilsynBarn(
                                perioder =
                                    listOf(
                                        beregningsresultatForMåned(
                                            vedtaksperioder =
                                                listOf(
                                                    vedtaksperiodeGrunnlag(vedtaksperiode = vedtaksperiodeBeregningsgrunnlag),
                                                ),
                                        ),
                                    ),
                            ),
                        begrunnelse = "Sånn her vil en begrunnelse se ut",
                        vedtaksperioder =
                            listOf(
                                vedtaksperiode,
                            ),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
            )

        private val aktiviteterTilsynBarn =
            listOf(
                VilkårperiodeTestUtil.aktivitet(
                    begrunnelse = "aktivitet abd",
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetTilsynBarn(
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
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteterTilsynBarn,
            )
    }

    object Læremidler {
        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.LÆREMIDLER)

        val behandling =
            saksbehandling(
                behandling =
                    behandling(
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

        val grunnlagsdata = GrunnlagsdataUtil.grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = emptyList()))

        val vedtaksperioder =
            listOf(
                LæremidlerTestUtil.vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 31),
                    målgruppe = null,
                    aktivitet = null,
                ),
            )
        val beregningsresultat =
            BeregningsresultatLæremidler(
                perioder =
                    listOf(
                        BeregningsresultatForMåned(
                            beløp = 951,
                            grunnlag =
                                Beregningsgrunnlag(
                                    fom = LocalDate.of(2024, 1, 1),
                                    tom = LocalDate.of(2024, 1, 31),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                    studienivå = Studienivå.HØYERE_UTDANNING,
                                    studieprosent = 100,
                                    sats = 951,
                                    satsBekreftet = true,
                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                    aktivitet = AktivitetType.TILTAK,
                                ),
                        ),
                        BeregningsresultatForMåned(
                            beløp = 951,
                            grunnlag =
                                Beregningsgrunnlag(
                                    fom = LocalDate.of(2024, 2, 1),
                                    tom = LocalDate.of(2024, 2, 29),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                    studienivå = Studienivå.HØYERE_UTDANNING,
                                    studieprosent = 100,
                                    sats = 951,
                                    satsBekreftet = true,
                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                    aktivitet = AktivitetType.TILTAK,
                                ),
                        ),
                    ),
            )

        val innvilgetVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseLæremidler(
                        vedtaksperioder = vedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        begrunnelse = "Sånn her vil en begrunnelse se ut",
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
            )

        val avslåttVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagLæremidler(
                        årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_UTSTYRSSTIPEND),
                        begrunnelse = "Begrunelse for avslag",
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
            )

        private val aktivitetererLæremidler =
            listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2024, 12, 10),
                    tom = LocalDate.of(2024, 12, 15),
                    faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
                ),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2024, 12, 10),
                    tom = LocalDate.of(2024, 12, 15),
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetLæremidler(
                            type = AktivitetType.UTDANNING,
                            harRettTilUtstyrsstipend = vurderingHarRettTilUtstyrsstipend(SvarJaNei.JA),
                            studienivå = Studienivå.VIDEREGÅENDE,
                        ),
                ),
            )

        private val målgrupperLæremidler: List<VilkårperiodeMålgruppe> =
            listOf(
                VilkårperiodeTestUtil.målgruppe(
                    begrunnelse = "målgruppe aap",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppeLæremidler(
                            type = MålgruppeType.AAP,
                            medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                            dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI),
                        ),
                    fom = LocalDate.of(2024, 2, 5),
                    tom = LocalDate.of(2024, 2, 10),
                ),
                VilkårperiodeTestUtil.målgruppe(
                    begrunnelse = "målgruppe os",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppeLæremidler(
                            type = MålgruppeType.OVERGANGSSTØNAD,
                            medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                            dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                        ),
                    fom = LocalDate.of(2024, 2, 5),
                    tom = LocalDate.of(2024, 2, 10),
                ),
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupperLæremidler,
                aktiviteter = aktivitetererLæremidler,
            )
    }

    object Boutgifter {
        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.BOUTGIFTER)

        val behandling =
            saksbehandling(
                behandling =
                    behandling(
                        id = behandlingId,
                        vedtakstidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                        opprettetTid = LocalDate.of(2024, 2, 5).atStartOfDay(),
                        fagsak = fagsak,
                        resultat = BehandlingResultat.INNVILGET,
                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    ),
                fagsak = fagsak,
            )

        val grunnlagsdata = GrunnlagsdataUtil.grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = emptyList()))

        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2024, JANUARY, 1),
                    tom = LocalDate.of(2024, FEBRUARY, 29),
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = MålgruppeType.AAP,
                ),
            )
        val beregningsresultat =
            BeregningsresultatBoutgifter(
                perioder =
                    listOf(
                        BeregningsresultatForLøpendeMåned(
                            grunnlag =
                                no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag(
                                    fom = LocalDate.of(2024, 1, 1),
                                    tom = LocalDate.of(2024, 1, 31),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                    makssats = 4953,
                                    makssatsBekreftet = true,
                                    målgruppe = MålgruppeType.AAP,
                                    aktivitet = AktivitetType.TILTAK,
                                    utgifter =
                                        mapOf(
                                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                                listOf(
                                                    UtgiftBeregningBoutgifter(
                                                        fom = LocalDate.of(2024, 1, 1),
                                                        tom = LocalDate.of(2024, 1, 31),
                                                        utgift = 3000,
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                        BeregningsresultatForLøpendeMåned(
                            grunnlag =
                                no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag(
                                    fom = LocalDate.of(2024, 2, 1),
                                    tom = LocalDate.of(2024, 2, 29),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                    makssats = 4953,
                                    makssatsBekreftet = true,
                                    målgruppe = MålgruppeType.AAP,
                                    aktivitet = AktivitetType.TILTAK,
                                    utgifter =
                                        mapOf(
                                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                                listOf(
                                                    UtgiftBeregningBoutgifter(
                                                        fom = LocalDate.of(2024, FEBRUARY, 1),
                                                        tom = LocalDate.of(2024, FEBRUARY, 2),
                                                        utgift = 3000,
                                                    ),
                                                ),
                                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                                listOf(
                                                    UtgiftBeregningBoutgifter(
                                                        fom = LocalDate.of(2024, FEBRUARY, 26),
                                                        tom = LocalDate.of(2024, FEBRUARY, 29),
                                                        utgift = 4000,
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
            )

        val vilkår =
            listOf(
                vilkår(
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    fom = LocalDate.of(2024, FEBRUARY, 1),
                    tom = LocalDate.of(2024, FEBRUARY, 2),
                    utgift = 3000,
                ),
                vilkår(
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    fom = LocalDate.of(2024, FEBRUARY, 26),
                    tom = LocalDate.of(2024, FEBRUARY, 29),
                    utgift = 4000,
                ),
            )

        val innvilgetVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseBoutgifter(
                        vedtaksperioder = vedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        begrunnelse = "Sånn her vil en begrunnelse se ut i det interne vedtaket",
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
            )

        private val aktivitetererBoutgifter =
            listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2024, 12, 10),
                    tom = LocalDate.of(2024, 12, 15),
                    faktaOgVurdering = faktaOgVurderingAktivitetBoutgifter(),
                ),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2024, 12, 10),
                    tom = LocalDate.of(2024, 12, 15),
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    begrunnelse = "ikke oppfylt",
                    faktaOgVurdering = faktaOgVurderingAktivitetBoutgifter(type = AktivitetType.UTDANNING),
                ),
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktivitetererBoutgifter,
            )
    }
}
