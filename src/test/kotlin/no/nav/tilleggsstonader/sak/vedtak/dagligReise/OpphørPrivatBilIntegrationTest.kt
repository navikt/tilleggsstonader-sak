package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDagStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OpphørPrivatBilIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val tilkjentYtelseService: TilkjentYtelseService,
) : IntegrationTest() {
    @Test
    fun `skal kutte rammevedtak og innsendte kjørelister i opphør`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 11 mars 2026

        // Opprett førstegangsbehandling og send inn kjøreliste
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 2 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 9 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 16 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 23 februar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 2 mars 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 9 mars 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 12 mars 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 16 mars 2026, parkeringsutgift = 50),
                        )
                }
            }

        // Behandle kjørelista
        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsbehandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelistebehandling)
        testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)
        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandlingContext.behandlingId)

        // Opphør
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                tilSteg = StegType.SIMULERING,
            ) {
                vedtak {
                    opphør(opphørsdato = opphørsdato)
                }
            }

        val opphørsvedtak = vedtakService.hentVedtak<OpphørDagligReise>(revurderingId).data
        testoppsettService.settAndelerTilOkForBehandling(revurderingId)

        val rammevedtak = opphørsvedtak.rammevedtakPrivatBil
        assertThat(rammevedtak).isNotNull
        assertThat(
            opphørsvedtak.rammevedtakPrivatBil!!
                .reiser
                .single()
                .grunnlag.tom,
        ).isEqualTo(opphørsdato.minusDays(1))

        val beregningsresultatPrivatBil = opphørsvedtak.beregningsresultat.privatBil
        assertThat(beregningsresultatPrivatBil).isNotNull
        assertThat(beregningsresultatPrivatBil!!.reiser).hasSize(1)

        val ukerIReise = beregningsresultatPrivatBil.reiser.single().perioder
        assertThat(ukerIReise).hasSize(6)
        assertThat(ukerIReise.maxOf { it.tom }).isEqualTo(opphørsdato.minusDays(1))

        val dagerISisteUke =
            beregningsresultatPrivatBil.reiser
                .single()
                .perioder
                .maxBy { it.fom }
                .grunnlag.dager
        assertThat(dagerISisteUke).hasSize(1)
        assertThat(dagerISisteUke.single().dato).isEqualTo(9 mars 2026)

        val andeler = tilkjentYtelseService.hentForBehandling(revurderingId).andelerTilkjentYtelse
        assertThat(andeler.maxOf { it.fom }).isBefore(opphørsdato.minusDays(1))
    }

    @Test
    fun `skal gjenopprette kjøreliste etter opphør selv om det er mellomliggende behandlinger`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 9 mars 2026 // mandag - start av uke 10

        // 1. FGB med kjøreliste for 9 mars (uke 10)
        val fgbContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager = listOf(KjørtDag(dato = opphørsdato))
                }
            }
        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(fgbContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelistebehandling)
        testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)
        testoppsettService.settAndelerTilOkForBehandling(fgbContext.behandlingId)

        // 2. Opphørsbehandling (FERDIGSTILT): opphørsdato = 9 mars → uke 10 SLETTET
        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = fgbContext.behandlingId) {
                vedtak { opphør(opphørsdato = opphørsdato) }
            }
        testoppsettService.settAndelerTilOkForBehandling(opphørId)

        // 3. Mellomliggende revurdering: uke 10 fortsatt utenfor (vilkår tom = 8 mars)
        val mellomliggendeId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = opphørId) {
                vilkår {
                    oppdaterDatoPåEnesteDagligeReise(fom = fom, tom = opphørsdato.minusDays(1))
                }
            }
        testoppsettService.settAndelerTilOkForBehandling(mellomliggendeId)

        // 4. Ny innvilgelse som inkluderer uke 10 igjen (vilkår tom = 20 mars)
        val nyInnvilgelseId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = mellomliggendeId) {
                vilkår {
                    oppdaterDatoPåEnesteDagligeReise(fom = fom, tom = tom)
                }
            }

        // 5. Verifiser at uke 10 er gjenopprettet: kjørelisteInnsendtDato og avklartUkeId er satt, status er NY
        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(nyInnvilgelseId).single()
        val uke10 = reisevurdering.uker.single { it.fraDato == opphørsdato }
        assertThat(uke10.avklartUkeId).isNotNull()
        assertThat(uke10.kjørelisteInnsendtDato).isNotNull()
        assertThat(uke10.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.NY)
    }

    @Test
    fun `skal gjenopprette slettet enkeltdag i eksisterende uke når rammevedtaket utvides igjen`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true

        val fom = 2 februar 2026
        val tom = 20 mars 2026
        val opphørsdato = 11 mars 2026

        val førstegangsbehandling =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                sendInnKjøreliste {
                    periode = Datoperiode(9 mars 2026, 15 mars 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 9 mars 2026),
                            KjørtDag(dato = 11 mars 2026),
                        )
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(førstegangsbehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        gjennomførKjørelisteBehandling(kjørelistebehandling)
        testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)
        testoppsettService.settAndelerTilOkForBehandling(førstegangsbehandling.behandlingId)

        val opphørId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = førstegangsbehandling.behandlingId) {
                vedtak { opphør(opphørsdato = opphørsdato) }
            }
        testoppsettService.settAndelerTilOkForBehandling(opphørId)

        val nyInnvilgelseId =
            opprettRevurderingOgGjennomførBehandlingsløp(fraBehandlingId = opphørId) {
                vilkår {
                    oppdaterDatoPåEnesteDagligeReise(fom = fom, tom = tom)
                }
            }

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(nyInnvilgelseId).single()
        val uke10 = reisevurdering.uker.single { it.fraDato == 9 mars 2026 }

        assertThat(uke10.avklartKjørtUkeStatus).isEqualTo(AvklartKjørtUkeStatus.ENDRET)
        val gjenopprettetDag = uke10.dager.single { it.dato == 11 mars 2026 }
        assertThat(gjenopprettetDag.avklartDag).isNotNull()
        assertThat(gjenopprettetDag.avklartDag!!.avklartKjørtDagStatus).isEqualTo(AvklartKjørtDagStatus.NY)
        assertThat(uke10.dager.single { it.dato == 9 mars 2026 }.avklartDag).isNotNull()
    }
}
