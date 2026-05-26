package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import java.time.LocalDate

data class VilkårReiseTilSamling(
    val id: VilkårId = VilkårId.random(),
    val behandlingId: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<Delvilkår>,
    val fakta: FaktaReiseTilSamling,
    val slettetKommentar: String? = null,
) : Periode<LocalDate>,
    Mergeable<LocalDate, VilkårReiseTilSamling> {
    init {
        validatePeriode()
        validerFaktaErForventetType()
    }

    override fun merge(other: VilkårReiseTilSamling): VilkårReiseTilSamling =
        this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))

    private fun validerFaktaErForventetType() {
        if (this.fakta.type === TypeReiseTilSamling.UBESTEMT) return

        return require(this.fakta.type === TypeReiseTilSamling.OFFENTLIG_TRANSPORT || this.fakta.type === TypeReiseTilSamling.PRIVAT_BIL) {
            "Innsendtfakta har ikke gyldig type: ${this.fakta.type}. Forventet type er ${TypeReiseTilSamling.OFFENTLIG_TRANSPORT} eller ${TypeReiseTilSamling.PRIVAT_BIL}"
        }
    }
}
