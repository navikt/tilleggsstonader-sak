package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto

data class BehandlingForOppfølgingDto(val behandlingDto: BehandlingDto, val stønadsperioderForKontroll: List<StønadsperiodeDto>)
