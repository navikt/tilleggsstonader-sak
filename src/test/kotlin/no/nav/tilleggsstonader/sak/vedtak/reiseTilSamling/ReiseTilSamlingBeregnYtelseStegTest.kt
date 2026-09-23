package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.OpprettAndelerReiseTilSamlingService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.OpphørReiseTilSamlingRequest
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReiseTilSamlingBeregnYtelseStegTest {
    private val repository = mockk<VedtakRepository>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val beregningService = mockk<ReiseTilSamlingBeregningService>(relaxed = true)
    private val opprettAndelerReiseTilSamlingService = mockk<OpprettAndelerReiseTilSamlingService>(relaxed = true)
    private val opphørValideringService = mockk<OpphørValideringService>(relaxed = true)
    private val utledTidligsteEndringService = mockk<UtledTidligsteEndringService>(relaxed = true)

    private val beregningsplanUtleder = BeregningsplanUtleder(utledTidligsteEndringService)

    private val steg =
        ReiseTilSamlingBeregnYtelseSteg(
            beregningService = beregningService,
            beregningsplanUtleder = beregningsplanUtleder,
            opprettAndelerReiseTilSamlingService = opprettAndelerReiseTilSamlingService,
            opphørValideringService = opphørValideringService,
            vedtakRepository = repository,
            tilkjentYtelseService = tilkjentYtelseService,
            simuleringService = simuleringService,
        )

    private val saksbehandling =
        saksbehandling(
            fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO),
        )

    private fun opphørRequest(opphørsdato: LocalDate?) =
        OpphørReiseTilSamlingRequest(
            årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
            begrunnelse = "begrunnelse",
            opphørsdato = opphørsdato,
        )

    @Test
    fun `skal feile dersom man velger opphør på en førstegangsbehandling`() {
        val vedtak = opphørRequest(opphørsdato = LocalDate.now())

        assertThatThrownBy {
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)
        }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling")
    }

    @Test
    fun `skal feile dersom man velger opphør og opphørsdato ikke er satt`() {
        val revurdering =
            saksbehandling(
                type = BehandlingType.REVURDERING,
                fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO),
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        val vedtak = opphørRequest(opphørsdato = null)

        assertThatThrownBy {
            steg.utførOgReturnerNesteSteg(revurdering, vedtak)
        }.hasMessage("Opphørsdato er ikke satt")
    }
}
