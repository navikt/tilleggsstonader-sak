package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OpphørValideringService(
    private val vilkårsperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {

    fun validerPerioder(saksbehandling: Saksbehandling) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling.id)

        validerIngenNyeOppfylteVilkårEllerVilkårperioder(vilkår, vilkårperioder)
        validerIngenEndredePerioderMedTomEtterOpphørsdato(
            vilkårperioder,
            vilkår,
            saksbehandling.revurderFra ?: error("Revurder fra er påkrevd for opphør"),
        )
    }

    fun validerIngenUtbetalingEtterOpphør(
        beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn,
        opphørsDato: LocalDate?,
    ) {
        brukerfeilHvis(opphørsDato == null) { "Revurder fra dato er påkrevd for opphør" }
        beregningsresultatTilsynBarn.perioder.forEach { periode ->
            periode.beløpsperioder.forEach {
                brukerfeilHvis(it.dato >= opphørsDato) { "Det er utbetalinger etter opphørsdato" }
            }
        }
    }

    private fun validerIngenNyeOppfylteVilkårEllerVilkårperioder(
        vilkår: List<Vilkår>,
        vilkårperioder: Vilkårperioder,
    ) {
        val finnesOppfyltVilkårEllerVilkårperioderMedStatusNy =
            vilkår.any { it.status == VilkårStatus.NY && it.resultat == Vilkårsresultat.OPPFYLT } ||
                vilkårperioder.målgrupper.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT } ||
                vilkårperioder.aktiviteter.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }

        brukerfeilHvis(finnesOppfyltVilkårEllerVilkårperioderMedStatusNy) { "Det er nye inngangsvilkår eller vilkår som er oppfylt." }
    }

    private fun validerIngenEndredePerioderMedTomEtterOpphørsdato(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        opphørsDato: LocalDate,
    ) {
        vilkårperioder.målgrupper.forEach { vilkårperiode ->
            if (vilkårperiode.status == Vilkårstatus.ENDRET) {
                brukerfeilHvis(vilkårperiode.tom > opphørsDato) { "Til og med dato for endret målgruppe er etter opphørsdato" }
            }
        }
        vilkårperioder.aktiviteter.forEach { vilkårperiode ->
            if (vilkårperiode.status == Vilkårstatus.ENDRET) {
                brukerfeilHvis(vilkårperiode.tom > opphørsDato) { "Til og med dato for endret aktivitet er etter opphørsdato" }
            }
        }
        vilkår.forEach { vilkår ->
            if (vilkår.status == VilkårStatus.ENDRET) {
                val tom = vilkår.tom ?: error("Til og med dato er påkrevd for endret vilkår")
                brukerfeilHvis(tom > opphørsDato) { "Til og med dato for endret vilkår er etter opphørsdato" }
            }
        }
    }
}
