package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import java.time.LocalDate
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

data class LagreDagligReise(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaDagligReise,
) : Periode<LocalDate>

data class SvarOgBegrunnelse(
    val svar: SvarId,
    val begrunnelse: String? = null,
)
