package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class OpphørValideringService(
    private val vilkårsperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
) {
    fun validerVilkårperioder(
        saksbehandling: Saksbehandling,
        opphørsdato: LocalDate,
    ) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling.id)

        validerIngenNyeOppfylteVilkårEllerVilkårperioder(vilkår, vilkårperioder)
        validerIngenEndredePerioderMedTomEtterOpphørsdato(
            vilkårperioder,
            vilkår,
            opphørsdato,
        )
    }

    fun validerIngenUtbetalingEtterOpphørsdato(
        beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn,
        opphørsdato: LocalDate,
    ) {
        beregningsresultatTilsynBarn.perioder.forEach { periode ->
            periode.beløpsperioder
                .filter { it.beløp > 0 }
                .forEach {
                    brukerfeilHvis(
                        it.dato >= opphørsdato,
                    ) { "Opphør er et ugyldig vedtaksresultat fordi det er utbetalinger på eller etter opphørsdato" }
                }
        }
    }

    fun validerVedtaksperioderAvkortetVedOpphør(
        forrigeBehandlingsVedtaksperioder: List<Vedtaksperiode>,
        opphørsdato: LocalDate,
    ) {
        val senesteTomIForrigeVedtaksperioder = forrigeBehandlingsVedtaksperioder.maxOf { it.tom }

        brukerfeilHvis(senesteTomIForrigeVedtaksperioder < opphørsdato) {
            "Opphør er et ugyldig valg fordi ønsket opphørsdato ikke korter ned vedtaket."
        }
    }

    // TODO: Fjern når læremidler er over på felles vedtaksperiode
    fun validerVedtaksperioderAvkortetVedOpphørLæremidler(
        forrigeBehandlingsVedtaksperioder: List<Vedtaksperiode>,
        opphørsdato: LocalDate,
    ) {
        validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder,
            opphørsdato,
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

    private fun validerIngenEndredePerioderMedTomEtterOpphørsdato(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        opphørsdato: LocalDate,
    ) {
        vilkårperioder.målgrupper.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.erOppfyltOgEndret() && vilkårperiode.tom > opphørsdato) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret målgruppe er etter opphørsdato"
            }
        }

        vilkårperioder.aktiviteter.forEach { vilkårperiode ->
            brukerfeilHvis(vilkårperiode.erOppfyltOgEndret() && vilkårperiode.tom > opphørsdato) {
                "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret aktivitet er etter opphørsdato"
            }
        }
        vilkår.forEach { enkeltVilkår ->
            if (enkeltVilkår.erOppfyltOgEndret()) {
                val tom = enkeltVilkår.tom ?: error("Til og med dato er påkrevd for endret vilkår")
                val erUgyldig =
                    if (enkeltVilkår.type == VilkårType.PASS_BARN) {
                        YearMonth.from(tom) > YearMonth.from(opphørsdato)
                    } else {
                        tom > opphørsdato
                    }
                brukerfeilHvis(
                    erUgyldig,
                ) { "Opphør er et ugyldig vedtaksresultat fordi til og med dato for endret vilkår er etter opphørsdato" }
            }
        }
    }

    private fun Vilkårperiode.erOppfyltOgEndret(): Boolean = (status == Vilkårstatus.ENDRET) && (resultat == ResultatVilkårperiode.OPPFYLT)

    private fun Vilkår.erOppfyltOgEndret(): Boolean = (status == VilkårStatus.ENDRET) && (resultat == Vilkårsresultat.OPPFYLT)
}
