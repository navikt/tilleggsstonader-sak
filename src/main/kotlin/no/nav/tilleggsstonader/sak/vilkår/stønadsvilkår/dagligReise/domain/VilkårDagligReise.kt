package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
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
    val fakta: FaktaDagligReise,
    val slettetKommentar: String? = null,
) : Periode<LocalDate>,
    Mergeable<LocalDate, VilkårDagligReise> {
    init {
        validatePeriode()
        validerFaktaErForventetType()
    }

    override fun merge(other: VilkårDagligReise): VilkårDagligReise =
        this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))

    // TODO - Legge til validering for taxi når det implementeres.
    private fun validerFaktaErForventetType() {
        if (this.fakta.type === TypeDagligReise.UBESTEMT) return

        return require(this.fakta.type == TypeDagligReise.OFFENTLIG_TRANSPORT || this.fakta.type == TypeDagligReise.PRIVAT_BIL) {
            "Innsendtfakta har ikke gyldig type: ${this.fakta.type}. Forventet type er ${TypeDagligReise.OFFENTLIG_TRANSPORT} eller ${TypeDagligReise.PRIVAT_BIL}"
        }
    }
}
