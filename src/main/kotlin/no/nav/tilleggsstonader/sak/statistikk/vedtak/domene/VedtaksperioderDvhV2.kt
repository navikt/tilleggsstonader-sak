package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper
import java.time.LocalDate

data class VedtaksperioderDvhV2(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeTypeDvh,
    val aktivitet: AktivitetTypeDvh,
    val antallBarn: Int? = null,
//    val barn: BarnDvh.JsonWrapper,
//    val studienivå: StudienivåDvh,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvhV2>,
    )

    companion object {
        fun fraDomene(vedtak: Vedtak?): JsonWrapper {
            vedtak?.takeIfType<InnvilgelseTilsynBarn>()?.data?.beregningsresultat?.perioder?.let {
                return JsonWrapper(
                    vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(it).map {
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

            vedtak?.takeIfType<InnvilgelseLæremidler>()?.data?.beregningsresultat?.perioder?.let {
                return JsonWrapper(
                    vedtaksperioder = VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(it).map {
                        VedtaksperioderDvhV2(
                            fom = it.fom,
                            tom = it.tom,
                            målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                        )
                    }
                )
            }

            return JsonWrapper(vedtaksperioder = emptyList())
        }
    }
}