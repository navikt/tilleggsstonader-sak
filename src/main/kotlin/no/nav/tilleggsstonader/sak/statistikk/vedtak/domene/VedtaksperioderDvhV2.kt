package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeMapper
import java.time.LocalDate

data class VedtaksperioderDvhV2(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeTypeDvh,
    val aktivitet: AktivitetTypeDvh,
    val antallBarn: Int,
//    val barn: BarnDvh.JsonWrapper,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvhV2>,
    )

    // TODO: Mapper for læremidler også

    companion object {
        fun fraDomene(beregningsresultat: List<BeregningsresultatForMåned>) = JsonWrapper(
            vedtaksperioder = VedtaksperiodeMapper.mapTilVedtaksperiode(beregningsresultat).map {
                VedtaksperioderDvhV2(
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                    aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                    antallBarn = it.antallBarn,
                )
            }
        )
    }
}