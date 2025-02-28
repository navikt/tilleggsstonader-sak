package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.AvkortResult
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.læremidler.VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortBeregningsresultatVedOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortVedtaksperiodeVedOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDomene
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LæremidlerBeregnYtelseSteg(
    private val beregningService: LæremidlerBeregningService,
    private val opphørValideringService: OpphørValideringService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakLæremidlerRequest>(
        stønadstype = Stønadstype.LÆREMIDLER,
        vedtakRepository = vedtakRepository,
        tilkjentytelseService = tilkjentytelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakLæremidlerRequest,
    ) {
        when (vedtak) {
            is InnvilgelseLæremidlerRequest ->
                beregnOgLagreInnvilgelse(
                    vedtak.vedtaksperioder.tilDomene(),
                    saksbehandling,
                )

            is AvslagLæremidlerDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørLæremidlerRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        vedtaksperioder: List<Vedtaksperiode>,
        saksbehandling: Saksbehandling,
    ) {
        val forrigeVedtaksperioder = saksbehandling.forrigeBehandlingId?.let { hentVedtak(it).data.vedtaksperioder }
        val vedtaksperioderMedStatus =
            settStatusPåVedtaksperioder(
                vedtaksperioder = vedtaksperioder,
                vedtaksperioderForrigeBehandling = forrigeVedtaksperioder,
            )

        lagreVedtak(vedtaksperioderMedStatus, saksbehandling)
    }

    private fun hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<InnvilgelseEllerOpphørLæremidler> =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()

    private fun lagreVedtak(
        vedtaksperioder: List<Vedtaksperiode>,
        saksbehandling: Saksbehandling,
    ) {
        val beregningsresultat = beregningService.beregn(vedtaksperioder, saksbehandling.id)
        val nyttBeregningsresultat = settSammenGamleOgNyePerioder(saksbehandling, beregningsresultat)

        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling.id, vedtaksperioder, nyttBeregningsresultat))
        lagreAndeler(saksbehandling, nyttBeregningsresultat)
    }

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til og med revurder-fra
     * Bruker reberegnede perioder fra og med revurder-fra dato
     * Dette gjøres for at vi ikke skal reberegne perioder før revurder-fra datoet
     * Men vi trenger å reberegne perioder som løper i revurder-fra datoet då en periode kan ha endrer % eller sats
     */
    private fun settSammenGamleOgNyePerioder(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatLæremidler,
    ): BeregningsresultatLæremidler {
        if (saksbehandling.forrigeBehandlingId == null) {
            return beregningsresultat
        }
        val revurderFra = saksbehandling.revurderFra
        feilHvis(revurderFra == null) { "Behandling=$saksbehandling mangler revurderFra" }

        val forrigeBeregningsresultat = hentVedtak(saksbehandling.forrigeBehandlingId).data.beregningsresultat

        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat
                .perioder
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() < revurderFra }
        val nyePerioder =
            beregningsresultat.perioder
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() >= revurderFra }

        val nyePerioderMedKorrigertUtbetalingsdato = korrigerUtbetalingsdato(nyePerioder, forrigeBeregningsresultat)

        return BeregningsresultatLæremidler(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + nyePerioderMedKorrigertUtbetalingsdato,
        )
    }

    private fun korrigerUtbetalingsdato(
        nyePerioder: List<BeregningsresultatForMåned>,
        forrigeBeregningsresultat: BeregningsresultatLæremidler,
    ): List<BeregningsresultatForMåned> {
        val utbetalingsdatoPerMåned =
            forrigeBeregningsresultat
                .perioder
                .associate { it.grunnlag.fom.toYearMonth() to it.grunnlag.utbetalingsdato }

        return nyePerioder
            .map {
                val utbetalingsdato = utbetalingsdatoPerMåned[it.fom.toYearMonth()]
                if (utbetalingsdato != null) {
                    it.medKorrigertUtbetalingsdato(utbetalingsdato)
                } else {
                    it
                }
            }
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørLæremidlerRequest,
    ) {
        feilHvis(saksbehandling.forrigeBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(saksbehandling.revurderFra == null) {
            "revurderFra-dato er påkrevd for opphør"
        }
        val forrigeVedtak =
            vedtakRepository
                .findByIdOrThrow(saksbehandling.forrigeBehandlingId)
                .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()

        opphørValideringService.validerVilkårperioder(saksbehandling)

        val avkortetBeregningsresultat = avkortBeregningsresultatVedOpphør(forrigeVedtak, saksbehandling.revurderFra)
        val avkortetVedtaksperioder = avkortVedtaksperiodeVedOpphør(forrigeVedtak, saksbehandling.revurderFra)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            revurderFraDato = saksbehandling.revurderFra,
        )

        val beregningsresultat =
            beregningsresultatForOpphør(
                behandling = saksbehandling,
                avkortetBeregningsresultat = avkortetBeregningsresultat,
                avkortetVedtaksperioder = avkortetVedtaksperioder,
            )

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørLæremidler(
                        vedtaksperioder = avkortetVedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                    ),
            ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }

    /**
     * Hvis man har avkortet siste måneden må man reberegne den i tilfelle % på aktiviteter har endret seg
     * Eks at man hadde 2 aktiviteter, 50 og 100% som då gir 100%.
     * Etter opphør så har man kun 50% og då trenger å omberegne perioden
     */
    private fun beregningsresultatForOpphør(
        behandling: Saksbehandling,
        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        if (!avkortetBeregningsresultat.harAvkortetPeriode) {
            return BeregningsresultatLæremidler(avkortetBeregningsresultat.perioder)
        }

        return beregningsresultatOpphørMedReberegnetPeriode(
            behandling = behandling,
            avkortetBeregningsresultat = avkortetBeregningsresultat,
            avkortetVedtaksperioder = avkortetVedtaksperioder,
        )
    }

    private fun beregningsresultatOpphørMedReberegnetPeriode(
        behandling: Saksbehandling,
        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        val perioderSomBeholdes = avkortetBeregningsresultat.perioder.dropLast(1)
        val periodeTilReberegning = avkortetBeregningsresultat.perioder.last()

        val reberegnedePerioderKorrigertUtbetalingsdato =
            reberegnPerioderForOpphør(
                behandling = behandling,
                avkortetVedtaksperioder = avkortetVedtaksperioder,
                beregningsresultatTilReberegning = periodeTilReberegning,
            )

        return BeregningsresultatLæremidler(perioderSomBeholdes + reberegnedePerioderKorrigertUtbetalingsdato)
    }

    /**
     * Reberegner siste perioden då den er avkortet og ev skal få annet beløp utbetalt
     * Korrigerer utbetalingsdatoet då vedtaksperioden sånn at andelen som sen genereres for det utbetales på likt dato
     */
    private fun reberegnPerioderForOpphør(
        behandling: Saksbehandling,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        beregningsresultatTilReberegning: BeregningsresultatForMåned,
    ): List<BeregningsresultatForMåned> {
        val vedtaksperioderSomOmregnes =
            vedtaksperioderInnenforLøpendeMåned(avkortetVedtaksperioder, beregningsresultatTilReberegning)

        val reberegnedePerioder = beregningService.beregn(vedtaksperioderSomOmregnes, behandling.id).perioder
        feilHvisIkke(reberegnedePerioder.size <= 1) {
            "Når vi reberegner vedtaksperioder innenfor en måned burde vi få maks 1 reberegnet periode, faktiskAntall=${reberegnedePerioder.size}"
        }

        return reberegnedePerioder.map {
            it.medKorrigertUtbetalingsdato(beregningsresultatTilReberegning.grunnlag.utbetalingsdato)
        }
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagLæremidlerDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagLæremidler(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
            ),
        )
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatLæremidler,
    ) {
        val andeler =
            beregningsresultat.perioder
                .groupBy { it.grunnlag.utbetalingsdato }
                .entries
                .sortedBy { (utbetalingsdato, _) -> utbetalingsdato }
                .flatMap { (utbetalingsdato, perioder) ->
                    val førstePerioden = perioder.first()
                    val satsBekreftet = førstePerioden.grunnlag.satsBekreftet

                    feilHvisIkke(perioder.all { it.grunnlag.satsBekreftet == satsBekreftet }) {
                        "Alle perioder for et utbetalingsdato må være bekreftet eller ikke bekreftet"
                    }

                    mapTilAndeler(perioder, saksbehandling, utbetalingsdato, satsBekreftet)
                }
        tilkjentytelseService.opprettTilkjentYtelse(saksbehandling, andeler)
    }

    /**
     * Andeler grupperes per [TypeAndel], sånn at hvis man har 2 ulike målgrupper men som er av samme [TypeAndel]
     * så summeres beløpet sammen for disse 2 andelene
     * Hvis man har 2 [BeregningsresultatForMåned] med med 2 ulike [TypeAndel]
     * så blir det mappet til ulike andeler for at regnskapet i økonomi skal få riktig type for gitt utbetalingsmåned
     */
    private fun mapTilAndeler(
        perioder: List<BeregningsresultatForMåned>,
        saksbehandling: Saksbehandling,
        utbetalingsdato: LocalDate,
        satsBekreftet: Boolean,
    ) = perioder
        .groupBy { it.grunnlag.målgruppe.tilTypeAndel() }
        .map { (typeAndel, perioder) ->
            AndelTilkjentYtelse(
                beløp = perioder.sumOf { it.beløp },
                fom = utbetalingsdato,
                tom = utbetalingsdato,
                satstype = Satstype.DAG,
                type = typeAndel,
                kildeBehandlingId = saksbehandling.id,
                statusIverksetting = statusIverksettingForSatsBekreftet(satsBekreftet),
                utbetalingsdato = utbetalingsdato,
            )
        }

    private fun lagInnvilgetVedtak(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsresultat: BeregningsresultatLæremidler,
    ): Vedtak =
        GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelseLæremidler(
                    vedtaksperioder = vedtaksperioder,
                    beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
                ),
        )

    /**
     * Hvis utbetalingsmåneden er fremover i tid og det er nytt år så skal det ventes på satsendring før iverksetting.
     */
    private fun statusIverksettingForSatsBekreftet(satsBekreftet: Boolean): StatusIverksetting {
        if (!satsBekreftet) {
            return StatusIverksetting.VENTER_PÅ_SATS_ENDRING
        }

        return StatusIverksetting.UBEHANDLET
    }

    private fun MålgruppeType.tilTypeAndel(): TypeAndel =
        when (this) {
            MålgruppeType.AAP, MålgruppeType.UFØRETRYGD, MålgruppeType.NEDSATT_ARBEIDSEVNE -> TypeAndel.LÆREMIDLER_AAP
            MålgruppeType.OVERGANGSSTØNAD -> TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER
            MålgruppeType.OMSTILLINGSSTØNAD -> TypeAndel.LÆREMIDLER_ETTERLATTE
            else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
        }
}
