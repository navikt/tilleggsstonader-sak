package no.nav.tilleggsstonader.sak.beregnfra

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.min

@Service
class UtledBeregnFraDatoService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun utledBeregnFraDato(behandlingId: BehandlingId): LocalDate? {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val sisteIverksatteBehandling =
            behandlingService
                .hentBehandlinger(behandling.fagsakId)
                .sortertEtterVedtakstidspunkt()
                .last { it.id != behandling.id }

        val vilkår = vilkårService.hentVilkår(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        val vilkårTidligereBehandling = vilkårService.hentVilkår(sisteIverksatteBehandling.id)
        val vilkårsperioderTidligereBehandling = vilkårperiodeService.hentVilkårperioder(sisteIverksatteBehandling.id)

        return BeregnFraUtleder(
            vilkår = vilkår,
            vilkårTidligereBehandling = vilkårTidligereBehandling,
            vilkårsperioder = vilkårsperioder,
            vilkårsperioderTidligereBehandling = vilkårsperioderTidligereBehandling,
        ).utledTidligsteEndring()
    }
}

data class BeregnFraUtleder(
    val vilkår: List<Vilkår>,
    val vilkårTidligereBehandling: List<Vilkår>,
    val vilkårsperioder: Vilkårperioder,
    val vilkårsperioderTidligereBehandling: Vilkårperioder,
//    val vedtaksperioder: List<Vedtaksperiode>,
//    val vedtaksperioderTidligereBehandling: List<Vedtaksperiode>,
) {
    fun utledTidligsteEndring(): LocalDate? =
        listOfNotNull(utledTidligsteEndringForVilkår())
            .takeIf { it.isNotEmpty() }
            ?.let { minOf(it.first(), *it.subList(1, it.size).toTypedArray()) }

    fun utledTidligsteEndringForVilkår(): LocalDate? {
        val antallVilkår = min(vilkår.size, vilkårTidligereBehandling.size)
        var i = 0
        while (i < antallVilkår) {
            val vilkårNå = vilkår[i]
            val vilkårTidligereBehandling = vilkårTidligereBehandling[i]

            if (vilkårNå.fom != vilkårTidligereBehandling.fom) {
                return no.nav.tilleggsstonader.sak.util
                    .min(vilkårNå.fom, vilkårTidligereBehandling.fom)
            }
            if (vilkårNå.tom != vilkårTidligereBehandling.tom) {
                return no.nav.tilleggsstonader.sak.util
                    .min(vilkårNå.tom, vilkårTidligereBehandling.tom)
            }
            if (vilkårNå.status == VilkårStatus.ENDRET) { // FIXME - bør vi heller sjekke felter om faktisk endret?
                return no.nav.tilleggsstonader.sak.util
                    .min(vilkårNå.fom, vilkårTidligereBehandling.fom)
            }
            i++
        }

        return if (i < vilkår.size) {
            vilkår[i].fom
        } else if (i < vilkårTidligereBehandling.size) {
            vilkårTidligereBehandling[i].fom
        } else {
            null
        }
    }

    fun utledTidligsteEndringForVilkårperiode(): LocalDate {
        // Logikk for å utlede tidligste endring basert på vilkårsperioder
        TODO("Implementer logikk for å utlede tidligste endring for vilkårperiode")
    }
}
