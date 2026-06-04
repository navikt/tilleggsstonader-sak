package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OpphørGirDegPengerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `opphør etter satsjustering beholder perioder fra tidligere vedtak og gir ikke dobbel utbetaling i siste måned`() {
        // 1. gangsbehandling 19.08.25 - 30.06.26
        // satsjustering 01.01.26
        val (innvilgelseId, satsjusteringId) = gjennomførInnvilgelseOgSatsjustering()

        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(satsjusteringId) {
                vedtak {
                    opphør(opphørsdato = 1 april 2026)
                }
            }
        testoppsettService.settAndelerTilOkForBehandling(testoppsettService.hentBehandling(opphørId))

        val tilkjentYtelseInnvilgelse = tilkjentYtelseRepository.findByBehandlingId(innvilgelseId)
        val tilkjentYtelseSatsjustering = tilkjentYtelseRepository.findByBehandlingId(satsjusteringId)
        val tilkjentYtelseOpphør = tilkjentYtelseRepository.findByBehandlingId(opphørId)

        assertThat(tilkjentYtelseInnvilgelse?.andelerTilkjentYtelse).hasSize(11)
        assertThat(tilkjentYtelseSatsjustering?.andelerTilkjentYtelse).hasSize(11)
        assertThat(tilkjentYtelseOpphør?.andelerTilkjentYtelse).hasSize(8)

        val beregningsresultatSatsjustering = vedtakRepository.findByIdOrThrow(satsjusteringId)

        val forventetBeregningsperioderEtterSatsjustering =
            listOf(
                Datoperiode(19 august 2025, 18 september 2025),
                Datoperiode(19 september 2025, 18 oktober 2025),
                Datoperiode(19 oktober 2025, 18 november 2025),
                Datoperiode(19 november 2025, 18 desember 2025),
                Datoperiode(19 desember 2025, 18 januar 2026),
                Datoperiode(19 januar 2026, 18 februar 2026),
                Datoperiode(19 februar 2026, 18 mars 2026),
                Datoperiode(19 mars 2026, 18 april 2026),
                Datoperiode(19 april 2026, 18 mai 2026),
                Datoperiode(19 mai 2026, 18 juni 2026),
                Datoperiode(19 juni 2026, 30 juni 2026),
            )

        val beregningsperioderEtterSatsjustering =
            (beregningsresultatSatsjustering.data as InnvilgelseEllerOpphørBoutgifter)
                .beregningsresultat
                .perioder
                .map {
                    Datoperiode(
                        it.fom,
                        it.tom,
                    )
                }

        assertThat(beregningsperioderEtterSatsjustering).isEqualTo(forventetBeregningsperioderEtterSatsjustering)

        val beregningsresultatOpphør = vedtakRepository.findByIdOrThrow(opphørId)

        val forventetBeregningsperioderEtterOpphør =
            listOf(
                Datoperiode(19 august 2025, 18 september 2025),
                Datoperiode(19 september 2025, 18 oktober 2025),
                Datoperiode(19 oktober 2025, 18 november 2025),
                Datoperiode(19 november 2025, 18 desember 2025),
                Datoperiode(19 desember 2025, 18 januar 2026),
                Datoperiode(19 januar 2026, 18 februar 2026),
                Datoperiode(19 februar 2026, 18 mars 2026),
                Datoperiode(19 mars 2026, 31 mars 2026),
            )

        val beregningsperioderEtterOpphør =
            (beregningsresultatOpphør.data as InnvilgelseEllerOpphørBoutgifter)
                .beregningsresultat
                .perioder
                .map {
                    Datoperiode(
                        it.fom,
                        it.tom,
                    )
                }

        assertThat(beregningsperioderEtterOpphør).isEqualTo(forventetBeregningsperioderEtterOpphør)
    }

    @Test
    fun `opphør på sak med satsjustering feilen skal ta vare på eksisterene perioder`() {
        val (_, satsjusteringId) = gjennomførInnvilgelseOgSatsjustering()

        // Simuler buggy satsjustering-data fra prod med nye perioder fra 1. januar 2026
        injectBuggySatsjusteringData(satsjusteringId)

        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(satsjusteringId) {
                vedtak {
                    opphør(opphørsdato = 1 april 2026)
                }
            }

        val tilkjentYtelseOpphør = tilkjentYtelseRepository.findByBehandlingId(opphørId)

        assertThat(tilkjentYtelseOpphør?.andelerTilkjentYtelse).hasSize(8)

        val beregningsresultatOpphør = vedtakRepository.findByIdOrThrow(opphørId)

        val forventetBeregningsperioderEtterOpphør =
            listOf(
                Datoperiode(19 august 2025, 18 september 2025),
                Datoperiode(19 september 2025, 18 oktober 2025),
                Datoperiode(19 oktober 2025, 18 november 2025),
                Datoperiode(19 november 2025, 18 desember 2025),
                Datoperiode(19 desember 2025, 31 desember 2025),
                Datoperiode(1 januar 2026, 31 januar 2026),
                Datoperiode(1 februar 2026, 28 februar 2026),
                Datoperiode(1 mars 2026, 31 mars 2026),
            )

        val beregningsperioderEtterOpphør =
            (beregningsresultatOpphør.data as InnvilgelseEllerOpphørBoutgifter)
                .beregningsresultat
                .perioder
                .map {
                    Datoperiode(
                        it.fom,
                        it.tom,
                    )
                }

        assertThat(beregningsperioderEtterOpphør).isEqualTo(forventetBeregningsperioderEtterOpphør)
    }

    private fun gjennomførInnvilgelseOgSatsjustering(): Pair<BehandlingId, BehandlingId> {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                defaultBoutgifterTestdata(fom = 19 august 2025, 30 juni 2026)
            }
        testoppsettService.settAndelerTilOkForBehandling(testoppsettService.hentBehandling(behandlingContext.behandlingId))

        // Simulerer en satsjustering ved å legge til ny aktivitet fra 1. januar slik at det beregnes fra 1. januar
        val satsjustering =
            opprettRevurderingOgGjennomførBehandlingsløp(behandlingContext.behandlingId) {
                aktivitet {
                    opprett {
                        aktivitetTiltakBoutgifter(1 januar 2026, 2 januar 2026)
                    }
                }
            }
        testoppsettService.settAndelerTilOkForBehandling(testoppsettService.hentBehandling(satsjustering))

        return Pair(behandlingContext.behandlingId, satsjustering)
    }

    private fun injectBuggySatsjusteringData(satsjusteringId: BehandlingId) {
        val vedtak =
            vedtakRepository.findByIdOrThrow(satsjusteringId).withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

        val nyeDatoer =
            listOf(
                Datoperiode(19 august 2025, 18 september 2025),
                Datoperiode(19 september 2025, 18 oktober 2025),
                Datoperiode(19 oktober 2025, 18 november 2025),
                Datoperiode(19 november 2025, 18 desember 2025),
                Datoperiode(19 desember 2025, 31 desember 2025),
                Datoperiode(1 januar 2026, 31 januar 2026),
                Datoperiode(1 februar 2026, 28 februar 2026),
                Datoperiode(1 mars 2026, 31 mars 2026),
                Datoperiode(1 april 2026, 30 april 2026),
                Datoperiode(1 mai 2026, 31 mai 2026),
                Datoperiode(1 juni 2026, 30 juni 2026),
            )

        val buggyPerioder =
            nyeDatoer
                .map {
                    vedtak.data.beregningsresultat.perioder
                        .first()
                        .grunnlag
                        .copy(fom = it.fom, tom = it.tom)
                }.map {
                    BeregningsresultatForLøpendeMåned(
                        grunnlag = it,
                        stønadsbeløp = 4953,
                        delAvTidligereUtbetaling = false,
                    )
                }
        val buggyData =
            when (val d = vedtak.data) {
                is InnvilgelseBoutgifter -> d.copy(beregningsresultat = BeregningsresultatBoutgifter(buggyPerioder))
                is OpphørBoutgifter -> d.copy(beregningsresultat = BeregningsresultatBoutgifter(buggyPerioder))
            }
        vedtakRepository.update(vedtak.copy(data = buggyData))
    }
}
