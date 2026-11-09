package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport as DagligReiseBeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport as DagligReiseBeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag as DagligReiseVedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag as LæremidlerBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned as LæremidlerBeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Aktivitet as PassAvBarnAktivitet
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beregningsgrunnlag as PassAvBarnBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatForMåned as PassAvBarnBeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.VedtaksperiodeGrunnlag as PassAvBarnVedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsgrunnlagOffentligTransport as OppstartBeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatOffentligTransport as OppstartBeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.VedtaksperiodeGrunnlag as OppstartVedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport as SamlingBeregningsresultatOffentligTransport

object VedtaksdataRepositoryTestdata {
    private val vedtaksperiodeId = VedtaksperiodeId.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val passAvBarnVedtaksperiodeId = VedtaksperiodeId.fromString("3580eb48-b676-49f1-9650-8b5062c2c604")
    private val barnId = BarnId.fromString("0ac59506-1685-40df-a004-ba176ee35edb")
    private val passAvBarnAktivitetId = java.util.UUID.fromString("5cac3675-f862-4302-94ea-4b9a3c453079")
    private val dagligReisePeriodeId = VedtaksperiodeId.fromString("d4c1944d-3ee5-435e-9618-8ae667b965a7")
    private val samlingPeriodeId = VedtaksperiodeId.fromString("29dc9897-4d0c-442a-a101-95295ac10111")
    private val samlingAktivitetId = VilkårperiodeGlobalId.fromString("29dc9897-4d0c-442a-a101-95295ac10222")
    private val reiseId = ReiseId.fromString("01a01909-7c2e-72dc-a318-f8c08154a603")
    private val dagligReiseId = ReiseId.fromString("02c86eca-36e5-451b-a22d-8501a0f7b8dd")

    private val passAvBarnBeregningsresultat =
        BeregningsresultatPassAvBarn(
            perioder =
                listOf(
                    PassAvBarnBeregningsresultatForMåned(
                        dagsats = BigDecimal("10.11"),
                        månedsbeløp = 1000,
                        grunnlag =
                            PassAvBarnBeregningsgrunnlag(
                                måned = java.time.YearMonth.of(2024, 1),
                                makssats = 200,
                                vedtaksperiodeGrunnlag =
                                    listOf(
                                        PassAvBarnVedtaksperiodeGrunnlag(
                                            vedtaksperiode =
                                                VedtaksperiodeBeregning(
                                                    fom = 1 januar 2024,
                                                    tom = 2 januar 2024,
                                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                                    aktivitet = AktivitetType.TILTAK,
                                                ),
                                            aktiviteter =
                                                listOf(
                                                    PassAvBarnAktivitet(
                                                        id = passAvBarnAktivitetId,
                                                        type = AktivitetType.TILTAK,
                                                        fom = 1 januar 2024,
                                                        tom = 2 januar 2024,
                                                        aktivitetsdager = 3,
                                                    ),
                                                ),
                                            antallDager = 2,
                                        ),
                                    ),
                                utgifter = listOf(UtgiftBarn(barnId = barnId, utgift = 3000)),
                                utgifterTotal = 3000,
                                antallBarn = 1,
                            ),
                        beløpsperioder =
                            listOf(
                                Beløpsperiode(
                                    dato = 1 januar 2024,
                                    beløp = 10,
                                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                ),
                            ),
                    ),
                ),
        )

