package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class IverksettDtoMapperTest {

    val iverksettingId = UUID.randomUUID()
    val fagsak = fagsak(identer = setOf(PersonIdent("ident1")), eksternId = EksternFagsakId(200, UUID.randomUUID()))
    val behandling = saksbehandling(fagsak = fagsak, behandling = behandling(vedtakstidspunkt = LocalDateTime.now()))
    val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
    val andel = andelTilkjentYtelse(kildeBehandlingId = behandling.id, beløp = 100, iverksetting = iverksetting)
    val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, startdato = null, andel)
    val totrinnskontroll = totrinnskontroll(
        status = TotrinnInternStatus.GODKJENT,
        saksbehandler = "saksbehandler",
        beslutter = "beslutter",
    )

    @Test
    fun `skal mappe felter riktig`() {
        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            tilkjentYtelse = tilkjentYtelse,
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = null,
        )

        assertThat(dto.iverksettingId).isEqualTo(iverksettingId)
        assertThat(dto.sakId).isEqualTo("200")
        assertThat(dto.behandlingId).isEqualTo(behandling.id)
        assertThat(dto.iverksettingId).isEqualTo(iverksettingId)
        assertThat(dto.personident).isEqualTo("ident1")
        assertThat(dto.forrigeIverksetting).isNull()
        with(dto.vedtak) {
            assertThat(vedtakstidspunkt).isEqualTo(behandling.vedtakstidspunkt!!)
            assertThat(saksbehandlerId).isEqualTo("saksbehandler")
            assertThat(beslutterId).isEqualTo("beslutter")
            assertThat(utbetalinger).containsExactly(
                UtbetalingDto(
                    beløp = 100,
                    fraOgMedDato = andel.fom,
                    tilOgMedDato = andel.tom,
                    satstype = SatstypeIverksetting.DAGLIG,
                    stønadstype = StønadstypeIverksetting.TILSYN_BARN_AAP,
                ),
            )
        }
    }
}
