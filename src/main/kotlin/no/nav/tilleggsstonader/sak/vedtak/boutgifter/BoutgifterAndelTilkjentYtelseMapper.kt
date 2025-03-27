package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2

object BoutgifterAndelTilkjentYtelseMapper {
    fun finnAndelTilkjentYtelse(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatBoutgifter,
    ): List<AndelTilkjentYtelse> {
        val andeler =
            beregningsresultat.perioder
                .groupBy { it.grunnlag.utbetalingsdato }
                .entries
                .sortedBy { (utbetalingsdato, _) -> utbetalingsdato }
                .flatMap { (utbetalingsdato, perioder) ->
                    val førstePerioden = perioder.first()
                    val satsBekreftet = førstePerioden.grunnlag.makssatsBekreftet

                    feilHvisIkke(perioder.all { it.grunnlag.makssatsBekreftet == satsBekreftet }) {
                        "Alle perioder for et utbetalingsdato må være bekreftet eller ikke bekreftet"
                    }

                    mapTilAndeler(perioder, saksbehandling, utbetalingsdato, satsBekreftet)
                }
        return andeler
    }

    /**
     * Andeler grupperes per [TypeAndel], sånn at hvis man har 2 ulike målgrupper men som er av samme [TypeAndel]
     * så summeres beløpet sammen for disse 2 andelene
     * Hvis man har 2 [BeregningsresultatForLøpendeMåned] med med 2 ulike [TypeAndel]
     * så blir det mappet til ulike andeler for at regnskapet i økonomi skal få riktig type for gitt utbetalingsmåned
     */
    private fun mapTilAndeler(
        perioder: List<BeregningsresultatForLøpendeMåned>,
        saksbehandling: Saksbehandling,
        utbetalingsdato: LocalDate,
        satsBekreftet: Boolean,
    ) = perioder
        .groupBy { it.grunnlag.målgruppe.tilTypeAndel(Stønadstype.BOUTGIFTER) }
        .map { (typeAndel, perioder) ->
            AndelTilkjentYtelse(
                beløp = perioder.sumOf { it.stønadsbeløp },
                fom = utbetalingsdato,
                tom = utbetalingsdato,
                satstype = Satstype.DAG,
                type = typeAndel,
                kildeBehandlingId = saksbehandling.id,
                statusIverksetting = statusIverksettingForSatsBekreftet(satsBekreftet),
                utbetalingsdato = utbetalingsdato,
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
}
