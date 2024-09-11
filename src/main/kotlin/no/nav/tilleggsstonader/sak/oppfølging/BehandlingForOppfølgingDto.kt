package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID

data class BehandlingForOppfølgingDto(val behandlingDto: BehandlingDto, val stønadsperioderForKontroll: List<StønadsperiodeDto>)

