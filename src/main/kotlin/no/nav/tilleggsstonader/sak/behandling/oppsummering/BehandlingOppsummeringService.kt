package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.oppsummering.BehandlingOppsummeringUtil.filtrerOgDelFraRevurderFra
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårUtil.finnPerioderEtterRevurderFra
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
    private val vedtaksresultatService: VedtaksresultatService,
) {
    fun hentBehandlingOppsummering(behandlingId: BehandlingId): BehandlingOppsummeringDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vedtaksresultat = vedtaksresultatService.hentVedtaksresultatHvisFinnes(behandlingId)

        return BehandlingOppsummeringDto(
            aktiviteter = vilkårperioder.aktiviteter.oppsummer(behandling.revurderFra),
            målgrupper = vilkårperioder.målgrupper.oppsummer(behandling.revurderFra),
            vilkår = oppsummerStønadsvilkår(behandlingId),
            vedtaksresultat = vedtaksresultat,
        )
    }

    /**
     * Slår sammen vilkårperioder med likt resultat og samme type, dersom de overlapper.
     */
    private fun List<Vilkårperiode>.oppsummer(revurderFra: LocalDate?): List<OppsummertVilkårperiode> =
        this
            .map { it.tilOppsummertVilkårperiode() }
            .sortedBy { it.fom }
            .mergeSammenhengende(
                skalMerges = { v1, v2 -> v1.type == v2.type && v1.resultat == v2.resultat && v1.overlapperEllerPåfølgesAv(v2) },
                merge = { v1, v2 -> v1.copy(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            ).filtrerOgDelFraRevurderFra(
                revurderFra = revurderFra,
            )

    private fun oppsummerStønadsvilkår(behandlingId: BehandlingId): List<Stønadsvilkår> {
        val vilkår = vilkårService.hentVilkår(behandlingId)

        // Lager en map per type slik at sammenhendende vilkår kan slås sammen ved like verdier
        // Grupperes også på barnId slik at PASS_BARN vilkår ikke slås sammen på tvers av barn.
        val mapPerTypeOgBarnId =
            vilkår
                .filter { it.fom != null && it.tom != null }
                .groupBy { it.type to it.barnId }

        return mapPerTypeOgBarnId.map {
            Stønadsvilkår(
                type = it.key.first,
                barnId = it.key.second,
                vilkår =
                    it.value
                        .slåSammenSammenhengende()
                        .finnPerioderEtterRevurderFra()
                        .map { it.tilOppsummertVilkår() },
            )
        }
    }
}
