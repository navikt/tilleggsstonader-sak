package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.RevurderFra
import no.nav.tilleggsstonader.sak.felles.domain.RevurderFra.Companion.compareTo
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service

@Service
class OpphørValideringService(
    private val vilkårsperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {
    fun validerVilkårperioder(saksbehandling: Saksbehandling) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling.id)

        validerIngenNyeOppfylteVilkårEllerVilkårperioder(vilkår, vilkårperioder)
        validerIngenEndredePerioderMedTomEtterRevurderFraDato(
            vilkårperioder,
            vilkår,
            saksbehandling.revurderFra ?: error("Revurder fra er påkrevd for opphør"),
        )
    }

    fun validerIngenUtbetalingEtterRevurderFraDato(
        beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn,
        revurderFra: RevurderFra?,
    ) {
        brukerfeilHvis(revurderFra == null) { "Revurder fra dato er påkrevd for opphør" }

        beregningsresultatTilsynBarn.perioder.forEach { periode ->
            periode.beløpsperioder.forEach {
                brukerfeilHvis(
                    it.dato >= revurderFra,
                ) { "Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger på eller etter revurder fra dato" }
            }
        }
    }

    fun validerVedtaksperioderAvkortetVedOpphør(
        forrigeBehandlingsVedtaksperioder: List<Vedtaksperiode>,
        revurderFra: RevurderFra,
    ) {
        val senesteTomIForrigeVedtaksperioder = forrigeBehandlingsVedtaksperioder.maxOf { it.tom }

        brukerfeilHvis(senesteTomIForrigeVedtaksperioder < revurderFra) {
            "Opphør er et ugyldig valg fordi ønsket opphørsdato ikke korter ned vedtaket."
        }
    }

    // TODO: Fjern når læremidler er over på felles vedtaksperiode
    fun validerVedtaksperioderAvkortetVedOpphørLæremidler(
        forrigeBehandlingsVedtaksperioder: List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode>,
        revurderFra: RevurderFra,
    ) {
        validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder.map { it.tilFellesDomeneVedtaksperiode() },
            revurderFra,
        )
    }

    private fun validerIngenNyeOppfylteVilkårEllerVilkårperioder(
        vilkår: List<Vilkår>,
        vilkårperioder: Vilkårperioder,
    ) {
        brukerfeilHvis(vilkår.any { it.status == VilkårStatus.NY && it.resultat == Vilkårsresultat.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye utgifter med oppfylte vilkår"
        }
        brukerfeilHvis(vilkårperioder.målgrupper.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye målgrupper med oppfylte vilkår"
        }
        brukerfeilHvis(vilkårperioder.aktiviteter.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }) {
            "Opphør er et ugyldig vedtaksresultat fordi det er lagt inn nye aktiviteter med oppfylte vilkår"
        }
    }

    private fun validerIngenEndredePerioderMedTomEtterRevurderFraDato(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        revurderFra: RevurderFra,
    ) {
        vilkårperioder.målgrupper.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.erOppfyltOgEndret() && vilkårperiode.tom > revurderFra) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret målgruppe er etter revurder fra dato"
            }
        }
        vilkårperioder.aktiviteter.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.erOppfyltOgEndret() && vilkårperiode.tom > revurderFra) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret aktivitet er etter revurder fra dato"
            }
        }
        vilkår.forEach { enkeltVilkår ->
            if (enkeltVilkår.erOppfyltOgEndret()) {
                val tom = enkeltVilkår.tom ?: error("Til og med dato er påkrevd for endret vilkår")
                brukerfeilHvis(tom > revurderFra) {
                    "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter revurder fra dato"
                }
            }
        }
    }

    private fun Vilkårperiode.erOppfyltOgEndret(): Boolean = (status == Vilkårstatus.ENDRET) && (resultat == ResultatVilkårperiode.OPPFYLT)

    private fun Vilkår.erOppfyltOgEndret(): Boolean = (status == VilkårStatus.ENDRET) && (resultat == Vilkårsresultat.OPPFYLT)
}
