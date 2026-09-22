package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.lagBeregningsresultatForOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TiltakReiseTilSamlingTsr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpprettAndelerReiseTilSamlingServiceTest {
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val vedtakService = mockk<VedtakService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()

    private val service =
        OpprettAndelerReiseTilSamlingService(
            tilkjentYtelseService,
            vedtakService,
            vilkårperiodeService,
        )

    private val mandag = 1 september 2025

    @Test
    fun `for TSR hentes tiltaksvariant fra aktiviteten via aktivitetId og lagres på andelen`() {
        val saksbehandlingTsr = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        val aktivitetId = VilkårperiodeGlobalId.random()
        mockHentVedtak(
            saksbehandlingTsr,
            listOf(
                lagBeregningsresultatForOffentligTransport(
                    fom = mandag,
                    aktivitetId = aktivitetId,
                    brukersNavKontor = "1234",
                ),
            ),
        )
        every {
            vilkårperiodeService.hentAktivitet(aktivitetId, saksbehandlingTsr.id)
        } returns
            VilkårperiodeTestUtil.aktivitet(
                behandlingId = saksbehandlingTsr.id,
                faktaOgVurdering = TiltakReiseTilSamlingTsr,
                tiltaksvariant = TypeAktivitet.ARBFORB,
            )
        val andelerSlot = slot<List<AndelTilkjentYtelse>>()
        every {
            tilkjentYtelseService.lagreTilkjentYtelse(behandlingId = saksbehandlingTsr.id, andeler = capture(andelerSlot))
        } returns mockk()

        service.lagreAndelerForBehandling(saksbehandlingTsr)

        assertThat(andelerSlot.captured.single().type).isEqualTo(TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE)
    }

    @Test
    fun `for TSR kastes det feil dersom aktivitetId mangler`() {
        val saksbehandlingTsr = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        mockHentVedtak(
            saksbehandlingTsr,
            listOf(
                lagBeregningsresultatForOffentligTransport(
                    fom = mandag,
                    aktivitetId = null,
                    brukersNavKontor = "1234",
                ),
            ),
        )

        assertThrows<Feil> {
            service.lagreAndelerForBehandling(saksbehandlingTsr)
        }

        verify(exactly = 0) { tilkjentYtelseService.lagreTilkjentYtelse(any(), any()) }
    }

    @Test
    fun `for TSR kastes det feil dersom aktiviteten ikke finnes`() {
        val saksbehandlingTsr = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))
        val aktivitetId = VilkårperiodeGlobalId.random()
        mockHentVedtak(
            saksbehandlingTsr,
            listOf(
                lagBeregningsresultatForOffentligTransport(
                    fom = mandag,
                    aktivitetId = aktivitetId,
                    brukersNavKontor = "1234",
                ),
            ),
        )
        every {
            vilkårperiodeService.hentAktivitet(aktivitetId, saksbehandlingTsr.id)
        } returns null

        assertThrows<Feil> {
            service.lagreAndelerForBehandling(saksbehandlingTsr)
        }

        verify(exactly = 0) { tilkjentYtelseService.lagreTilkjentYtelse(any(), any()) }
    }

    @Test
    fun `for TSO slås det ikke opp aktivitet`() {
        val saksbehandlingTso = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))
        mockHentVedtak(
            saksbehandlingTso,
            listOf(lagBeregningsresultatForOffentligTransport(fom = mandag)),
        )
        every { tilkjentYtelseService.lagreTilkjentYtelse(behandlingId = saksbehandlingTso.id, andeler = any()) } returns mockk()

        service.lagreAndelerForBehandling(saksbehandlingTso)

        verify(exactly = 0) { vilkårperiodeService.hentAktivitet(any(), any()) }
    }

    private fun mockHentVedtak(
        saksbehandling: no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling,
        offentligTransport: List<no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport>,
    ) {
        val vedtak =
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                data =
                    InnvilgelseReiseTilSamling(
                        beregningsresultat =
                            BeregningsresultatReiseTilSamling(
                                offentligTransport = offentligTransport,
                                privatBil = emptyList(),
                            ),
                        vedtaksperioder = emptyList(),
                        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    ),
                gitVersjon = null,
                tidligsteEndring = null,
            )
        every { vedtakService.hentVedtakEllerFeil(saksbehandling.id) } returns vedtak
    }
}
