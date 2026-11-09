package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto

fun VilkårFlyttingDto.tilLagreVilkårFlyttingDto() =
    LagreVilkårFlyttingDto(
        flyttedato = flyttedato,
        svar = delvilkårsett.tilSvarPåVilkår(),
        fakta = fakta,
    )
