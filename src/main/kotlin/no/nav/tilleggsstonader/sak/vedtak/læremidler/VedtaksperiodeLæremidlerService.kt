package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeFraVilkårperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeLæremidlerService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingService: BehandlingService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        brukerfeilHvis(detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling)) {
            "Kan ikke foreslå vedtaksperioder fordi det finnes lagrede vedtaksperioder fra en tidligere behandling"
        }

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        return ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioderFaktiskMålgruppe(vilkårperioder).single().let {
            listOf(
                Vedtaksperiode(
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                ),
            )
        }
    }

    fun detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling: Saksbehandling): Boolean =
        finnVedtaksperioder(saksbehandling.forrigeIverksatteBehandlingId)?.isNotEmpty() == true

    private fun finnVedtaksperioder(behandlingId: BehandlingId?): List<Vedtaksperiode>? {
        if (behandlingId == null) return null
        return vedtakRepository.findByIdOrNull(behandlingId)?.let {
            it.withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>().data.vedtaksperioder
        }
    }
}
