package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class VilkårReiseOppstartAvslutningHjemreise(
    override val id: VilkårId = VilkårId.random(),
    override val globalId: VilkårGlobalId? = VilkårGlobalId.random(),
    override val behandlingId: BehandlingId,
    override val aktivitetId: VilkårperiodeGlobalId,
    val reiseId: ReiseId,
    val typeReiseformål: TypeReiseformål,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val resultat: Vilkårsresultat,
    override val status: VilkårStatus?,
    override val delvilkårsett: List<Delvilkår>,
    val fakta: FaktaReiseOppstartAvslutningHjemreise,
    override val slettetKommentar: String? = null,
    val parentVilkår: VilkårGlobalId? = null,
) : VilkårROAH,
    Mergeable<LocalDate, VilkårReiseOppstartAvslutningHjemreise> {
    init {
        validatePeriode()
        validerFaktaErForventetType()
    }

    override fun merge(other: VilkårReiseOppstartAvslutningHjemreise): VilkårReiseOppstartAvslutningHjemreise =
        this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))

    private fun validerFaktaErForventetType() {
        if (this.fakta.type === TypeReiseOppstartAvslutningHjemreise.UBESTEMT) return

        return require(
            this.fakta.type === TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT ||
                this.fakta.type === TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL,
        ) {
            "Innsendtfakta har ikke gyldig type: ${this.fakta.type}. Forventet type er " +
                "${TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT} eller ${TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL}"
        }
    }
}
