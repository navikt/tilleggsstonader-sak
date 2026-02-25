package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

object DetaljertVedtaksperioderDagligReiseMapper {
    fun finnDetaljerteVedtaksperioderDagligReise(
        vedtaksdataTso: InnvilgelseEllerOpphørDagligReise?,
        vedtaksdataTsr: InnvilgelseEllerOpphørDagligReise?,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val alleReisePerioderTso = vedtaksdataTso?.tilReiseperioder()
        val alleReisePerioderTsr = vedtaksdataTsr?.tilReiseperioder()

        val vedaksperioderFraBeregningsresultatTso =
            alleReisePerioderTso?.tilDetaljertVedtaksperiode(Stønadstype.DAGLIG_REISE_TSO)
        val vedaksperioderFraBeregningsresultatTsr =
            alleReisePerioderTsr?.tilDetaljertVedtaksperiode(Stønadstype.DAGLIG_REISE_TSR)

        val vedaksperioderFraBeregningsresultat =
            listOf(
                vedaksperioderFraBeregningsresultatTso.orEmpty(),
                vedaksperioderFraBeregningsresultatTsr.orEmpty(),
            ).flatten()
        return vedaksperioderFraBeregningsresultat.sorterOgMergeSammenhengendeEllerOverlappende()
    }

    private fun InnvilgelseEllerOpphørDagligReise.tilReiseperioder() =
        beregningsresultat.offentligTransport?.reiser?.flatMap { reise ->
            reise.perioder
        }

    private fun List<BeregningsresultatForPeriode>.tilDetaljertVedtaksperiode(
        stønadstype: Stønadstype,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val perioder = this
        val perioderMedLikAktivitetOgMålgruppe =
            perioder
                .map { it.tilBeregningsresultatForPeriodeMedFomOgTom() }
                .mergeSammenhengendeMedLikAktivitetOgMålgruppe()
        return perioderMedLikAktivitetOgMålgruppe.map { periode ->
            DetaljertVedtaksperiodeDagligReise(
                fom = periode.grunnlag.fom,
                tom = periode.grunnlag.tom,
                aktivitet = periode.grunnlag.vedtaksperiode.aktivitet,
                typeAktivtet = periode.grunnlag.vedtaksperiode.typeAktivitet,
                målgruppe = periode.grunnlag.vedtaksperiode.målgruppe,
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                stønadstype = stønadstype,
                beregningsresultat = mapBeregnDetajlerForPerioder(periode),
            )
        }
    }

    fun BeregningsresultatForPeriode.mergeSammenhengendeVedtaksperioderMedLikAktivitetOgMålgruppe() {
    }

    private fun mapBeregnDetajlerForPerioder(periode: BeregningsresultatForPeriode): List<BeregningsresultatForPeriodeDto> =
        periode.grunnlag.vedtaksperioder.map { vedtaksperiode ->
            BeregningsresultatForPeriodeDto(
                fom = periode.grunnlag.fom,
                tom = periode.grunnlag.tom,
                prisEnkeltbillett = periode.grunnlag.prisEnkeltbillett,
                prisSyvdagersbillett = periode.grunnlag.prisSyvdagersbillett,
                pris30dagersbillett = periode.grunnlag.pris30dagersbillett,
                antallReisedagerPerUke = periode.grunnlag.antallReisedagerPerUke,
                beløp = periode.beløp,
                billettdetaljer = periode.billettdetaljer,
                antallReisedager = periode.grunnlag.antallReisedager,
                fraTidligereVedtak = periode.fraTidligereVedtak,
                brukersNavKontor = null,
            )
        }
}
