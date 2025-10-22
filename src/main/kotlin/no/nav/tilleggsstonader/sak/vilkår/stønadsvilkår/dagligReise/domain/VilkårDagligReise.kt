package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import java.time.LocalDate

data class VilkårDagligReise(
    val id: VilkårId = VilkårId.random(),
    val behandlingId: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<Delvilkår>,
    val fakta: FaktaDagligReise?,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        validerFaktaErNullNårResultatIkkeOppfylt()
        validerFaktaErForventetType()
    }

    private fun validerFaktaErNullNårResultatIkkeOppfylt() {
        feilHvis(resultat == Vilkårsresultat.IKKE_OPPFYLT && fakta != null) {
            "Fakta må være null når resultat er ikke oppfylt"
        }
    }

    // Foreløpig støttes kun offentlig transport.
    // Når vi implementerer privat bil og taxi legge til logikk og validering for at innsendt fakta er av forventet type.
    private fun validerFaktaErForventetType() {
        feilHvis(fakta != null && fakta !is FaktaOffentligTransport) {
            "Foreløpig støttes kun fakta av typen offentlig transport"
        }
    }
}
