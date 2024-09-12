package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import java.util.UUID

data class VilkårsoppsummeringDto(
    val stønadsperioder: List<StønadsperiodeDto>,
    val visVarselKontantstøtte: Boolean,
    @Deprecated("Denne brukes ikke lengre?")
    val aktivitet: Boolean,
    @Deprecated("Denne brukes ikke lengre?")
    val målgruppe: Boolean,
    @Deprecated("Denne brukes ikke lengre?")
    val stønadsperiode: Boolean,
    @Deprecated("Denne brukes ikke lengre?")
    val passBarn: List<BarnOppsummering>,
)

data class BarnOppsummering(
    val barnId: UUID,
    val ident: String,
    val navn: String,
    val alder: Int?,
    val alderNårStønadsperiodeBegynner: Int?,
    val oppfyllerAlleVilkår: Boolean,
)
