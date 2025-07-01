package no.nav.tilleggsstonader.sak.tidligsteendring

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.min

@Service
class UtledTidligsteEndringService(
    private val behandlingService: BehandlingService,
    private val vilkårService: VilkårService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val barnService: BarnService,
    private val unleashService: UnleashService,
) {
    /**
     * Vil plukke opp alle endringer i vilkår, aktiviteter, målgrupper og vedtaksperioder og betrakte det som en endring i revurderingen.
     * Noe som kan vurderes om vi bør ta hensyn til:
     * Vil også plukke oppe endringer som ikke har noe å si for beregning, eksempelvis om man legger til en ny målgruppe.
     */
    fun utledTidligsteEndring(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ): LocalDate? =
        utledTidligsteEndring(
            behandlingId = behandlingId,
            vedtaksperioder = vedtaksperioder,
            hentVedtaksperioderTidligereBehandlingFunction = { behandlingId ->
                vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId, null)
            },
        )

    fun utledTidligsteEndringIgnorerVedtaksperioder(behandlingId: BehandlingId): LocalDate? =
        utledTidligsteEndring(
            behandlingId = behandlingId,
            vedtaksperioder = emptyList(),
            hentVedtaksperioderTidligereBehandlingFunction = { _ -> emptyList() },
        )

    private fun utledTidligsteEndring(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
        hentVedtaksperioderTidligereBehandlingFunction: (BehandlingId) -> List<Vedtaksperiode>,
    ): LocalDate? {
        val behandling = behandlingService.hentBehandling(behandlingId)

        if (!unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) {
            return behandling.revurderFra
        }

        val sisteIverksatteBehandling = behandling.forrigeIverksatteBehandlingId?.let { behandlingService.hentBehandling(it) }

        if (sisteIverksatteBehandling == null) {
            return null
        }

        val vilkår = vilkårService.hentVilkår(behandlingId)
        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        val vilkårTidligereBehandling = vilkårService.hentVilkår(sisteIverksatteBehandling.id)
        val vilkårsperioderTidligereBehandling = vilkårperiodeService.hentVilkårperioder(sisteIverksatteBehandling.id)

        val vedtaksperioderTidligereBehandling = hentVedtaksperioderTidligereBehandlingFunction(sisteIverksatteBehandling.id)

        val barnIder = barnService.finnBarnPåBehandling(behandlingId)
        val barnIderTidligereBehandling = barnService.finnBarnPåBehandling(sisteIverksatteBehandling.id)

        return TidligsteEndringIBehandlingUtleder(
            vilkår = vilkår,
            vilkårTidligereBehandling = vilkårTidligereBehandling,
            vilkårsperioder = vilkårsperioder,
            vilkårsperioderTidligereBehandling = vilkårsperioderTidligereBehandling,
            vedtaksperioder = vedtaksperioder,
            vedtaksperioderTidligereBehandling = vedtaksperioderTidligereBehandling,
            barnIdTilIdentMap = (barnIder + barnIderTidligereBehandling).associate { it.id to it.ident },
        ).utledTidligsteEndring()
    }
}

