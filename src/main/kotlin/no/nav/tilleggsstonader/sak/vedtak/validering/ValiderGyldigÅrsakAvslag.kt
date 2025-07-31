package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.springframework.stereotype.Component

@Component
class ValiderGyldigÅrsakAvslag(
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun validerGyldigAvslagInngangsvilkår(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
    ) {
        validerAvslagÅrsakAktivitet(behandlingId, årsakerAvslag)
        validerAvslagÅrsakMålgruppe(behandlingId, årsakerAvslag)
    }

    private fun validerAvslagÅrsakAktivitet(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
    ) {
        if (!årsakerAvslag.contains(ÅrsakAvslag.INGEN_AKTIVITET)) return

        val aktiviteter = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter

        brukerfeilHvisIkke(aktiviteter.any { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med årsak '${ÅrsakAvslag.INGEN_AKTIVITET.displayName}' uten å legge inn minst én aktivitet som ikke er oppfylt."
        }
    }

    private fun validerAvslagÅrsakMålgruppe(
        behandlingId: BehandlingId,
        årsakerAvslag: List<ÅrsakAvslag>,
    ) {
        if (!årsakerAvslag.contains(ÅrsakAvslag.IKKE_I_MÅLGRUPPE)) return

        val målgrupper = vilkårperiodeService.hentVilkårperioder(behandlingId).målgrupper

        brukerfeilHvisIkke(målgrupper.any { it.resultat == ResultatVilkårperiode.IKKE_OPPFYLT }) {
            "Kan ikke avslå med årsak '${ÅrsakAvslag.IKKE_I_MÅLGRUPPE.displayName}' uten å legge inn minst én målgruppe som ikke er oppfylt."
        }
    }
}
