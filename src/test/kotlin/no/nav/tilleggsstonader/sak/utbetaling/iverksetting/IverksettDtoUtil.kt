package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object IverksettDtoUtil {
    fun iverksettDto(
        sakId: String = "fagsakId",
        behandlingId: BehandlingId = BehandlingId.random(),
        eksternBehandlingId: Long = 1,
        iverksettingId: UUID = behandlingId.id,
        personident: String = "123",
        vedtak: VedtaksdetaljerDto = vedtaksdetaljerDto(),
        forrigeIverksetting: ForrigeIverksettingDto? = null,
    ) = IverksettDto(
        sakId = sakId,
        behandlingId = eksternBehandlingId.toString(),
        iverksettingId = iverksettingId,
        personident = personident,
        vedtak = vedtak,
        forrigeIverksetting = forrigeIverksetting,
    )

    fun vedtaksdetaljerDto(
        vedtakstidspunkt: LocalDateTime = osloNow(),
        saksbehandlerId: String = "saksbehandler",
        beslutterId: String = "beslutter",
        utbetalinger: List<UtbetalingDto> = listOf(utbetalingDto()),
    ) = VedtaksdetaljerDto(
        vedtakstidspunkt = vedtakstidspunkt,
        saksbehandlerId = saksbehandlerId,
        beslutterId = beslutterId,
        utbetalinger = utbetalinger,
    )

    fun utbetalingDto(
        beløp: Int = 100,
        satstype: SatstypeIverksetting = SatstypeIverksetting.DAGLIG,
        fraOgMedDato: LocalDate = LocalDate.of(2023, 1, 1),
        tilOgMedDato: LocalDate = LocalDate.of(2023, 1, 31),
        stønadstype: StønadstypeIverksetting = StønadstypeIverksetting.TILSYN_BARN_AAP,
        brukersNavKontor: String? = null,
    ) = UtbetalingDto(
        beløp = beløp,
        satstype = satstype,
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
        stønadsdata =
            StønadsdataDto(
                stønadstype = stønadstype,
                brukersNavKontor = brukersNavKontor,
            ),
    )
}
