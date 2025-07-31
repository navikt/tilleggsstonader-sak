package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.springframework.stereotype.Component

@Component
class TilsynBarnValiderGyldigÅrsakAvslag(
    private val vilkårService: VilkårService,
    private val validerGyldigÅrsakAvslag: ValiderGyldigÅrsakAvslag,
) {
    fun validerGyldigAvslag(
        behandlingId: BehandlingId,
        vedtak: AvslagTilsynBarnDto,
    ) {
        validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(behandlingId, vedtak.årsakerAvslag)
        validerAvslagPåStønadsvilkår(behandlingId, vedtak.årsakerAvslag)
    }

    private fun validerAvslagPåStønadsvilkår(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
    ) {
        val årsakerBasertPåStønadsvilkår = listOf(ÅrsakAvslag.HAR_IKKE_UTGIFTER, ÅrsakAvslag.MANGELFULL_DOKUMENTASJON)

        val aktuelleÅrsaker = årsakerAvslag.intersect(årsakerBasertPåStønadsvilkår)

        if (aktuelleÅrsaker.isEmpty()) return

        val stønadsvilkår = vilkårService.hentVilkår(behandlingId)

        brukerfeilHvisIkke(stønadsvilkår.any { it.resultat == Vilkårsresultat.IKKE_OPPFYLT }) {
            "Kan ikke avslå med årsak '${
                aktuelleÅrsaker.joinToString(
                    "' og '",
                ) { it.displayName }
            }' uten å legge inn minst ett vilkår for pass barn som ikke er oppfylt."
        }
    }
}
