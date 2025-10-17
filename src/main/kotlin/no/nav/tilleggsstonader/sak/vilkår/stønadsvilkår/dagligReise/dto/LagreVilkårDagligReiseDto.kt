package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate

data class LagreDagligReise(
    val behandlingId: BehandlingId,
    val fom: LocalDate,
    val tom: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelse>,
    val fakta: FaktaDagligReiseDto? = null,
)

data class SvarOgBegrunnelse(
    val svarId: SvarId,
    val begrunnelse: String? = null,
)
