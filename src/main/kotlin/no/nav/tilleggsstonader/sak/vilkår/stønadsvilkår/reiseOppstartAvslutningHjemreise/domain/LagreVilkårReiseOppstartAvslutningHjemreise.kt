package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaReiseOppstartAvslutningHjemreise,
) : Periode<LocalDate>
