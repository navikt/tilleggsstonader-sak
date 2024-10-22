package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto

data class BehandlingForOppfølgingDto(
    val behandling: BehandlingDto,
    val stønadsperioderForKontroll: List<PeriodeTilKontroll>,
    val hentingFeiletFor: List<TypeYtelsePeriode>
)

data class PeriodeTilKontroll(
    val stønadsperiodeDto: StønadsperiodeDto,
    val årsak: MutableSet<ÅrsakKontroll>,
)

enum class ÅrsakKontroll {
    YTELSE,
    AKTIVITET
}