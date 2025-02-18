package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.ForeslåVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtakService: VedtakService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        brukerfeilHvis(detFinnesVedtaksperioder(behandlingId)) {
            "Det finnes allerede lagrede vedtaksperioder for denne behandlingen"
        }

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkår = vilkårService.hentVilkår(behandlingId)

        return ForeslåVedtaksperiode.finnVedtaksperiode(
            vilkårperioder = vilkårperioder,
            vilkår = vilkår,
        )
    }

    private fun detFinnesVedtaksperioder(behandlingId: BehandlingId) =
        vedtakService
            .hentVedtak(behandlingId)
            ?.takeIfType<InnvilgelseTilsynBarn>()
            ?.data
            ?.beregningsresultat
            ?.tilDto(null)
            ?.vedtaksperioder
            .isNullOrEmpty()
            .not()
}