data class TidligsteEndringIBehandlingUtleder(
    val vilkår: List<Vilkår>,
    val vilkårTidligereBehandling: List<Vilkår>,
    val vilkårsperioder: Vilkårperioder,
    val vilkårsperioderTidligereBehandling: Vilkårperioder,
    val vedtaksperioder: List<Vedtaksperiode>,
    val vedtaksperioderTidligereBehandling: List<Vedtaksperiode>,
    val barnIdTilIdentMap: Map<BarnId, String>,
) {
    private val vilkårComparator =
        Comparator
            .comparing<PeriodeWrapper<Vilkår>, LocalDate> { it.fom }
            .thenComparing { o1, o2 ->
                (barnIdTilIdentMap[o1.periodeType.barnId] ?: "").compareTo(barnIdTilIdentMap[o2.periodeType.barnId] ?: "")
            }.thenComparing { o1, o2 -> o1.tom.compareTo(o2.tom) }

    private val vilkårsperioderComparator =
        Comparator
            .comparing<GeneriskVilkårperiode<*>, LocalDate> { it.fom }
            .thenComparing { o1, o2 -> o1.tom.compareTo(o2.tom) }
            .thenComparing { o1, o2 ->
                o1.faktaOgVurdering.type.vilkårperiodeType.toString().compareTo(
                    o2.faktaOgVurdering.type.vilkårperiodeType
                        .toString(),
                )
            }

    /**
     * Utleder tidligste endring i vilkår, aktiviteter, målgrupper og vedtaksperioder.
     * Gjør dette ved å sortere listene med perioder og sammenligne med periode i tidligere behandling
     * Returnerer null hvis det ikke er noen endringer.
     */
    fun utledTidligsteEndring(): LocalDate? =
        listOfNotNull(
            utledTidligsteEndringForVilkår(),
            utledTidligsteEndringForAktiviteter(),
            utledTidligsteEndringForMålgrupper(),
            utledTidligsteEndringForVedtaksperioder(),
        ).minOrNull()

    private fun utledTidligsteEndringForVilkår(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vilkår.fjernSlettede().mapNotNull { it.wrapSomPeriode() }.sortedWith(vilkårComparator),
            perioderTidligere = vilkårTidligereBehandling.fjernSlettede().mapNotNull { it.wrapSomPeriode() }.sortedWith(vilkårComparator),
        ) { vilkårNå, vilkårTidligereBehandling ->
            erVilkårEndret(vilkårNå.periodeType, vilkårTidligereBehandling.periodeType)
        }

    private fun Iterable<Vilkår>.fjernSlettede() = this.filterNot { it.status == VilkårStatus.SLETTET }

    private fun utledTidligsteEndringForAktiviteter(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vilkårsperioder.aktiviteter.fjernSlettede().sortedWith(vilkårsperioderComparator),
            perioderTidligere = vilkårsperioderTidligereBehandling.aktiviteter.fjernSlettede().sortedWith(vilkårsperioderComparator),
            erEndret = ::erMålgruppeEllerAktivitetEndret,
        )

    private fun utledTidligsteEndringForMålgrupper(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vilkårsperioder.målgrupper.fjernSlettede().sortedWith(vilkårsperioderComparator),
            perioderTidligere = vilkårsperioderTidligereBehandling.målgrupper.fjernSlettede().sortedWith(vilkårsperioderComparator),
            erEndret = ::erMålgruppeEllerAktivitetEndret,
        )

    private fun List<GeneriskVilkårperiode<*>>.fjernSlettede() = this.filterNot { it.status == Vilkårstatus.SLETTET }

    private fun utledTidligsteEndringForVedtaksperioder(): LocalDate? =
        utledEndringIPeriode(
            perioderNå = vedtaksperioder.sorted(),
            perioderTidligere = vedtaksperioderTidligereBehandling.sorted(),
            erEndret = ::erVedtaksperiodeEndret,
        )

    private fun erVilkårEndret(
        vilkårNå: Vilkår,
        vilkårTidligereBehandling: Vilkår,
    ): Boolean =
        vilkårNå.utgift != vilkårTidligereBehandling.utgift ||
            // barnId er forskjellig for hver behandling, sjekker på ident
            barnIdTilIdentMap[vilkårNå.barnId] != barnIdTilIdentMap[vilkårTidligereBehandling.barnId] ||
            vilkårNå.erFremtidigUtgift != vilkårTidligereBehandling.erFremtidigUtgift ||
            vilkårNå.type != vilkårTidligereBehandling.type ||
            vilkårNå.resultat != vilkårTidligereBehandling.resultat

    private fun erMålgruppeEllerAktivitetEndret(
        vilkårperiode: GeneriskVilkårperiode<*>,
        tidligereVilkårperiode: GeneriskVilkårperiode<*>,
    ): Boolean =
        vilkårperiode.resultat != tidligereVilkårperiode.resultat ||
            vilkårperiode.type != tidligereVilkårperiode.type ||
            vilkårperiode.faktaOgVurdering.fakta != tidligereVilkårperiode.faktaOgVurdering.fakta ||
            vilkårperiode.kildeId != tidligereVilkårperiode.kildeId

    private fun erVedtaksperiodeEndret(
        vedtaksperiode: Vedtaksperiode,
        tidligereVedtaksperiode: Vedtaksperiode,
    ): Boolean =
        vedtaksperiode.målgruppe != tidligereVedtaksperiode.målgruppe ||
            vedtaksperiode.aktivitet != tidligereVedtaksperiode.aktivitet

    private fun <T : Periode<LocalDate>> utledEndringIPeriode(
        perioderNå: List<T>,
        perioderTidligere: List<T>,
        erEndret: (T, T) -> Boolean,
    ): LocalDate? {
        val antallPerioder = min(perioderNå.size, perioderTidligere.size)

        val minsteEndringVedSammenligningAvPerioder =
            perioderNå
                .take(antallPerioder)
                .mapIndexedNotNull { index, periodeNå ->
                    val periodeTidligere = perioderTidligere[index]

                    if (periodeNå.fom != periodeTidligere.fom || erEndret(periodeNå, periodeTidligere)) {
                        minOf(periodeNå.fom, periodeTidligere.fom)
                    } else if (periodeNå.tom != periodeTidligere.tom) {
                        // Legger på en dag, da det først er fra dagen etter tom-datoen at det er en endring
                        minOf(periodeNå.tom, periodeTidligere.tom).plusDays(1)
                    } else {
                        null
                    }
                }.minOrNull()

        val minsteEndringIResterendePerioder =
            when {
                // Ny periode i ny behandling
                perioderNå.size > antallPerioder -> perioderNå[antallPerioder].fom
                // Periode har blitt slettet i ny behandling
                perioderTidligere.size > antallPerioder -> perioderTidligere[antallPerioder].fom
                // Ingen endringer i perioder
                else -> null
            }

        return listOfNotNull(minsteEndringVedSammenligningAvPerioder, minsteEndringIResterendePerioder).minOrNull()
    }
}

// Gamle vilkår har ikke fom/tom, så de må pakkes inn i en PeriodeWrapper for å kunne brukes som Periode<LocalDate>
private fun Vilkår.wrapSomPeriode() =
    if (fom != null && tom != null) {
        PeriodeWrapper(periodeType = this, fom = fom, tom = tom)
    } else {
        null
    }

data class PeriodeWrapper<T>(
    val periodeType: T,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>