    private val passAvBarnVedtaksperioder =
        listOf(
            Vedtaksperiode(
                id = passAvBarnVedtaksperiodeId,
                fom = 1 januar 2024,
                tom = 2 januar 2024,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        )

    private val læremidlerVedtaksperioder =
        listOf(
            LæremidlerTestUtil.vedtaksperiode(
                id = vedtaksperiodeId,
                fom = 1 januar 2024,
                tom = 31 januar 2024,
            ),
        )

    private val læremidlerBeregningsresultat =
        BeregningsresultatLæremidler(
            perioder =
                listOf(
                    LæremidlerBeregningsresultatForMåned(
                        beløp = 875,
                        grunnlag =
                            LæremidlerBeregningsgrunnlag(
                                fom = 1 januar 2024,
                                tom = 31 januar 2024,
                                utbetalingsdato = 1 januar 2024,
                                studienivå = Studienivå.HØYERE_UTDANNING,
                                studieprosent = 100,
                                sats = 875,
                                satsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                    ),
                ),
        )

    private val boutgifterVedtaksperiode =
        Vedtaksperiode(
            id = vedtaksperiodeId,
            fom = 1 januar 2024,
            tom = 31 januar 2024,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    private val boutgifterInnvilgelseBeregningsresultat =
        BeregningsresultatBoutgifter(
            perioder =
                listOf(
                    BoutgifterTestUtil.lagBeregningsresultatMåned(
                        fom = 1 januar 2024,
                        tom = 31 januar 2024,
                        utgifter =
                            mapOf(
                                TypeBoutgift.UTGIFTER_OVERNATTING to
                                    listOf(
                                        BoutgifterTestUtil.lagUtgiftBeregningBoutgifter(
                                            fom = 1 januar 2024,
                                            tom = 20 januar 2024,
                                            utgift = 2000,
                                        ),
                                    ),
                            ),
                    ),
                ),
        )

    private val boutgifterOpphørBeregningsresultat =
        BeregningsresultatBoutgifter(
            perioder =
                listOf(
                    BoutgifterTestUtil.lagBeregningsresultatMåned(
                        fom = 1 januar 2025,
                        tom = 31 januar 2025,
                        utgifter =
                            mapOf(
                                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                    listOf(
                                        BoutgifterTestUtil.lagUtgiftBeregningBoutgifter(
                                            fom = 1 januar 2025,
                                            tom = 31 januar 2025,
                                            utgift = 2500,
                                        ),
                                    ),
                            ),
                    ),
                ),
        )

    private val boutgifterOpphørVedtaksperiode =
        boutgifterVedtaksperiode.copy(fom = 1 januar 2025, tom = 31 januar 2025)

    private val dagligReiseVedtaksperiode =
        Vedtaksperiode(
            id = dagligReisePeriodeId,
            fom = 21 august 2025,
            tom = 31 august 2025,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    private val dagligReiseBeregningsresultat =
        BeregningsresultatDagligReise(
            offentligTransport =
                DagligReiseBeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            BeregningsresultatForReise(
                                reiseId = dagligReiseId,
                                perioder =
                                    listOf(
                                        BeregningsresultatForPeriode(
                                            grunnlag =
                                                DagligReiseBeregningsgrunnlagOffentligTransport(
                                                    fom = 21 august 2025,
                                                    tom = 31 august 2025,
                                                    prisEnkeltbillett = 44,
                                                    prisSyvdagersbillett = null,
                                                    pris30dagersbillett = 750,
                                                    antallReisedagerPerUke = 4,
                                                    vedtaksperioder =
                                                        listOf(
                                                            DagligReiseVedtaksperiodeGrunnlag(
                                                                id = dagligReisePeriodeId,
                                                                fom = 21 august 2025,
                                                                tom = 31 august 2025,
                                                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                                                aktivitet = AktivitetType.TILTAK,
                                                                antallReisedagerIVedtaksperioden = 6,
                                                            ),
                                                        ),
                                                    antallReisedager = 6,
                                                    brukersNavKontor = null,
                                                ),
                                            beløp = 528,
                                            billettdetaljer = mapOf(Billettype.ENKELTBILLETT to 12),
                                        ),
                                    ),
                            ),
                        ),
                ),
            privatBil = null,
        )

    private val samlingVedtaksperiode =
        Vedtaksperiode(
            id = samlingPeriodeId,
            fom = 20 august 2026,
            tom = 22 august 2026,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    private val samlingBeregningsresultat =
        BeregningsresultatReiseTilSamling(
            offentligTransport =
                listOf(
                    SamlingBeregningsresultatOffentligTransport(
                        reiseId = reiseId,
                        grunnlag =
                            BeregningsgrunnlagOffentligTransportForSamling(
                                adresse = "rfg",
                                fom = 20 august 2026,
                                tom = 22 august 2026,
                                vedtaksperioder =
                                    listOf(
                                        no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.VedtaksperiodeGrunnlag(
                                            id = samlingPeriodeId,
                                            fom = 20 august 2026,
                                            tom = 22 august 2026,
                                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                            aktivitet = AktivitetType.TILTAK,
                                        ),
                                    ),
                                brukersNavKontor = null,
                            ),
                        beløp = BigDecimal("245365"),
                        begrunnelse = "diurgierhug",
                        aktivitetId = null,
                    ),
                ),
            privatBil = emptyList(),
        )

    private val oppstartVedtaksperiode =
        Vedtaksperiode(
            id = samlingPeriodeId,
            fom = 20 august 2026,
            tom = 22 august 2026,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    private val oppstartBeregningsresultat =
        BeregningReiseOppstartAvslutningHjemreise(
            offentligTransport =
                listOf(
                    OppstartBeregningsresultatOffentligTransport(
                        reiseId = reiseId,
                        grunnlag =
                            OppstartBeregningsgrunnlagOffentligTransport(
                                adresse = "rfg",
                                fom = 20 august 2026,
                                tom = 20 august 2026,
                                vedtaksperioder =
                                    listOf(
                                        OppstartVedtaksperiodeGrunnlag(
                                            id = samlingPeriodeId,
                                            fom = 20 august 2026,
                                            tom = 22 august 2026,
                                        ),
                                    ),
                            ),
                        beløp = BigDecimal("245365"),
                        aktivitetId = samlingAktivitetId,
                    ),
                ),
            privatBil = emptyList(),
        )

    fun forType(type: TypeVedtaksdata): Vedtaksdata =
        when (type) {
            TypeVedtakPassAvBarn.INNVILGELSE_TILSYN_BARN ->
                InnvilgelsePassAvBarn(
                    beregningsresultat = passAvBarnBeregningsresultat,
                    vedtaksperioder = passAvBarnVedtaksperioder,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    begrunnelse = "Begrunnelse for vedtaksperiodene",
                )

            TypeVedtakPassAvBarn.AVSLAG_TILSYN_BARN ->
                AvslagPassAvBarn(listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE), "En begrunnelse")

            TypeVedtakPassAvBarn.OPPHØR_TILSYN_BARN ->
                OpphørPassAvBarn(
                    beregningsresultat = passAvBarnBeregningsresultat,
                    årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                    begrunnelse = "En begrunnelse",
                    vedtaksperioder = emptyList(),
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 1 januar 2024),
                )

            TypeVedtakLæremidler.INNVILGELSE_LÆREMIDLER ->
                InnvilgelseLæremidler(
                    vedtaksperioder = læremidlerVedtaksperioder,
                    beregningsresultat = læremidlerBeregningsresultat,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    begrunnelse = "En begrunnelse",
                )

            TypeVedtakLæremidler.AVSLAG_LÆREMIDLER ->
                AvslagLæremidler(listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE), "En begrunnelse")

            TypeVedtakLæremidler.OPPHØR_LÆREMIDLER ->
                OpphørLæremidler(
                    vedtaksperioder = læremidlerVedtaksperioder,
                    beregningsresultat = læremidlerBeregningsresultat,
                    årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                    begrunnelse = "En begrunnelse",
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 1 januar 2024),
                )

            TypeVedtakBoutgifter.INNVILGELSE_BOUTGIFTER ->
                InnvilgelseBoutgifter(
                    beregningsresultat = boutgifterInnvilgelseBeregningsresultat,
                    vedtaksperioder = listOf(boutgifterVedtaksperiode),
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    begrunnelse = "En begrunnelse",
                )

            TypeVedtakBoutgifter.AVSLAG_BOUTGIFTER ->
                AvslagBoutgifter(listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE), "En begrunnelse")

