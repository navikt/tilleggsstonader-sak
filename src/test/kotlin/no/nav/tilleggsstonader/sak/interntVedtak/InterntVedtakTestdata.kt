package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTestdata.Boutgifter.vedtaksperioder
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadMetadata
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagFaktaGrunnlagPersonopplysninger
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.dummyAktivitetId
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.totrinnskontroll
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilSatsForDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatEkstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBilBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelsePassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.PassAvBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.PassAvBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagDrosjeForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagPrivatBilForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.ikkeOppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.BoutgifterRegelTestUtil.oppfylteDelvilkårUtgifterOvernatting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.ikkeOppfylteDelvilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.oppfylteDelvilkårDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseRegelTestUtil.oppfylteDelvilkårDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseTilSamlingRegelTestUtil.ikkeOppfylteDelvilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseTilSamlingRegelTestUtil.oppfylteDelvilkårReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.ReiseTilSamlingRegelTestUtil.oppfylteDelvilkårReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetDagligReiseTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetDagligReiseTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetPassAvBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetReiseTilSamlingTso
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetReiseTilSamlingTsr
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingHarRettTilUtstyrsstipend
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.temporal.ChronoUnit
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning as VedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.SvarJaNei as SvarJaNeiReiseTilSamling

object InterntVedtakTestdata {
    val behandlingId = BehandlingId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31")
    val vedtaksperiodeId = VedtaksperiodeId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e37")
    val vedtaksperiodeId2 = VedtaksperiodeId.fromString("9f13d9bf-a6b2-4024-8cf2-7d86148a4f31")
    val vedtaksperiodeId3 = VedtaksperiodeId.fromString("50d6c31e-db2e-4f55-9da1-30ddf4f27b2b")

    val førsteJan = 1 januar 2025
    val sisteJan = 31 januar 2025
    val førsteFeb = 1 februar 2025
    val sisteFeb = 28 februar 2025

    val opprettetTid = førsteJan.atStartOfDay()
    val vedtakstidspunkt = førsteFeb.atStartOfDay()

    val totrinnskontroll = totrinnskontroll(behandlingId, beslutter = "saksbeh2")

