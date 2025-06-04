package no.nav.tilleggsstonader.sak.beregnfra

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.min
import no.nav.tilleggsstonader.sak.util.min as nullableMin

@Service
class UtledBeregnFraDatoService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vedtaksperiodeService: VedtaksperiodeService,
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

        val vedtaksperioder = vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId, null)
        val vedtaksperioderTidligereBehandling = vedtaksperiodeService.finnVedtaksperioderForBehandling(sisteIverksatteBehandling.id, null)

        return BeregnFraUtleder(
            vilkår = vilkår,
            vilkårTidligereBehandling = vilkårTidligereBehandling,
            vilkårsperioder = vilkårsperioder,
            vilkårsperioderTidligereBehandling = vilkårsperioderTidligereBehandling,
            vedtaksperioder = vedtaksperioder,
            vedtaksperioderTidligereBehandling = vedtaksperioderTidligereBehandling,
        ).utledTidligsteEndring()
    }
}

data class BeregnFraUtleder(
    val vilkår: List<Vilkår>,
    val vilkårTidligereBehandling: List<Vilkår>,
    val vilkårsperioder: Vilkårperioder,
    val vilkårsperioderTidligereBehandling: Vilkårperioder,
    val vedtaksperioder: List<Vedtaksperiode>,
    val vedtaksperioderTidligereBehandling: List<Vedtaksperiode>,
) {
    fun utledTidligsteEndring(): LocalDate? =
        listOfNotNull(
            utledTidligsteEndringForVilkår(),
            utledTidligsteEndringForAktiviteter(),
            utledTidligsteEndringForMålgrupper(),
            utledTidligsteEndringForVedtaksperioder(),
        ).minOrNull()

    private fun utledTidligsteEndringForVilkår(): LocalDate? =
        utledEndringIPeriode(
            perioderNå =
                vilkår.map { PeriodeWrapper(periodeType = it, fom = it.fom!!, tom = it.tom!!) }.sorted(),
            perioderTidligere =
                vilkårTidligereBehandling.map { PeriodeWrapper(periodeType = it, fom = it.fom!!, tom = it.tom!!) }.sorted(),
        ) { vilkårNå, vilkårTidligereBehandling ->
            erEndret(vilkårNå.periodeType, vilkårTidligereBehandling.periodeType)
        }

    private fun utledTidligsteEndringForAktiviteter(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vilkårsperioder.aktiviteter.sorted(),
            perioderTidligere = vilkårsperioderTidligereBehandling.aktiviteter.sorted(),
            erEndretFunksjon = ::erEndret,
        )

    private fun utledTidligsteEndringForMålgrupper(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vilkårsperioder.målgrupper.sorted(),
            perioderTidligere = vilkårsperioderTidligereBehandling.målgrupper.sorted(),
            erEndretFunksjon = ::erEndret,
        )

    private fun utledTidligsteEndringForVedtaksperioder(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vedtaksperioder.sorted(),
            perioderTidligere = vedtaksperioderTidligereBehandling.sorted(),
            erEndretFunksjon = ::erEndret,
        )

    private fun erEndret(
        vilkårNå: Vilkår,
        vilkårTidligereBehandling: Vilkår,
    ): Boolean =
        vilkårNå.utgift != vilkårTidligereBehandling.utgift ||
            vilkårNå.barnId != vilkårTidligereBehandling.barnId ||
            delvilkårErEndret(vilkårNå, vilkårTidligereBehandling)

    private fun delvilkårErEndret(
        vilkårNå: Vilkår,
        vilkårTidligereBehandling: Vilkår,
    ): Boolean = vilkårNå.delvilkårsett.utenVurderinger() != vilkårTidligereBehandling.delvilkårsett.utenVurderinger()

    private fun erEndret(
        vilkårperiode: GeneriskVilkårperiode<*>,
        tidligereVilkårperiode: GeneriskVilkårperiode<*>,
    ): Boolean = vilkårperiode.resultat != tidligereVilkårperiode.resultat

    private fun erEndret(
        vedtaksperiode: Vedtaksperiode,
        tidligereVedtaksperiode: Vedtaksperiode,
    ): Boolean =
        vedtaksperiode.målgruppe != tidligereVedtaksperiode.målgruppe ||
            vedtaksperiode.aktivitet != tidligereVedtaksperiode.aktivitet

    private fun <T : Periode<LocalDate>> utledEndringIPeriode(
        perioderNå: List<T>,
        perioderTidligere: List<T>,
        erEndretFunksjon: (T, T) -> Boolean,
    ): LocalDate? {
        val antallPerioder = min(perioderNå.size, perioderTidligere.size)
        var i = 0
        while (i < antallPerioder) {
            val periodeNå = perioderNå[i]
            val periodeTidligere = perioderTidligere[i]

            if (periodeNå.fom != periodeTidligere.fom || erEndretFunksjon(periodeNå, periodeTidligere)) {
                return nullableMin(periodeNå.fom, periodeTidligere.fom)
            }
            if (periodeNå.tom != periodeTidligere.tom) {
                // Legger på en dag, da det først er fra dagen etter tom-datoen at det er en endring
                return nullableMin(periodeNå.tom, periodeTidligere.tom)?.plusDays(1)
            }

            i++
        }

        return if (i < perioderNå.size) {
            // Ny periode i ny behandling
            perioderNå[i].fom
        } else if (i < perioderTidligere.size) {
            // Periode har blitt slettet i ny behandling
            perioderTidligere[i].fom
        } else {
            // Ingen endringer i perioder
            null
        }
    }
}

private fun List<Delvilkår>.utenVurderinger() = this.map { it.copy(vurderinger = emptyList()) }

data class PeriodeWrapper<T>(
    val periodeType: T,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>
