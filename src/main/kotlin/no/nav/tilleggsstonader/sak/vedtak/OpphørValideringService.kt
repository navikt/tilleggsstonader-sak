package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
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
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
) {

    fun validerOpphør(saksbehandling: Saksbehandling) {
        val vilkår = vilkårService.hentVilkår(saksbehandling.id)
        val vilkårperioder = vilkårsperiodeService.hentVilkårperioder(saksbehandling.id)

        validerIngenNyeOppfylteVilkårEllerVilkårperioder(vilkår, vilkårperioder)
        validerIngenUtbetalingEtterOpphør(saksbehandling)
        validerIngenEndredePerioderMedTomEtterOpphørsdato(
            vilkårperioder,
            vilkår,
            saksbehandling.revurderFra ?: error("RevurderFra er påkrevd for opphør"),
        )
    }

    private fun validerIngenNyeOppfylteVilkårEllerVilkårperioder(
        vilkår: List<Vilkår>,
        vilkårperioder: Vilkårperioder,
    ) {
        val finnesOppfyltVilkårEllerVilkårperioderMedStatusNy =
            vilkår.any { it.status == VilkårStatus.NY && it.resultat == Vilkårsresultat.OPPFYLT } ||
                vilkårperioder.målgrupper.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT } ||
                vilkårperioder.aktiviteter.any { it.status == Vilkårstatus.NY && it.resultat == ResultatVilkårperiode.OPPFYLT }

        brukerfeilHvis(finnesOppfyltVilkårEllerVilkårperioderMedStatusNy) { "Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT." }
    }

    private fun validerIngenUtbetalingEtterOpphør(saksbehandling: Saksbehandling) {
        val beregningsresultatTilsynBarn = tilsynBarnBeregningService.beregn(saksbehandling)
        val revurderFraDato = saksbehandling.revurderFra
        beregningsresultatTilsynBarn.perioder.forEach { periode ->
            periode.beløpsperioder.forEach {
                brukerfeilHvis(it.dato > revurderFraDato && it.beløp > 0) { "Det er utbetalinger etter opphørsdato" }
            }
        }
    }

    private fun validerIngenEndredePerioderMedTomEtterOpphørsdato(
        vilkårperioder: Vilkårperioder,
        vilkår: List<Vilkår>,
        opphørsDato: LocalDate,
    ) {
        vilkårperioder.målgrupper.forEach { vilkårperiode ->
            if (vilkårperiode.status == Vilkårstatus.ENDRET) {
                brukerfeilHvis(vilkårperiode.tom > opphørsDato) { "TOM er etter opphørsdato for endret målgruppe" }
            }
        }
        vilkårperioder.aktiviteter.forEach { vilkårperiode ->
            if (vilkårperiode.status == Vilkårstatus.ENDRET) {
                brukerfeilHvis(vilkårperiode.tom > opphørsDato) { "TOM er etter opphørsdato for endret aktivitet" }
            }
        }
        vilkår.forEach { vilkår ->
            if (vilkår.status == VilkårStatus.ENDRET) {
                val tom = vilkår.tom ?: error("TOM er påkrevd for endret vilkår")
                brukerfeilHvis(tom > opphørsDato) { "TOM er etter opphørsdato for endret vilkår" }
            }
        }
    }
}
