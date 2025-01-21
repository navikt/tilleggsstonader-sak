package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetTypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeTypeDvh
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeTilsynBarnMapper
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import java.time.LocalDate

data class VedtaksperioderDvhV2(
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppe: MålgruppeTypeDvh,
    val lovverketsMålgruppe: LovverketsMålgruppeDvh,
    val aktivitet: AktivitetTypeDvh? = null,
    val antallBarn: Int? = null,
    val barn: BarnDvh.JsonWrapper? = null,
    val studienivå: StudienivåDvh? = null,
) {
    data class JsonWrapper(
        val vedtaksperioder: List<VedtaksperioderDvhV2>,
    )

    companion object {
        fun fraDomene(vedtak: Vedtak?, vilkår: List<Vilkår>, barnFakta: List<BehandlingBarn>): JsonWrapper {
            vedtak?.takeIfType<InnvilgelseTilsynBarn>()?.data?.beregningsresultat?.perioder?.let {
                return JsonWrapper(
                    vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(it).map {
                        VedtaksperioderDvhV2(
                            fom = it.fom,
                            tom = it.tom,
                            målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            aktivitet = AktivitetTypeDvh.fraDomene(it.aktivitet),
                            antallBarn = it.antallBarn,
                            barn = BarnDvh.fraDomene(
                                it.finnOverlappendeVilkårperioder(vilkår)
                                    .finnBarnasFødselsnumre(barnFakta)
                            )
                        )
                    },
                )
            }

            vedtak?.takeIfType<InnvilgelseLæremidler>()?.data?.beregningsresultat?.perioder?.let {
                return JsonWrapper(
                    vedtaksperioder = VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(it).map {
                        VedtaksperioderDvhV2(
                            fom = it.fom,
                            tom = it.tom,
                            målgruppe = MålgruppeTypeDvh.fraDomene(it.målgruppe),
                            lovverketsMålgruppe = LovverketsMålgruppeDvh.fraDomene(it.målgruppe),
                            studienivå = StudienivåDvh.fraDomene(it.studienivå),
                        )
                    },
                )
            }

            return JsonWrapper(vedtaksperioder = emptyList())
        }

        fun VedtaksperiodeTilsynBarn.finnOverlappendeVilkårperioder(vilkårperioder: List<Vilkår>) =
            vilkårperioder.filter { it.overlapper(this) }

        fun List<Vilkår>.finnBarnasFødselsnumre(barn: List<BehandlingBarn>) = this.mapNotNull { vilkår ->
            barn.find { vilkår.barnId == it.id }?.ident
        }
    }
}