            TypeVedtakBoutgifter.OPPHØR_BOUTGIFTER ->
                OpphørBoutgifter(
                    vedtaksperioder = listOf(boutgifterOpphørVedtaksperiode),
                    beregningsresultat = boutgifterOpphørBeregningsresultat,
                    årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                    begrunnelse = "En begrunnelse for opphøret",
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 1 januar 2025),
                )

            TypeVedtakDagligReise.INNVILGELSE_DAGLIG_REISE ->
                InnvilgelseDagligReise(
                    beregningsresultat = dagligReiseBeregningsresultat,
                    rammevedtakPrivatBil = null,
                    vedtaksperioder = listOf(dagligReiseVedtaksperiode),
                    begrunnelse = "En begrunnelse",
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                )

            TypeVedtakDagligReise.AVSLAG_DAGLIG_REISE ->
                AvslagDagligReise(listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE), "En begrunnelse")

            TypeVedtakDagligReise.OPPHØR_DAGLIG_REISE ->
                OpphørDagligReise(
                    vedtaksperioder = listOf(dagligReiseVedtaksperiode),
                    beregningsresultat = dagligReiseBeregningsresultat,
                    rammevedtakPrivatBil = null,
                    årsaker = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "En begrunnelse",
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 21 august 2025),
                )

            TypeVedtakReiseTilSamling.INNVILGELSE_REISE_TIL_SAMLING ->
                InnvilgelseReiseTilSamling(
                    beregningsresultat = samlingBeregningsresultat,
                    vedtaksperioder = listOf(samlingVedtaksperiode),
                    begrunnelse = null,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                )

            TypeVedtakReiseTilSamling.AVSLAG_REISE_TIL_SAMLING ->
                AvslagReiseTilSamling(listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE), "En begrunnelse")

            TypeVedtakReiseTilSamling.OPPHØR_REISE_TIL_SAMLING ->
                OpphørReiseTilSamling(
                    beregningsresultat = samlingBeregningsresultat,
                    vedtaksperioder = listOf(samlingVedtaksperiode),
                    årsaker = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "En begrunnelse",
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 21 august 2026),
                )

            TypeVedtakReiseOppstartAvslutningHjemreise.INNVILGELSE_REISE_OPPSTART_AVSLUTNING_HJEMREISE ->
                InnvilgelseReiseOppstartAvslutningHjemreise(
                    beregningsresultat = oppstartBeregningsresultat,
                    vedtaksperioder = listOf(oppstartVedtaksperiode),
                    begrunnelse = null,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                )

            TypeVedtakFlytting.INNVILGELSE_FLYTTING -> innvilgelseFlytting()
            TypeVedtakFlytting.AVSLAG_FLYTTING ->
                AvslagFlytting(listOf(ÅrsakAvslag.INGEN_AKTIVITET), "En begrunnelse")

            TypeVedtakFlytting.OPPHØR_FLYTTING ->
                OpphørFlytting(
                    vedtaksperioder = emptyList(),
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 2 januar 2026),
                    beregningsresultat = BeregningsresultatFlytting(emptyList()),
                    årsaker = listOf(ÅrsakOpphør.ENDRING_AKTIVITET),
                    begrunnelse = "Opphørsbegrunnelse",
                )
        }

    private fun innvilgelseFlytting() =
        InnvilgelseFlytting(
            vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        id = vedtaksperiodeId,
                        fom = 1 januar 2026,
                        tom = 31 januar 2026,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = null,
                    ),
                ),
            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            beregningsresultat =
                BeregningsresultatFlytting(
                    resultater =
                        listOf(
                            BeregningsresultatFlyttevilkår(
                                fom = 1 januar 2026,
                                tom = 31 januar 2026,
                                beløp = BigDecimal("10000"),
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                grunnlag =
                                    BeregningsgrunnlagFlyttebyrå(
                                        tilbud =
                                            listOf(
                                                BeregningsgrunnlagFlyttebyråTilbud("Syntetisk tilbud 1", BigDecimal("10000"), true),
                                                BeregningsgrunnlagFlyttebyråTilbud("Syntetisk tilbud 2", BigDecimal("12000"), false),
                                            ),
                                        erUtgifterDokumentert = true,
                                    ),
                            ),
                        ),
                ),
            begrunnelse = null,
        )
}
