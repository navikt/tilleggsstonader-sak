package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode as VedtaksperiodeLæremidler

/**
 * TODO 1 - Burde man forholde seg til arenaTom når man foreslår vedtaksperioder?
 * TODO 2 - Hvordan skal man sette min/maks datoer på forslag av vedtaksperioder? Eks skal man ikke innvilge alt for langt frem i tid?
 */
@Service
class ForeslåVedtaksperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val unleashService: UnleashService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun foreslåVedtaksperioderLæremidler(behandlingId: BehandlingId): List<VedtaksperiodeLæremidler> =
        foreslåPerioder(behandlingId).map { it.tilVedtaksperiodeLæremidler() }

    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        return if (unleashService.isEnabled(Toggle.BRUK_NY_FORESLÅ_VEDTAKSPERIODE)) {
            foreslåV2(saksbehandling)
        } else {
            foreslåV1(saksbehandling)
        }
    }

    private fun foreslåV2(saksbehandling: Saksbehandling): List<Vedtaksperiode> {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)
        val forrigeVedtaksperioder =
            saksbehandling.forrigeIverksatteBehandlingId?.let {
                // skal hente alle vedtaksperioder fra forrige behandling, så setter revurderFra til null
                vedtaksperiodeService.finnVedtaksperioderForBehandling(it, revurdererFra = null)
            } ?: emptyList()
        val tidligstEndring =
            utledTidligsteEndringService.utledTidligsteEndringIgnorerVedtaksperioder(saksbehandling.id)

        return if (saksbehandling.stønadstype.skalHenteStønadsvilkår()) {
            ForeslåVedtaksperiode.finnVedtaksperiodeV2(
                vilkårperioder = vilkårperioder,
                vilkår = vilkårService.hentVilkår(saksbehandling.id),
                tidligereVedtaksperioder = forrigeVedtaksperioder,
                tidligstEndring = tidligstEndring,
            )
        } else {
            ForeslåVedtaksperiode.finnVedtaksperiodeUtenVilkårV2(
                vilkårperioder = vilkårperioder,
                tidligereVedtaksperioder = forrigeVedtaksperioder,
                tidligstEndring = tidligstEndring,
            )
        }
    }

    private fun foreslåV1(saksbehandling: Saksbehandling): List<Vedtaksperiode> {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)
        validerFinnesIkkeVedtaksperioderPåTidligereBehandling(saksbehandling)
        return if (saksbehandling.stønadstype.skalHenteStønadsvilkår()) {
            ForeslåVedtaksperiode.finnVedtaksperiode(vilkårperioder, vilkårService.hentVilkår(saksbehandling.id))
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
