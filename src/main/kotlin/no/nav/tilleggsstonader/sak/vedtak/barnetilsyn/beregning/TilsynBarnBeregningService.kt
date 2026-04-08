package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningValideringUtil.validerPerioderForInnvilgelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.domain.BeregningsgrunnlagUtils
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.domain.Uke
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitFra
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.tilUke
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
val DEKNINGSGRAD_TILSYN_BARN = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtakRepository: VedtakRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val vedtaksperiodeValidingerService: VedtaksperiodeValideringService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        plan: Beregningsplan,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }

        return if (plan.omfang == Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT) {
            hentForrigeBeregningsresultat(behandling)
        } else {
            beregnFra(vedtaksperioder, behandling, typeVedtak, plan.beregnFra())
        }
    }

    private fun beregnFra(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        beregnFra: LocalDate?,
    ): BeregningsresultatTilsynBarn {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifterPerBarn)

        vedtaksperiodeValidingerService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFra(beregnFra)

        val perioder = beregnAktuellePerioder(behandling, typeVedtak, vedtaksperioderBeregning, beregnFra)
        val relevantePerioderFraForrigeVedtak =
            finnRelevantePerioderFraForrigeVedtak(behandling, beregnFra)

        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    private fun hentForrigeBeregningsresultat(behandling: Saksbehandling): BeregningsresultatTilsynBarn {
        val forrigeBehandlingId =
            requireNotNull(behandling.forrigeIverksatteBehandlingId) {
                "Kan ikke hente forrige beregningsresultat uten forrige iverksatt behandling"
            }
        return vedtakRepository
            .findByIdOrThrow(forrigeBehandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørTilsynBarn>()
            .data
            .beregningsresultat
    }

    private fun beregnAktuellePerioder(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        beregnFra: LocalDate?,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)

        val aktiviteter = finnAktiviteter(behandling.id)

        validerPerioderForInnvilgelse(vedtaksperioder, utgifterPerBarn, typeVedtak)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagPerMåned(vedtaksperioder, aktiviteter, utgifterPerBarn)
                .brukPerioderFraOgMedDato(beregnFra)
        return beregn(beregningsgrunnlag)
    }

    private fun finnRelevantePerioderFraForrigeVedtak(
        behandling: Saksbehandling,
        beregnFra: LocalDate?,
    ): List<BeregningsresultatForMåned> =
        behandling.forrigeIverksatteBehandlingId?.let {
            val beregningsresultat = hentForrigeBeregningsresultat(behandling)
            val beregnFraMåned = beregnFra?.toYearMonth() ?: YEAR_MONTH_MIN

            beregningsresultat.perioder.filter { it.grunnlag.måned < beregnFraMåned }
        } ?: emptyList()

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<BeregningsresultatForMåned> =
        beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            BeregningsresultatForMåned(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }

    private fun lagBeløpsperioder(
        dagsats: BigDecimal,
        it: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        it.vedtaksperiodeGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.vedtaksperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.vedtaksperiode.målgruppe,
            )
        }

    private fun beregnBeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ) = dagsats
        .multiply(antallDager.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes =
            utgifter
                .multiply(DEKNINGSGRAD_TILSYN_BARN)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun lagBeregningsgrunnlagPerMåned(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val vedtaksperioderPerMåned = vedtaksperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return vedtaksperioderPerMåned.entries.mapNotNull { (måned, vedtaksperiode) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                val vedtaksperiodeGrunnlag = finnVedtaksperiodeGrunnlag(vedtaksperiode, aktiviteterForMåned)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    vedtaksperiodeGrunnlag = vedtaksperiodeGrunnlag,
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnVedtaksperiodeGrunnlag(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<VedtaksperiodeGrunnlag> {
        val aktiviteterPerUke = aktiviteter.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return vedtaksperioder.map { vedtaksperiode ->
            val relevanteAktiviteter = finnAktiviteterForVedtaksperiode(vedtaksperiode, aktiviteter)

            VedtaksperiodeGrunnlag(
                vedtaksperiode = vedtaksperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = antallDager(vedtaksperiode, aktiviteterPerUke),
            )
        }
    }

    private fun finnAktiviteterForVedtaksperiode(
        vedtaksperiode: VedtaksperiodeBeregning,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> =
        aktiviteter[vedtaksperiode.aktivitet]?.filter { it.overlapper(vedtaksperiode) }
            ?: error(
                "Finnes ingen aktiviteter av type ${vedtaksperiode.aktivitet} som passer med vedtaksperiode med fom=${vedtaksperiode.fom} og tom=${vedtaksperiode.tom}",
            )

    private fun antallDager(
        vedtaksperiode: VedtaksperiodeBeregning,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val vedtaksperioderUker = vedtaksperiode.tilUke()
        val aktiviteterPerUke =
            aktiviteterPerType[vedtaksperiode.aktivitet]
                ?: error("Finner ikke aktiviteter for ${vedtaksperiode.aktivitet}")

        return vedtaksperioderUker
            .map { (uke, periode) ->
                val aktiviteterForUke =
                    aktiviteterPerUke[uke]
                        ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

                BeregningsgrunnlagUtils.beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }

    /**
     * Ved revurdering fra midt i en måned må hele den måneden beregnes på nytt, ikke bare fra beregn fra-datoen.
     *
     * Bakgrunn: En måneds støtte utbetales som én andel, der hele beløpet knyttes til 1. i måneden.
     * Eksempel: Innvilgelse for august gir én andel: fom=1.aug, tom=1.aug, beløp=1000kr.
     *
     * Hvis man revurderer fra 15. august (f.eks. fordi målgruppe eller aktivitetsdager endres),
     * og 500kr allerede er utbetalt for 1.–14. august, vil beregningen produsere to nye andeler:
     *   - 1.–14. aug: 500kr  (det som allerede er utbetalt)
     *   - 15.–31. aug: 700kr (det nye beløpet)
     *
     * For å få riktige andeler må beregningen starte fra begynnelsen av måneden,
     * selv om revurderingen starter midt i måneden.
     */
    private fun List<Beregningsgrunnlag>.brukPerioderFraOgMedDato(fraOgMedDato: LocalDate?): List<Beregningsgrunnlag> {
        val beregnFraMåned = fraOgMedDato?.toYearMonth() ?: return this

        return this.filter { it.måned >= beregnFraMåned }
    }

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
}
