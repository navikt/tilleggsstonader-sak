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
        validerIngenEndredePerioderMedTomEtterRevurderFraDato(
            vilkårperioder,
            vilkår,
            saksbehandling.revurderFra ?: error("Revurder fra er påkrevd for opphør"),
        )
    }

    fun validerIngenUtbetalingEtterOpphør(
        beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn,
        revurderFra: LocalDate?,
    ) {
        brukerfeilHvis(revurderFra == null) { "Revurder fra dato er påkrevd for opphør" }
        beregningsresultatTilsynBarn.perioder.forEach { periode ->
            periode.beløpsperioder.forEach {
                brukerfeilHvis(it.dato >= revurderFra) { "Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger etter revurder fra dato." }
            }
        }
    }

    private fun validerIngenNyeOppfylteVilkårEllerVilkårperioder(
        vilkår: List<Vilkår>,
        vilkårperioder: Vilkårperioder,
    ) {
        brukerfeilHvis(vilkår.any { it.status == VilkårStatus.NY && it.resultat == Vilkårsresultat.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er nye utgifter som er oppfylt."
        }
        brukerfeilHvis(vilkårperioder.målgrupper.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er nye målgrupper som er oppfylt."
        }
        brukerfeilHvis(vilkårperioder.aktiviteter.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er nye aktiviteter som er oppfylt."
        }
    }

    private fun validerIngenEndredePerioderMedTomEtterRevurderFraDato(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        revurderFraDato: LocalDate,
    ) {
        vilkårperioder.målgrupper.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.status == Vilkårstatus.ENDRET && vilkårperiode.tom > revurderFraDato) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret målgruppe er etter revurder fra dato."
            }
        }
        vilkårperioder.aktiviteter.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.status == Vilkårstatus.ENDRET && vilkårperiode.tom > revurderFraDato) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret aktivitet er etter revurder fra dato."
            }
        }
        vilkår.forEach { enkeltVilkår ->
            if (enkeltVilkår.status == VilkårStatus.ENDRET) {
                val tom = enkeltVilkår.tom ?: error("Til og med dato er påkrevd for endret vilkår")
                brukerfeilHvis(tom > revurderFraDato) { "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter revurder fra dato." }
            }
        }
    }
}