    val søknadMetadata =
        SøknadMetadata(
            journalpostId = "journalpostId",
            mottattTidspunkt = førsteJan.atStartOfDay().truncatedTo(ChronoUnit.MILLIS),
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
                        mottarSykepengerForFulltidsstilling = vurderingMottarSykepengerForFulltidsstilling(SvarJaNei.NEI_IMPLISITT),
                    ),
                fom = førsteJan,
                tom = sisteFeb,
            ),
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe os",
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = MålgruppeType.OVERGANGSSTØNAD,
                        medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                        dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                    ),
                fom = førsteJan,
                tom = sisteFeb,
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
                fom = førsteJan,
                tom = sisteFeb,
            ),
        )

    private val målgrupperTsr: List<VilkårperiodeMålgruppe> =
        listOf(
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgruppe dagpenger",
                faktaOgVurdering =
                    faktaOgVurderingMålgruppe(
                        type = MålgruppeType.DAGPENGER,
                        medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                        dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI),
                        aldersvilkår = vurderingAldersVilkår(),
                        mottarSykepengerForFulltidsstilling = vurderingMottarSykepengerForFulltidsstilling(SvarJaNei.NEI_IMPLISITT),
                    ),
                fom = førsteJan,
                tom = sisteFeb,
            ),
        )

    object PassAvBarn {
        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()))

        val behandling = dummySaksbehandling(fagsak = fagsak)

        val barn =
            listOf(
                GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "1", fødselsdato = førsteJan.minusYears(6)),
                GrunnlagsdataUtil.lagGrunnlagsdataBarn(ident = "2", fødselsdato = førsteJan.minusYears(10)),
            )

        val grunnlagsdata =
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = barn))

        val behandlingBarn =
            listOf(
                behandlingBarn(
                    personIdent =
                        grunnlagsdata.personopplysninger.barn
                            .first()
                            .ident,
                ),
                behandlingBarn(
                    personIdent =
                        grunnlagsdata.personopplysninger.barn
                            .last()
                            .ident,
                ),
            )
        val barnId = behandlingBarn[0].id
        val barnId2 = behandlingBarn[1].id

        val vilkår =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = oppfylteDelvilkårPassBarn(),
                    barnId = barnId,
                    fom = førsteJan,
                    tom = sisteJan,
                    utgift = 100,
                ),
                vilkår(
                    resultat = Vilkårsresultat.IKKE_OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = ikkeOppfylteDelvilkårPassBarn(),
                    barnId = barnId,
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgift = null,
                ),
                vilkår(
                    resultat = Vilkårsresultat.SLETTET,
                    behandlingId = behandlingId,
                    type = VilkårType.PASS_BARN,
                    delvilkår = oppfylteDelvilkårPassBarn(),
                    barnId = barnId2,
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgift = null,
                ).markerSlettet("slettet dette vilkår"),
            )

        val vedtaksperiodeBeregningsgrunnlag =
            VedtaksperiodeBeregningsgrunnlag(
                fom = førsteJan,
                tom = sisteFeb,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            )

        val vedtaksperiode =
            Vedtaksperiode(
                id = vedtaksperiodeId,
                fom = førsteJan,
                tom = førsteFeb,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            )

        val vedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelsePassAvBarn(
                        beregningsresultat =
                            BeregningsresultatPassAvBarn(
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
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = førsteJan,
                opphørsdato = null,
            )

        private val aktiviteterPassAvBarn =
            listOf(
                dummyAktivitet(
                    begrunnelse = "aktivitet abd",
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetPassAvBarn(
                            lønnet = vurderingLønnet(SvarJaNei.JA),
                        ),
                ),
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetPassAvBarn(),
                    slettetKommentar = "kommentar slettet",
                ),
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteterPassAvBarn,
            )
    }

    object Læremidler {
        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.LÆREMIDLER)

        val behandling =
            dummySaksbehandling(
                fagsak = fagsak,
            )

        val grunnlagsdata =
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = emptyList()))

        val vedtaksperioder =
            listOf(
                LæremidlerTestUtil.vedtaksperiode(
                    id = vedtaksperiodeId2,
                    fom = førsteJan,
                    tom = sisteFeb,
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
                                    fom = førsteJan,
                                    tom = sisteJan,
                                    utbetalingsdato = førsteJan,
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
                                    fom = førsteFeb,
                                    tom = sisteFeb,
                                    utbetalingsdato = førsteJan,
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
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = førsteJan,
                opphørsdato = null,
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
                tidligsteEndring = null,
                opphørsdato = null,
            )

        private val aktivitetererLæremidler =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetLæremidler(),
                ),
                dummyAktivitet(
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
                    fom = førsteJan,
                    tom = sisteFeb,
                ),
                VilkårperiodeTestUtil.målgruppe(
                    begrunnelse = "målgruppe os",
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppeLæremidler(
                            type = MålgruppeType.OVERGANGSSTØNAD,
                            medlemskap = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT),
                            dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = null),
                        ),
                    fom = førsteJan,
                    tom = sisteFeb,
                ),
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupperLæremidler,
                aktiviteter = aktivitetererLæremidler,
            )
    }

    object Boutgifter {
        val fomOvernatting1 = førsteJan
        val tomOvernatting1 = fomOvernatting1.plusDays(1)
        val fomOvernatting2 = førsteFeb
        val tomOvernatting2 = fomOvernatting2.plusDays(1)
        val fomOvernatting3 = førsteFeb.plusDays(25)
        val tomOvernatting3 = fomOvernatting3.plusDays(1)

        val fagsak = fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.BOUTGIFTER)

        val behandling = dummySaksbehandling(fagsak = fagsak)

        val grunnlagsdata =
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = emptyList()))

        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = fomOvernatting1,
                    tom = tomOvernatting1,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
                Vedtaksperiode(
                    id = vedtaksperiodeId3,
                    fom = fomOvernatting2,
                    tom = tomOvernatting2,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )
        val beregningsresultat =
            BeregningsresultatBoutgifter(
                perioder =
                    listOf(
                        BeregningsresultatForLøpendeMåned(
                            grunnlag =
                                no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag(
                                    fom = fomOvernatting1,
                                    tom = tomOvernatting1,
                                    makssats = 4953,
                                    makssatsBekreftet = true,
                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                    aktivitet = AktivitetType.TILTAK,
                                    utgifter =
                                        mapOf(
                                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                                listOf(
                                                    lagUtgiftBeregningBoutgifter(
                                                        fom = førsteJan,
                                                        tom = førsteJan,
                                                        utgift = 3000,
                                                    ),
                                                ),
                                        ),
                                ),
                            stønadsbeløp = 3000,
                        ),
                        BeregningsresultatForLøpendeMåned(
                            grunnlag =
                                no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag(
                                    fom = fomOvernatting2,
                                    tom = tomOvernatting2,
                                    makssats = 4953,
                                    makssatsBekreftet = true,
                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                    aktivitet = AktivitetType.TILTAK,
                                    utgifter =
                                        mapOf(
                                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                                listOf(
                                                    lagUtgiftBeregningBoutgifter(
                                                        fom = fomOvernatting2,
                                                        tom = tomOvernatting2,
                                                        utgift = 4000,
                                                    ),
                                                ),
                                        ),
                                ),
                            stønadsbeløp = 4000,
                        ),
                    ),
            )

        val vilkår =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    fom = fomOvernatting1,
                    tom = tomOvernatting1,
                    utgift = 3000,
                ),
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    fom = fomOvernatting2,
                    tom = tomOvernatting2,
                    utgift = 4000,
                ),
                vilkår(
                    resultat = Vilkårsresultat.IKKE_OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = ikkeOppfylteDelvilkårUtgifterOvernatting(),
                    fom = fomOvernatting3,
                    tom = tomOvernatting3,
                    utgift = null,
                ),
                vilkår(
                    resultat = Vilkårsresultat.SLETTET,
                    behandlingId = behandlingId,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    delvilkår = oppfylteDelvilkårUtgifterOvernatting(),
                    fom = fomOvernatting3,
                    tom = tomOvernatting3,
                    utgift = null,
                ).markerSlettet("slettet vilkår fordi reisen ble registrert på feil aktivitet"),
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
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = null,
            )
        val avslåttVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagBoutgifter(
                        årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON, ÅrsakAvslag.RETT_TIL_BOSTØTTE),
                        begrunnelse = "Begrunnelse for avslag",
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = null,
            )
        val opphørtVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørBoutgifter(
                        årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER, ÅrsakOpphør.ENDRING_MÅLGRUPPE),
                        begrunnelse = "Begrunnelse for opphør",
                        vedtaksperioder = vedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 10 desember 2024),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = 10 desember 2024,
            )

        private val aktivitetererBoutgifter =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetBoutgifter(type = AktivitetType.UTDANNING),
                ),
                dummyAktivitet(
                    begrunnelse = "ikke oppfylt",
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetBoutgifter(
                            type = AktivitetType.TILTAK,
                            lønnet =
                                vurderingLønnet(
                                    SvarJaNei.JA,
                                ),
                        ),
                ),
            )

        val vilkårperioder =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktivitetererBoutgifter,
            )
    }

    object DagligReise {
        private val reiseIdIkkeOppfylt = ReiseId.fromString("02c86eca-36e5-451b-a22d-8501a0f7b8de")

        val fagsak =
            fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.DAGLIG_REISE_TSO)
        val fagsakTsr =
            fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.DAGLIG_REISE_TSR)
        val behandling = dummySaksbehandling(fagsak = fagsak)
        val behandlingTsr = dummySaksbehandling(fagsak = fagsakTsr)

        private val aktivitetererDagligReiseTso =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTso(),
                ),
                dummyAktivitet(
                    begrunnelse = "ikke oppfylt",
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetDagligReiseTso(
                            type = AktivitetType.UTDANNING,
                            harUtgifter =
                                vurderingHarUtgifter(
                                    SvarJaNei.NEI,
                                ),
                        ),
                ),
            )

        private val aktivitetererDagligReiseTsr =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(),
                    tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                ),
            )

        val vilkårperioderTso =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktivitetererDagligReiseTso,
            )

        val vilkårperioderTsr =
            Vilkårperioder(
                målgrupper = målgrupperTsr,
                aktiviteter = aktivitetererDagligReiseTsr,
            )

        val grunnlagsdata =
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = emptyList()))

        val rammevedtakPrivatBil =
            RammevedtakPrivatBil(
                reiser =
                    listOf(
                        RammevedtakForReiseMedPrivatBil(
                            reiseId = dummyReiseId,
                            aktivitetsadresse = "Testveien 1, 1234 Testby",
                            aktivitetType = AktivitetType.TILTAK,
                            tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                            grunnlag =
                                RammevedtakForReiseMedPrivatBilBeregningsgrunnlag(
                                    fom = førsteJan,
                                    tom = sisteFeb,
                                    delperioder =
                                        listOf(
                                            RammeForReiseMedPrivatBilDelperiode(
                                                fom = førsteJan,
                                                tom = førsteJan.plusDays(25),
                                                reisedagerPerUke = 3,
                                                ekstrakostnader =
                                                    RammeForReiseMedPrivatEkstrakostnader(
                                                        bompengerPerDag = BigDecimal("40"),
                                                        fergekostnadPerDag = BigDecimal("50"),
                                                    ),
                                                satser =
                                                    listOf(
                                                        RammeForReiseMedPrivatBilSatsForDelperiode(
                                                            fom = førsteJan,
                                                            tom = førsteJan.plusDays(25),
                                                            satsBekreftetVedVedtakstidspunkt = true,
                                                            kilometersats = BigDecimal.valueOf(2.94),
                                                            dagsatsUtenParkering = BigDecimal.valueOf(100.0),
                                                        ),
                                                    ),
                                            ),
                                            RammeForReiseMedPrivatBilDelperiode(
                                                fom = førsteJan.plusDays(26),
                                                tom = sisteFeb,
                                                reisedagerPerUke = 5,
                                                ekstrakostnader =
                                                    RammeForReiseMedPrivatEkstrakostnader(
                                                        bompengerPerDag = BigDecimal("40"),
                                                        fergekostnadPerDag = BigDecimal("50"),
                                                    ),
                                                satser =
                                                    listOf(
                                                        RammeForReiseMedPrivatBilSatsForDelperiode(
                                                            fom = førsteJan.plusDays(26),
                                                            tom = sisteFeb,
                                                            satsBekreftetVedVedtakstidspunkt = true,
                                                            kilometersats = BigDecimal.valueOf(2.94),
                                                            dagsatsUtenParkering = BigDecimal.valueOf(100.0),
                                                        ),
                                                    ),
                                            ),
                                        ),
                                    reiseavstandEnVei = BigDecimal.valueOf(40.0),
                                    vedtaksperioder = vedtaksperioder,
                                ),
                        ),
                    ),
            )

        fun beregningsresultatTso() = beregningsresultat(brukersNavKontor = null)

        fun beregningsresultatTsr() = beregningsresultat(brukersNavKontor = "1234")

        fun beregningsresultat(brukersNavKontor: String?) =
            BeregningsresultatDagligReise(
                offentligTransport =
                    no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport(
                        reiser =
                            listOf(
                                BeregningsresultatForReise(
                                    reiseId = dummyReiseId,
                                    perioder =
                                        listOf(
                                            BeregningsresultatForPeriode(
                                                grunnlag =
                                                    BeregningsgrunnlagOffentligTransport(
                                                        fom = førsteFeb.plusDays(4),
                                                        tom = førsteFeb.plusDays(9),
                                                        prisEnkeltbillett = 80,
                                                        prisSyvdagersbillett = 400,
                                                        pris30dagersbillett = 1800,
                                                        antallReisedagerPerUke = 5,
                                                        vedtaksperioder = emptyList(),
                                                        antallReisedager = 5,
                                                        brukersNavKontor = brukersNavKontor,
                                                    ),
                                                beløp = 400,
                                                billettdetaljer = mapOf(Billettype.SYVDAGERSBILLETT to 1),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                privatBil =
                    BeregningsresultatPrivatBil(
                        reiser =
                            listOf(
                                BeregningsresultatForReisePrivatBil(
                                    reiseId = dummyReiseId,
                                    perioder =
                                        listOf(
                                            BeregningsresultatForReisePrivatBilPeriode(
                                                fom = førsteJan,
                                                tom = førsteJan.plusDays(25),
                                                grunnlag =
                                                    BeregningsresultatForReisePrivatBilGrunnlag(
                                                        dager =
                                                            listOf(
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(1),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(2),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(3),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                            ),
                                                    ),
                                                stønadsbeløp = BigDecimal(600),
                                                brukersNavKontor = brukersNavKontor,
                                                fraTidligereVedtak = false,
                                            ),
                                            BeregningsresultatForReisePrivatBilPeriode(
                                                fom = førsteJan.plusDays(26),
                                                tom = sisteFeb,
                                                grunnlag =
                                                    BeregningsresultatForReisePrivatBilGrunnlag(
                                                        dager =
                                                            listOf(
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(28),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(29),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteJan.plusDays(30),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteFeb.plusDays(5),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                                BeregningsresultatForReisePrivatBilDag(
                                                                    dato = førsteFeb.plusDays(6),
                                                                    parkeringskostnad = 100,
                                                                    dagsatsUtenParkering = 100.toBigDecimal(),
                                                                    stønadsbeløpForDag = BigDecimal(200),
                                                                ),
                                                            ),
                                                    ),
                                                stønadsbeløp = BigDecimal(1000),
                                                brukersNavKontor = brukersNavKontor,
                                                fraTidligereVedtak = false,
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )

        val vedtaksperioderTso =
            listOf(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = førsteJan,
                    tom = sisteFeb,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )

        val vedtaksperioderTsr =
            listOf(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = førsteJan,
                    tom = sisteFeb,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
                ),
            )

        fun innvilgetVedtakTso() = innvilgetVedtak(beregningsresultatTso(), vedtaksperioderTso)

        fun innvilgetVedtakTsr() = innvilgetVedtak(beregningsresultatTsr(), vedtaksperioderTsr)

        private fun innvilgetVedtak(
            beregningsresultatDagligReise: BeregningsresultatDagligReise,
            vedtaksperioder: List<Vedtaksperiode>,
        ) = GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelseDagligReise(
                    vedtaksperioder = vedtaksperioder,
                    beregningsresultat = beregningsresultatDagligReise,
                    rammevedtakPrivatBil = rammevedtakPrivatBil,
                    begrunnelse = "Sånn her vil en begrunnelse se ut i det interne vedtaket",
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
        )

        val avslåttVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                data =
                    AvslagDagligReise(
                        årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON),
                        begrunnelse = "Årsaken til avslaget",
                    ),
                type = TypeVedtak.AVSLAG,
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            )

        val vilkårOffentligTransport =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.DAGLIG_REISE,
                    delvilkår = oppfylteDelvilkårDagligReiseOffentligTransport(),
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgift = null,
                    fakta =
                        FaktaDagligReiseOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksgata 1",
                            reisedagerPerUke = 5,
                            prisEnkelbillett = 44,
                            prisSyvdagersbillett = 340,
                            prisTrettidagersbillett = 750,
                        ),
                ),
                vilkår(
                    resultat = Vilkårsresultat.IKKE_OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.DAGLIG_REISE,
                    delvilkår = ikkeOppfylteDelvilkårDagligReise(),
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgift = null,
                    fakta = FaktaDagligReiseUbestemt(reiseId = reiseIdIkkeOppfylt, adresse = null),
                ),
            )
        val vilkårPrivatBil =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.DAGLIG_REISE,
                    delvilkår = oppfylteDelvilkårDagligReisePrivatBil(),
                    fom = førsteJan,
                    tom = sisteFeb,
                    utgift = null,
                    fakta =
                        FaktaDagligReisePrivatBil(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksgata 1",
                            reiseavstandEnVei = BigDecimal(40.0),
                            faktaDelperioder =
                                listOf(
                                    FaktaDelperiodePrivatBil(
                                        fom = førsteJan,
                                        tom = 26 januar 2025,
                                        reisedagerPerUke = 3,
                                        bompengerPerDag = BigDecimal("40"),
                                        fergekostnadPerDag = BigDecimal("50"),
                                    ),
                                    FaktaDelperiodePrivatBil(
                                        fom = 27 januar 2025,
                                        tom = sisteFeb,
                                        reisedagerPerUke = 5,
                                        bompengerPerDag = BigDecimal("40"),
                                        fergekostnadPerDag = BigDecimal("50"),
                                    ),
                                ),
                            aktivitetId = dummyAktivitetId,
                        ),
                ),
            )
    }

    object ReiseTilSamling {
        private val reiseIdIkkeOppfylt = ReiseId.fromString("02c86eca-36e5-451b-a22d-8501a0f7b8df")

        val fagsak =
            fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
        val fagsakTsr =
            fagsak(eksternId = EksternFagsakId(1673L, FagsakId.random()), stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR)
        val behandling = dummySaksbehandling(fagsak = fagsak)
        val behandlingTsr = dummySaksbehandling(fagsak = fagsakTsr)
        private val aktivitetererReiseTilSamlingTso =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetReiseTilSamlingTso(type = AktivitetType.UTDANNING),
                ),
                dummyAktivitet(
                    begrunnelse = "ikke oppfylt",
                    faktaOgVurdering =
                        faktaOgVurderingAktivitetReiseTilSamlingTso(
                            type = AktivitetType.TILTAK,
                            lønnet =
                                vurderingLønnet(
                                    SvarJaNei.JA,
                                ),
                        ),
                ),
            )

        private val aktivitetererReiseTilSamlingTsr =
            listOf(
                dummyAktivitet(
                    faktaOgVurdering = faktaOgVurderingAktivitetReiseTilSamlingTsr(),
                    tiltaksvariant = TypeAktivitet.GRUPPEAMO,
                ),
            )

        val vilkårperioderTso =
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktivitetererReiseTilSamlingTso,
            )

        val vilkårperioderTsr =
            Vilkårperioder(
                målgrupper = målgrupperTsr,
                aktiviteter = aktivitetererReiseTilSamlingTsr,
            )

        val grunnlagsdata =
            lagGrunnlagsdata(personopplysninger = lagFaktaGrunnlagPersonopplysninger(barn = emptyList()))

        fun beregningsresultatTso() = beregningsresultat(brukersNavKontor = null)

        fun beregningsresultatTsr() = beregningsresultat(brukersNavKontor = "1234")

        fun beregningsresultat(brukersNavKontor: String?) =
            BeregningsresultatReiseTilSamling(
                offentligTransport =
                    listOf(
                        no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport(
                            reiseId = dummyReiseId,
                            grunnlag =
                                BeregningsgrunnlagOffentligTransportForSamling(
                                    adresse = "Testveien 1, 1234 Testby",
                                    fom = førsteJan,
                                    tom = førsteJan,
                                    vedtaksperioder = emptyList(),
                                    brukersNavKontor = null,
                                ),
                            beløp = BigDecimal(3000),
                            begrunnelse = "Begrunnelse for offentlig transport",
                            aktivitetId = dummyAktivitetId,
                        ),
                        no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport(
                            reiseId = dummyReiseId,
                            grunnlag =
                                BeregningsgrunnlagOffentligTransportForSamling(
                                    adresse = "Testveien 2, 2345 Testby",
                                    fom = førsteJan,
                                    tom = sisteJan,
                                    vedtaksperioder = emptyList(),
                                    brukersNavKontor = null,
                                ),
                            beløp = BigDecimal(3000),
                            begrunnelse = "Begrunnelse for offentlig transport",
                            aktivitetId = dummyAktivitetId,
                        ),
                    ),
                privatBil =
                    listOf(
                        no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil(
                            reiseId = dummyReiseId,
                            grunnlag =
                                BeregningsgrunnlagPrivatBilForSamling(
                                    adresse = "Testveien 2, 1234 Testby",
                                    fom = førsteJan,
                                    tom = sisteFeb,
                                    sats = BigDecimal(3.50),
                                    totalReiseavstand = BigDecimal(60.0),
                                    bompenger = BigDecimal(40),
                                    fergekostnad = BigDecimal(50),
                                    parkering = BigDecimal(40),
                                    piggdekkavgift = BigDecimal(40),
                                    vedtaksperioder = emptyList(),
                                    brukersNavKontor = null,
                                    satsBekreftet = true,
                                ),
                            beløp = BigDecimal(380),
                            aktivitetId = dummyAktivitetId,
                        ),
                    ),
                drosje =
                    listOf(
                        no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatDrosje(
                            reiseId = dummyReiseId,
                            grunnlag =
                                BeregningsgrunnlagDrosjeForSamling(
                                    adresse = "Testveien 2, 1234 Testby",
                                    fom = førsteJan,
                                    tom = sisteFeb,
                                    vedtaksperioder = emptyList(),
                                    brukersNavKontor = null,
                                ),
                            beløp = BigDecimal(380),
                            begrunnelse = "Begrunnelse for drosje",
                            aktivitetId = dummyAktivitetId,
                        ),
                    ),
            )

        val vedtaksperioderTso =
            listOf(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = førsteJan,
                    tom = sisteFeb,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )

        val vedtaksperioderTsr =
            listOf(
                Vedtaksperiode(
                    id = vedtaksperiodeId,
                    fom = førsteJan,
                    tom = sisteFeb,
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
                ),
            )

        fun innvilgetVedtakTso() = innvilgetVedtak(beregningsresultatTso(), vedtaksperioderTso)

        fun innvilgetVedtakTsr() = innvilgetVedtak(beregningsresultatTsr(), vedtaksperioderTsr)

        private fun innvilgetVedtak(
            beregningsresultatReiseTilSamling: BeregningsresultatReiseTilSamling,
            vedtaksperioder: List<Vedtaksperiode>,
        ) = GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelseReiseTilSamling(
                    vedtaksperioder = vedtaksperioder,
                    beregningsresultat = beregningsresultatReiseTilSamling,
                    begrunnelse = "Sånn her vil en begrunnelse se ut i det interne vedtaket",
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
        )

        val avslåttVedtak =
            GeneriskVedtak(
                behandlingId = behandlingId,
                data =
                    AvslagDagligReise(
                        årsaker = listOf(ÅrsakAvslag.MANGELFULL_DOKUMENTASJON),
                        begrunnelse = "Årsaken til avslaget",
                    ),
                type = TypeVedtak.AVSLAG,
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            )

        val vilkårOffentligTransport =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.REISE_TIL_SAMLING,
                    delvilkår = oppfylteDelvilkårReiseTilSamlingOffentligTransport(),
                    fom = LocalDate.of(2024, FEBRUARY, 1),
                    tom = LocalDate.of(2024, FEBRUARY, 2),
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksgata 1",
                            utgifterOffentligTransport = 2000.toBigDecimal(),
                            begrunnelse = "Begrunnelse",
                            aktivitetId = dummyAktivitetId,
                            nødvendigUtgift =
                                NødvendigUtgift(
                                    erUtgiftenNødvendig = SvarJaNeiReiseTilSamling.JA,
                                    begrunnelse = null,
                                ),
                        ),
                ),
                vilkår(
                    resultat = Vilkårsresultat.IKKE_OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.REISE_TIL_SAMLING,
                    delvilkår = ikkeOppfylteDelvilkårReiseTilSamling(),
                    fom = LocalDate.of(2024, FEBRUARY, 5),
                    tom = LocalDate.of(2024, FEBRUARY, 10),
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingUbestemt(
                            reiseId = reiseIdIkkeOppfylt,
                            adresse = "Tiltaksgata 2",
                        ),
                ),
            )
        val vilkårPrivatBil =
            listOf(
                vilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    behandlingId = behandlingId,
                    type = VilkårType.REISE_TIL_SAMLING,
                    delvilkår = oppfylteDelvilkårReiseTilSamlingPrivatBil(),
                    fom = LocalDate.of(2025, JANUARY, 1),
                    tom = LocalDate.of(2025, FEBRUARY, 28),
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingPrivatBil(
                            reiseId = dummyReiseId,
                            adresse = "Tiltaksgata 1",
                            reiseavstand = BigDecimal(40),
                            begrunnelse = "Begrunnelse",
                            aktivitetId = dummyAktivitetId,
                            bompenger = BigDecimal(40),
                            fergekostnad = BigDecimal(50),
                            parkering = BigDecimal(40),
                            piggdekkavgift = BigDecimal(50),
                            nødvendigUtgift =
                                NødvendigUtgift(
                                    erUtgiftenNødvendig = SvarJaNeiReiseTilSamling.NEI,
                                    begrunnelse = "Ferge nr 2 kosta mye mindre",
                                ),
                        ),
                ),
            )
    }

    private fun dummySaksbehandling(fagsak: Fagsak) =
        saksbehandling(
            behandling =
                behandling(
                    id = behandlingId,
                    vedtakstidspunkt = vedtakstidspunkt,
                    opprettetTid = opprettetTid,
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                ),
            fagsak = fagsak,
        )

    fun dummyAktivitet(
        faktaOgVurdering: AktivitetFaktaOgVurdering,
        begrunnelse: String? = null,
        slettetKommentar: String? = null,
        tiltaksvariant: TypeAktivitet? = null,
    ) = VilkårperiodeTestUtil.aktivitet(
        fom = førsteJan,
        tom = sisteFeb,
        begrunnelse = begrunnelse,
        resultat = if (slettetKommentar != null) ResultatVilkårperiode.SLETTET else faktaOgVurdering.utledResultat(),
        faktaOgVurdering = faktaOgVurdering,
        slettetKommentar = slettetKommentar,
        tiltaksvariant = tiltaksvariant,
    )
}
