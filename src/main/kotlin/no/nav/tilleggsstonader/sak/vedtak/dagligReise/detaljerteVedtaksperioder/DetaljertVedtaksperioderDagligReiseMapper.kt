package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId

object DetaljertVedtaksperioderDagligReiseMapper {
    fun finnDetaljerteVedtaksperioderDagligReise(
        vedtaksdataTso: InnvilgelseEllerOpphørDagligReise?,
        vedtaksdataTsr: InnvilgelseEllerOpphørDagligReise?,
        adresserTso: Map<ReiseId, String>,
        adresserTsr: Map<ReiseId, String>,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val reiserOffentligTransportTso = vedtaksdataTso?.hentUtOffentligTransport()
        val reiserOffentligTransportTsr = vedtaksdataTsr?.hentUtOffentligTransport()
        val reiserPrivatBilTso = vedtaksdataTso?.hentUtRammevedtakPrivatBil()
        val reiserPrivatBilTsr = vedtaksdataTsr?.hentUtRammevedtakPrivatBil()

        return listOf(
            reiserOffentligTransportTso.tilDetaljerteVedtaksperioderOffentligTransport(
                Stønadstype.DAGLIG_REISE_TSO,
                adresserTso,
            ),
            reiserOffentligTransportTsr.tilDetaljerteVedtaksperioderOffentligTransport(
                Stønadstype.DAGLIG_REISE_TSR,
                adresserTsr,
            ),
            reiserPrivatBilTso.tilDetaljerteVedtaksperioderPrivatBil(Stønadstype.DAGLIG_REISE_TSO, adresserTso),
            reiserPrivatBilTsr.tilDetaljerteVedtaksperioderPrivatBil(Stønadstype.DAGLIG_REISE_TSR, adresserTsr),
        ).flatten()
    }

    private fun InnvilgelseEllerOpphørDagligReise.hentUtOffentligTransport(): List<BeregningsresultatForReise> =
        beregningsresultat.offentligTransport?.reiser.orEmpty()

    private fun InnvilgelseEllerOpphørDagligReise.hentUtRammevedtakPrivatBil(): List<RammeForReiseMedPrivatBil> =
        rammevedtakPrivatBil?.reiser.orEmpty()

    private fun List<BeregningsresultatForReise>?.tilDetaljerteVedtaksperioderOffentligTransport(
        stønadstype: Stønadstype,
        adresser: Map<ReiseId, String>,
    ): List<DetaljertVedtaksperiodeDagligReise> =
        this.orEmpty().map { reise ->
            reise.tilDetaljertBeregningsperiode(
                stønadstype = stønadstype,
                adresse = adresser[reise.reiseId],
            )
        }

    private fun List<RammeForReiseMedPrivatBil>?.tilDetaljerteVedtaksperioderPrivatBil(
        stønadstype: Stønadstype,
        adresser: Map<ReiseId, String>,
    ): List<DetaljertVedtaksperiodeDagligReise> =
        this.orEmpty().map { reise ->
            DetaljertVedtaksperiodeDagligReise(
                stønadstype = stønadstype,
                typeDagligReise = TypeDagligReise.PRIVAT_BIL,
                detaljertBeregningsperioder = null,
                adresse = adresser[reise.reiseId] ?: reise.aktivitetsadresse,
                rammevedtakPrivatBil = reise.tilDto(),
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
            rammevedtakPrivatBil = null,
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
