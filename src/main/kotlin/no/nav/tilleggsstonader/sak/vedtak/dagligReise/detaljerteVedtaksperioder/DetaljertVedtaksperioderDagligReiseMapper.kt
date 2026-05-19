package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId

object DetaljertVedtaksperioderDagligReiseMapper {
    fun finnDetaljerteVedtaksperioderDagligReise(
        vedtaksdataTso: InnvilgelseEllerOpphørDagligReise?,
        vedtaksdataTsr: InnvilgelseEllerOpphørDagligReise?,
        adresserTso: Map<ReiseId, String>,
        adresserTsr: Map<ReiseId, String>,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val reiserTso = vedtaksdataTso?.hentUtOffentligTransport()
        val reiserTsr = vedtaksdataTsr?.hentUtOffentligTransport()

        return listOf(
            reiserTso.tilDetaljerteVedtaksperioder(Stønadstype.DAGLIG_REISE_TSO, adresserTso),
            reiserTsr.tilDetaljerteVedtaksperioder(Stønadstype.DAGLIG_REISE_TSR, adresserTsr),
        ).flatten()
    }

    private fun InnvilgelseEllerOpphørDagligReise.hentUtOffentligTransport(): List<BeregningsresultatForReise> =
        beregningsresultat.offentligTransport?.reiser.orEmpty()

    private fun List<BeregningsresultatForReise>?.tilDetaljerteVedtaksperioder(
        stønadstype: Stønadstype,
        adresser: Map<ReiseId, String>,
    ): List<DetaljertVedtaksperiodeDagligReise> =
        this.orEmpty().map { reise ->
            reise.tilDetaljertBeregningsperiode(
                stønadstype = stønadstype,
                adresse = adresser[reise.reiseId],
            )
        }

    private fun BeregningsresultatForReise.tilDetaljertBeregningsperiode(
        stønadstype: Stønadstype,
        adresse: String?,
    ): DetaljertVedtaksperiodeDagligReise {
        val detaljertBeregningsperioder =
            perioder
                .sortedByDescending { it.grunnlag.fom }
                .map { it.tilDetaljertBeregningsperiode() }

        return DetaljertVedtaksperiodeDagligReise(
            stønadstype = stønadstype,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            detaljertBeregningsperioder = detaljertBeregningsperioder,
            adresse = adresse,
        )
    }

    private fun BeregningsresultatForPeriode.tilDetaljertBeregningsperiode() =
        DetaljertBeregningsperioder(
            fom = grunnlag.fom,
            tom = grunnlag.tom,
            prisEnkeltbillett = grunnlag.prisEnkeltbillett,
            prisSyvdagersbillett = grunnlag.prisSyvdagersbillett,
            pris30dagersbillett = grunnlag.pris30dagersbillett,
            beløp = beløp,
            billettdetaljer = billettdetaljer,
            antallReisedager = grunnlag.antallReisedager,
            antallReisedagerPerUke = grunnlag.antallReisedagerPerUke,
        )
}
