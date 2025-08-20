package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.Innvilgelse
import no.nav.tilleggsstonader.sak.vedtak.domain.Opphør
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårUtil.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingOppsummeringService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtakService: VedtakService,
) {
    fun hentBehandlingOppsummering(behandlingId: BehandlingId): BehandlingOppsummeringDto {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vedtak = vedtakService.hentVedtak(behandlingId)

        // TODO - ble tidligere kuttet med revurderFra som nå er fjernet. Gjør at man ikke får kuttet før man har lagret vedtak
        return BehandlingOppsummeringDto(
            aktiviteter = vilkårperioder.aktiviteter.oppsummer(vedtak?.tidligsteEndring),
            målgrupper = vilkårperioder.målgrupper.oppsummer(vedtak?.tidligsteEndring),
            vilkår = oppsummerStønadsvilkår(behandlingId, vedtak?.tidligsteEndring),
            vedtak = oppsummerVedtak(vedtak),
        )
    }

    /**
     * Slår sammen vilkårperioder med likt resultat og samme type, dersom de overlapper.
     */
    private fun List<Vilkårperiode>.oppsummer(tidligsteEndring: LocalDate?): List<OppsummertVilkårperiode> =
        this
            .map { it.tilOppsummertVilkårperiode() }
            .sortedBy { it.fom }
            .filter { tomLikEllerEtterDatoForTidligsteEndring(tidligsteEndring = tidligsteEndring, tom = it.tom) }
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
        tidligsteEndring: LocalDate?,
    ): List<Stønadsvilkår> {
        val vilkår = vilkårService.hentVilkår(behandlingId)

        // Tar kun med vilkår som overlapper eller er etter tidligsteEndring
        val relevanteVilkårIRevurdering =
            vilkår.filter { tomLikEllerEtterDatoForTidligsteEndring(tidligsteEndring = tidligsteEndring, tom = it.tom) }

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

    private fun oppsummerVedtak(vedtak: Vedtak?): OppsummertVedtak? =
        vedtak?.data?.let { data ->
            when (data) {
                is Avslag -> OppsummertVedtakAvslag(årsaker = data.årsaker)

                is Innvilgelse -> {
                    OppsummertVedtakInnvilgelse(
                        vedtaksperioder = data.vedtaksperioder.tilDto(),
                    )
                }

                is Opphør ->
                    OppsummertVedtakOpphør(
                        årsaker = data.årsaker,
                        opphørsdato = vedtak.opphørsdato!!,
                    )
            }
        }

    private fun tomLikEllerEtterDatoForTidligsteEndring(
        tidligsteEndring: LocalDate?,
        tom: LocalDate?,
    ): Boolean {
        if (tidligsteEndring == null || tom == null) {
            return true
        }
        return tidligsteEndring <= tom
    }
}
