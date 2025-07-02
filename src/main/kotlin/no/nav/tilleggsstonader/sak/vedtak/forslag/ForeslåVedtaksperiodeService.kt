package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode as VedtaksperiodeLæremidler

@Service
class ForeslåVedtaksperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtaksperiodeService: VedtaksperiodeService,
) {
    fun foreslåVedtaksperioderLæremidler(behandlingId: BehandlingId): List<VedtaksperiodeLæremidler> =
        foreslåPerioder(behandlingId).map { it.tilVedtaksperiodeLæremidler() }

    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        validerFinnesIkkeVedtaksperioderPåTidligereBehandling(saksbehandling)

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        return if (saksbehandling.stønadstype.skalHenteStønadsvilkår()) {
            ForeslåVedtaksperiode.finnVedtaksperiode(vilkårperioder, vilkårService.hentVilkår(behandlingId))
        } else {
            ForeslåVedtaksperiodeFraVilkårperioder.foreslåVedtaksperioder(vilkårperioder).map { it.tilVedtaksperiode() }
        }
    }

    private fun Vedtaksperiode.tilVedtaksperiodeLæremidler() =
        VedtaksperiodeLæremidler(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = aktivitet,
        )

    private fun Stønadstype.skalHenteStønadsvilkår(): Boolean =
        when (this) {
            Stønadstype.LÆREMIDLER -> false
            Stønadstype.BARNETILSYN -> true
            Stønadstype.BOUTGIFTER -> true
            Stønadstype.DAGLIG_REISE_TSO -> true
            Stønadstype.DAGLIG_REISE_TSR -> true
        }

    private fun validerFinnesIkkeVedtaksperioderPåTidligereBehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(vedtaksperiodeService.detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling)) {
            "Kan ikke foreslå vedtaksperioder fordi det finnes lagrede vedtaksperioder fra en tidligere behandling"
        }
    }
}
