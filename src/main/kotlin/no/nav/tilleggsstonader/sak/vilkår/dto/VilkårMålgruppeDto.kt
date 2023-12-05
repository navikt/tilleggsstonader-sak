package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.MålgruppeType
import java.time.LocalDate
import java.util.UUID

data class VilkårMålgruppeDto(
    val id: UUID,
    val type: MålgruppeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val vilkår: VilkårDto
) : Periode<LocalDate>

data class OpprettMålgruppe(
    val type: MålgruppeType,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Periode<LocalDate>