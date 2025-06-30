package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.Innvilgelse
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårUtil.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingOppsummeringService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val vedtakService: VedtakService,
) {
    fun hentBehandlingOppsummering(behandlingId: BehandlingId): BehandlingOppsummeringDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vedtak = oppsummerVedtak(behandlingId, behandling.revurderFra)

        return BehandlingOppsummeringDto(
            aktiviteter = vilkårperioder.aktiviteter.oppsummer(behandling.revurderFra),
            målgrupper = vilkårperioder.målgrupper.oppsummer(behandling.revurderFra),
            vilkår = oppsummerStønadsvilkår(behandlingId, behandling.revurderFra),
            vedtak = vedtak,
        )
    }

    /**
     * Slår sammen vilkårperioder med likt resultat og samme type, dersom de overlapper.
     */
    private fun List<Vilkårperiode>.oppsummer(revurderFra: LocalDate?): List<OppsummertVilkårperiode> =
        this
            .map { it.tilOppsummertVilkårperiode() }
            .sortedBy { it.fom }
            .filter { tomLikEllerEtterRevurderFra(revurderFra = revurderFra, tom = it.tom) }
            .mergeSammenhengende(
                skalMerges = { v1, v2 ->
                    v1.type == v2.type &&
                        v1.resultat == v2.resultat &&
                        v1.overlapperEllerPåfølgesAv(
                            v2,
                        )
                },
                merge = { v1, v2 -> v1.copy(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            )

    private fun oppsummerStønadsvilkår(
        behandlingId: BehandlingId,
        revurderFra: LocalDate?,
    ): List<Stønadsvilkår> {
        val vilkår = vilkårService.hentVilkår(behandlingId)

        // Tar kun med vilkår som overlapper eller er etter revurderFra
        val relevanteVilkårIRevurdering =
            vilkår.filter { tomLikEllerEtterRevurderFra(revurderFra = revurderFra, tom = it.tom) }

        // Lager en map per type slik at sammenhendende vilkår kan slås sammen ved like verdier
        // Grupperes også på barnId slik at PASS_BARN vilkår ikke slås sammen på tvers av barn.
        val mapPerTypeOgBarnId =
            relevanteVilkårIRevurdering
                .filter { it.fom != null && it.tom != null }
                .groupBy { it.type to it.barnId }

        return mapPerTypeOgBarnId.map {
            Stønadsvilkår(
                type = it.key.first,
                barnId = it.key.second,
                vilkår =
                    it.value
                        .slåSammenSammenhengende()
                        .map { it.tilOppsummertVilkår() },
            )
        }
    }

    private fun oppsummerVedtak(
        behandlingId: BehandlingId,
        revurderFra: LocalDate?,
    ): OppsummertVedtak? {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        return vedtak?.data?.let { data ->
            when (data) {
                is Avslag -> OppsummertVedtakAvslag(årsaker = data.årsaker)

                is Innvilgelse -> {
                    val vedtaksperioder =
                        vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId, revurderFra)

                    OppsummertVedtakInnvilgelse(
                        vedtaksperioder = vedtaksperioder.map { it.tilDto() },
                    )
                }

                is Opphør ->
                    OppsummertVedtakOpphør(
                        årsaker = data.årsaker,
                        opphørsdato = vedtak.opphørsdato ?: revurderFra!!,
                    )
            }
        }
    }

    private fun tomLikEllerEtterRevurderFra(
        revurderFra: LocalDate?,
        tom: LocalDate?,
    ): Boolean {
        if (revurderFra == null || tom == null) {
            return true
        }
        return revurderFra <= tom
    }
}
