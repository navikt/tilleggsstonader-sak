package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DagligReiseVedtaksperioderValideringService(
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakRepository: VedtakRepository,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ) {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )
        validerIkkeOverlappendeVedtaksperioderForTsrOgTso(
            behandling = behandling,
            vedtaksperioder = vedtaksperioder,
        )
        validerTypeAktivitetForTsr(
            behandling = behandling,
            vedtaksperioder = vedtaksperioder,
        )
    }

    private fun validerIkkeOverlappendeVedtaksperioderForTsrOgTso(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val fagsakIdAnnenEnhet =
            when (behandling.stønadstype) {
                Stønadstype.DAGLIG_REISE_TSR -> fagsakService.finnFagsakerForFagsakPersonId(behandling.fagsakPersonId).dagligReiseTso?.id
                Stønadstype.DAGLIG_REISE_TSO -> fagsakService.finnFagsakerForFagsakPersonId(behandling.fagsakPersonId).dagligReiseTsr?.id
                else -> error("Kan ikke finne fagsakId for annen enhet for daglige reiser når stønadstype er: ${behandling.stønadstype}")
            }

        val vedtaksperioderAnnenEnhet =
            fagsakIdAnnenEnhet?.let {
                hentVedtaksdataForSisteIverksatteBehandling(it)?.vedtaksperioder
            }
        if (vedtaksperioderAnnenEnhet != null) {
            brukerfeilHvis(
                harOverlappendeVedtaksperioderPåTversAvEnheter(
                    vedtaksperioderDenneEnhenten = vedtaksperioder,
                    vedtaksperioderAnnenEnhet = vedtaksperioderAnnenEnhet,
                ),
            ) {
                "Kan ikke ha overlappende vedtaksperioder for Nay og Tiltaksenheten. Se oversikt øverst på siden for å finne overlappende vedtaksperiode."
            }
        }
    }

    private fun validerTypeAktivitetForTsr(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        if (behandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
            feilHvis(
                finnesVedtaksperiodeUtenTypeAktivitet(
                    vedtaksperioder,
                ),
            ) {
                "Fant ikke Variant/TypeAktivitet for Daglig Reise Tsr. Ta kontakt med utvikler teamet"
            }

            validerFinnesAktivitetMedTypeAktivitetForHeleVedtaksperioden(
                behandlingId = behandling.id,
                vedtaksperioder = vedtaksperioder,
            )
        }
    }

    private fun validerFinnesAktivitetMedTypeAktivitetForHeleVedtaksperioden(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter
        val sammenhengendeAktiviteterMedLikTypeAktivitet = aktiviteter.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

        vedtaksperioder.forEach { vedtaksperiode ->
            val sammenslåtteAktiviteterMedRelevantTypeAktivitet =
                sammenhengendeAktiviteterMedLikTypeAktivitet[vedtaksperiode.typeAktivitet]
                    ?.takeIf { it.isNotEmpty() }
                    ?: brukerfeil("Finner ingen perioder hvor vilkår for ${vedtaksperiode.typeAktivitet} er oppfylt")

            sammenslåtteAktiviteterMedRelevantTypeAktivitet.firstOrNull { it.inneholder(vedtaksperiode) }
                ?: brukerfeil(
                    "Finner ingen oppfylte vilkår med ${vedtaksperiode.typeAktivitet} for hele perioden ${vedtaksperiode.formatertPeriodeNorskFormat()}",
                )
        }
    }

    private fun finnesVedtaksperiodeUtenTypeAktivitet(vedtaksperioder: List<Vedtaksperiode>) =
        vedtaksperioder.any { it.typeAktivitet == null }

    private fun harOverlappendeVedtaksperioderPåTversAvEnheter(
        vedtaksperioderDenneEnhenten: List<Vedtaksperiode>,
        vedtaksperioderAnnenEnhet: List<Vedtaksperiode>,
    ) = vedtaksperioderDenneEnhenten.any { vedaksperiodeDenneEnheten ->
        vedtaksperioderAnnenEnhet.any { vedtaksperiodeAnnenEnhet ->
            vedaksperiodeDenneEnheten.overlapper(
                vedtaksperiodeAnnenEnhet,
            )
        }
    }

    private fun hentVedtaksdataForSisteIverksatteBehandling(fagsakId: FagsakId): InnvilgelseEllerOpphørDagligReise? =
        behandlingService
            .finnSisteIverksatteBehandling(fagsakId)
            ?.let {
                vedtakRepository.findByIdOrNull(it.id)?.withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()
            }?.data
}
