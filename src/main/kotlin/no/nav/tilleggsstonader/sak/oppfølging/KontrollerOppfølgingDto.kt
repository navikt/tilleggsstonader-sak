package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class KontrollerOppfølgingRequest(
    val id: UUID,
    val version: Int,
    val utfall: KontrollertUtfall,
    val kommentar: String?,
)

data class KontrollerOppfølgingResponse(
    val id: UUID,
    val behandlingId: BehandlingId,
    val version: Int,
    val opprettetTidspunkt: LocalDateTime,
    val perioderTilKontroll: List<PeriodeForKontrollDto>,
    val kontrollert: Kontrollert?,
    val behandlingsdetaljer: Behandlingsdetaljer,
)

data class PeriodeForKontrollDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: VilkårperiodeType,
    val endringer: List<Kontroll>,
)

fun List<OppfølgingMedDetaljer>.tilDto() =
    this.map {
        KontrollerOppfølgingResponse(
            id = it.id,
            behandlingId = it.behandlingId,
            version = it.version,
            opprettetTidspunkt = it.opprettetTidspunkt,
            perioderTilKontroll =
                it.data.perioderTilKontroll.flatMap { periode ->
                    val endringAktivitet =
                        periode.endringAktivitet?.takeIf { it.isNotEmpty() }?.let {
                            PeriodeForKontrollDto(
                                fom = periode.fom,
                                tom = periode.tom,
                                type = periode.aktivitet!!,
                                endringer = it,
                            )
                        }
                    val endringMålgruppe =
                        periode.endringMålgruppe?.takeIf { it.isNotEmpty() }?.let {
                            PeriodeForKontrollDto(
                                fom = periode.fom,
                                tom = periode.tom,
                                type = periode.målgruppe!!,
                                endringer = it,
                            )
                        }
                    val endringerEllers =
                        periode.endringer?.takeIf { it.isNotEmpty() }?.let {
                            PeriodeForKontrollDto(
                                fom = periode.fom,
                                tom = periode.tom,
                                type = periode.type!!,
                                endringer = it,
                            )
                        }
                    listOfNotNull(endringAktivitet, endringMålgruppe, endringerEllers)
                },
            kontrollert = it.kontrollert,
            behandlingsdetaljer = it.behandlingsdetaljer,
        )
    }
