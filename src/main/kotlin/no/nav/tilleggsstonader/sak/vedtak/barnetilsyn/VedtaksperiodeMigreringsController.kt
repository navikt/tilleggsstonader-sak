package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/admin/vedtak/migrer")
@Unprotected
class VedtaksperiodeMigreringsController(
    val vedtakservice: VedtakService,
    val vedtakRepository: VedtakRepository,
) {
    @GetMapping()
    fun migrer() {
        val innvilgelseTilsynBarn = hentInnvilgelser()
        val opphørTilsynBarn = hentOpphør()

        innvilgelseTilsynBarn.forEach {
            if (it.data.vedtaksperioder == null) {
                val beregningsresultat = it.data.beregningsresultat
                val vedtaksperioder = mapTilVedtaksperiode(beregningsresultat.perioder)

                val nyttVedtak = it.copy(data = it.data.copy(vedtaksperioder = vedtaksperioder))

                vedtakRepository.update(nyttVedtak)
            }
        }

        opphørTilsynBarn.forEach {
            if (it.data.vedtaksperioder == null) {
                val beregningsresultat = it.data.beregningsresultat
                val vedtaksperioder = mapTilVedtaksperiode(beregningsresultat.perioder)

                val nyttVedtak = it.copy(data = it.data.copy(vedtaksperioder = vedtaksperioder))

                vedtakRepository.update(nyttVedtak)
            }
        }
    }

    private fun hentInnvilgelser(): List<GeneriskVedtak<InnvilgelseTilsynBarn>> {
        val innvilgelseTilsynBarn =
            vedtakRepository
                .findAll()
                .filter { it.data is InnvilgelseTilsynBarn } as List<GeneriskVedtak<InnvilgelseTilsynBarn>>
        return innvilgelseTilsynBarn
    }

    private fun hentOpphør(): List<GeneriskVedtak<OpphørTilsynBarn>> {
        val innvilgelseTilsynBarn =
            vedtakRepository
                .findAll()
                .filter { it.data is OpphørTilsynBarn } as List<GeneriskVedtak<OpphørTilsynBarn>>
        return innvilgelseTilsynBarn
    }
}

fun Vedtaksperiode.erLikOgPåfølgesAv(other: Vedtaksperiode): Boolean {
    val erLik =
        this.aktivitet == other.aktivitet &&
            this.målgruppe == other.målgruppe
    return erLik && this.påfølgesAv(other)
}

fun mapTilVedtaksperiode(beregningsresultat: List<BeregningsresultatForMåned>): List<Vedtaksperiode> =
    beregningsresultat
        .flatMap { tilVedtaksperioder(it) }
        .sorted()
        .mergeSammenhengende(
            skalMerges = { s1, s2 -> s1.erLikOgPåfølgesAv(s2) },
            merge = { s1: Vedtaksperiode, s2: Vedtaksperiode ->
                s1.copy(
                    fom = minOf(s1.fom, s2.fom),
                    tom = maxOf(s1.tom, s2.tom),
                )
            },
        )

private fun tilVedtaksperioder(beregningsresultat: BeregningsresultatForMåned) =
    beregningsresultat.grunnlag.stønadsperioderGrunnlag
        .map { it.stønadsperiode }
        .map { vedtaksperiode ->
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = vedtaksperiode.fom,
                tom = vedtaksperiode.tom,
                målgruppe = vedtaksperiode.målgruppe,
                vedtaksperiode.aktivitet,
            )
        }
