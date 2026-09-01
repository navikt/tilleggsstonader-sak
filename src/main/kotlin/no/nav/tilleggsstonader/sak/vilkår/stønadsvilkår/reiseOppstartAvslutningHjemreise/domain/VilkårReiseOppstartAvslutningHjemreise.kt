package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import java.time.LocalDate

data class VilkårReiseOppstartAvslutningHjemreise(
    val id: VilkårId = VilkårId.random(),
    val behandlingId: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<Delvilkår>,
    val fakta: FaktaReiseOppstartAvslutningHjemreise,
    val slettetKommentar: String? = null,
) : Periode<LocalDate>,
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
