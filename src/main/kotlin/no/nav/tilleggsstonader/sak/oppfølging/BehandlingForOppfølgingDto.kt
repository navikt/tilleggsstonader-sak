package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingForOppfølgingDto(
    val behandling: BehandlingInformasjon,
    val stønadsperioderForKontroll: List<PeriodeForKontroll>,
    val registerAktiviteter: List<RegisterAktivitetDto>,
)

data class BehandlingInformasjon(
    val behandlingId: BehandlingId,
    val fagsakId: FagsakId,
    val eksternFagsakId: Long,
    val stønadstype: Stønadstype,
    val vedtakstidspunkt: LocalDateTime,
)

data class RegisterAktivitetDto(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val typeNavn: String,
    val erUtdanning: Boolean?,
)
