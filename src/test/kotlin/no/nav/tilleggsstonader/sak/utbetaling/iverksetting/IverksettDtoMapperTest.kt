package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil.totrinnskontroll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class IverksettDtoMapperTest {

    val iverksettingId = UUID.randomUUID()
    val fagsak = fagsak(identer = setOf(PersonIdent("ident1")), eksternId = EksternFagsakId(200, FagsakId.random()))
    val behandling = saksbehandling(fagsak = fagsak, behandling = behandling(vedtakstidspunkt = osloNow()))
    val iverksetting = Iverksetting(iverksettingId, osloNow())
    val andel = andelTilkjentYtelse(kildeBehandlingId = behandling.id, beløp = 100, iverksetting = iverksetting)
    val totrinnskontroll = totrinnskontroll(
        status = TotrinnInternStatus.GODKJENT,
        saksbehandler = "saksbehandler",
        beslutter = "beslutter",
    )

    @Test
    fun `skal mappe felter riktig`() {
        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            andelerTilkjentYtelse = listOf(andel),
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = null,
        )

        assertThat(dto.iverksettingId).isEqualTo(iverksettingId)
        assertThat(dto.sakId).isEqualTo("200")
        assertThat(dto.behandlingId).isEqualTo(behandling.eksternId.toString())
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
                    stønadsdata = StønadsdataDto(
                        stønadstype = StønadstypeIverksetting.TILSYN_BARN_AAP,
                    ),
                ),
            )
        }
    }

    @Test
    fun `skal filtrere vekk andeler med 0-beløp då disse ikke trenger iverksetting`() {
        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            andelerTilkjentYtelse = listOf(andel.copy(beløp = 0)),
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = null,
        )
        assertThat(dto.vedtak.utbetalinger).isEmpty()
    }

    @Test
    fun `skal mappe forrigeIverksetting`() {
        val forrigeIverksetting = ForrigeIverksettingDto("11", UUID.randomUUID())
        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            andelerTilkjentYtelse = listOf(andel.copy(beløp = 0)),
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = forrigeIverksetting,
        )
        assertThat(dto.forrigeIverksetting).isEqualTo(forrigeIverksetting)
    }
}
