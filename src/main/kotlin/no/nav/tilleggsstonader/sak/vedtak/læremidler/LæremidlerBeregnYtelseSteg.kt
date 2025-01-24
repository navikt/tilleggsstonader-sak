package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortBeregningsresultatVedOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDomene
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.data.repository.findByIdOrNull
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

    override fun lagreVedtak(saksbehandling: Saksbehandling, vedtak: VedtakLæremidlerRequest) {
        when (vedtak) {
            is InnvilgelseLæremidlerRequest -> beregnOgLagreInnvilgelse(vedtak.vedtaksperioder.tilDomene(), saksbehandling)
            is AvslagLæremidlerDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørLæremidlerRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(vedtaksperioder: List<Vedtaksperiode>, saksbehandling: Saksbehandling) {
        feilHvis(saksbehandling.forrigeBehandlingId != null) {
            "Har foreløpig ikke støtte for innvilgelse av revurdering"
        }
        val beregningsresultat = beregningService.beregn(vedtaksperioder, saksbehandling.id)
        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling.id, vedtaksperioder, beregningsresultat))
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(saksbehandling: Saksbehandling, vedtak: OpphørLæremidlerRequest) {
        brukerfeilHvis(saksbehandling.forrigeBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        val forrigeBehandling = vedtakRepository.findByIdOrNull(saksbehandling.forrigeBehandlingId)
            ?: error("Opphør må ha en forrige behandling")
        if (saksbehandling.revurderFra == null) {
            error("revurderFra-dato er påkrevd for opphør")
        }
        feilHvis(forrigeBehandling.type == TypeVedtak.AVSLAG) {
            "Opphør er et ugyldig vedtaksresultat fordi forrige behandling var et avslag"
        }

        opphørValideringService.validerVilkårsPerioder(saksbehandling)

        val forrigeBehandlingData = forrigeBehandling.data
        val avkortetBeregningsresultat: List<BeregningsresultatForMåned> = avkortBeregningsresultatVedOpphør(forrigeBehandling, saksbehandling.revurderFra)

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data = OpphørLæremidler(
                    vedtaksperioder = emptyList(),
                    beregningsresultat = BeregningsresultatLæremidler(avkortetBeregningsresultat),
                    årsaker = vedtak.årsakerOpphør,
                    begrunnelse = vedtak.begrunnelse,
                ),
            ),
        )

        val beregningsresultatLæremidler = BeregningsresultatLæremidler(avkortetBeregningsresultat)

        lagreAndeler(saksbehandling, beregningsresultatLæremidler)
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagLæremidlerDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data = AvslagLæremidler(
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
        val andeler = beregningsresultat.perioder.groupBy { it.grunnlag.utbetalingsdato }
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
    ): Vedtak {
        return GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.INNVILGELSE,
            data = InnvilgelseLæremidler(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
            ),
        )
    }

    /**
     * Hvis utbetalingsmåneden er fremover i tid og det er nytt år så skal det ventes på satsendring før iverksetting.
     */
    private fun statusIverksettingForSatsBekreftet(satsBekreftet: Boolean): StatusIverksetting {
        if (!satsBekreftet) {
            return StatusIverksetting.VENTER_PÅ_SATS_ENDRING
        }

        return StatusIverksetting.UBEHANDLET
    }

    private fun MålgruppeType.tilTypeAndel(): TypeAndel {
        return when (this) {
            MålgruppeType.AAP, MålgruppeType.UFØRETRYGD, MålgruppeType.NEDSATT_ARBEIDSEVNE -> TypeAndel.LÆREMIDLER_AAP
            MålgruppeType.OVERGANGSSTØNAD -> TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER
            MålgruppeType.OMSTILLINGSSTØNAD -> TypeAndel.LÆREMIDLER_ETTERLATTE
            else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
        }
    }
}
