package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import java.time.LocalDate
import java.util.UUID

data class VilkårPeriodeDto(
    val id: UUID,
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val vilkår: VilkårDto
) : Periode<LocalDate>

data class OpprettVilkårperiode(
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate
) : Periode<LocalDate>