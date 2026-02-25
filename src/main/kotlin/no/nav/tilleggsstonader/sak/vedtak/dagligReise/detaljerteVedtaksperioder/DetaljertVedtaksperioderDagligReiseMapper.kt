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
    ): List<DetaljertVedtaksperiodeDagligReise> =
        map { periode ->
            val målgrupper = periode.grunnlag.vedtaksperioder.map { it.målgruppe }
            val typeAktiviteter = periode.grunnlag.vedtaksperioder.map { it.typeAktivitet }
            val aktiviteter = periode.grunnlag.vedtaksperioder.map { it.aktivitet }

            // Nå klarer vi ikke å opprette andeler hvis det finnes ulike målgrupper eller typeAktiviteter på samme behandling.
            // Vi kan derfor sette dette som krav.
            require(målgrupper.distinct().size == 1) {
                "Klarer foreløbig ikke å vise vedtaksperioder når det er flere i målgrupper i samme beregningsperiode."
            }

            require(typeAktiviteter.distinct().size == 1) {
                "Klarer foreløbig ikke å vise vedtaksperioder når det er flere i tiltaksvarianter i samme beregningsperiode"
            }

            // TODO for tso må vi kunne støtte ulike aktiviteter i samme beregningsperiode
            require(aktiviteter.distinct().size == 1) {
                "Klarer foreløbig ikke å vise vedtaksperioder når det er flere i aktiviteter i samme beregningsperiode"
            }

            DetaljertVedtaksperiodeDagligReise(
                fom = periode.grunnlag.fom,
                tom = periode.grunnlag.tom,
                aktivitet = aktiviteter.first(),
                typeAktivtet = typeAktiviteter.first(),
                målgruppe = målgrupper.first(),
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                stønadstype = stønadstype,
                beregningsresultat = mapBeregnDetajlerForPerioder(periode),
            )
        }

    private fun mapBeregnDetajlerForPerioder(periode: BeregningsresultatForPeriode): List<BeregningsresultatForPeriodeDto> =
        listOf(
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
                brukersNavKontor = periode.grunnlag.brukersNavKontor,
            ),
        )
}
