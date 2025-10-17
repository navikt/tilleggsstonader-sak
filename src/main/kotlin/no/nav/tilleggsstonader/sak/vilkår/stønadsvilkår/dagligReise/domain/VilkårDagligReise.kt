package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import java.time.LocalDate

data class VilkårDagligReise(
    val id: VilkårId = VilkårId.random(),
    val behandlingId: BehandlingId,
    override val fom: LocalDate, // TODO: Vurder om dato skal kunne være nullable sånn som på vilkår.
    override val tom: LocalDate,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<Delvilkår>,
    val fakta: FaktaDagligReise?,
) : Periode<LocalDate> {
    init {
        validatePeriode()

        // TODO: Validering av input
    }

    fun tilDto() =
        VilkårDagligReiseDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta?.tilDto(),
        )
}
