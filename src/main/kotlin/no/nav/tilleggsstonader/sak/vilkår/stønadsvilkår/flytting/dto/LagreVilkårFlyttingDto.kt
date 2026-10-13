package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDomain
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import java.time.LocalDate

data class LagreVilkårFlyttingDto(
    val flytteDato: LocalDate,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaFlyttingDto,
) : LagreVilkår {
    fun tilDomain() =
        LagreVilkårFlytting(
            flytteDato = flytteDato,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta.tilDomain(),
        )
}
