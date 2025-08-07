package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.PeriodeMedId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode as FellesDomeneVedtaksperiode

enum class VedtaksperiodeStatus {
    NY,
    ENDRET,
    UENDRET,
}

fun avkortVedtaksperiodeVedOpphør(
    forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørLæremidler>,
    revurderFra: LocalDate,
): List<FellesDomeneVedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(revurderFra.minusDays(1))
