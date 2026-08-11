package no.nav.tilleggsstonader.sak.satsjustering

import io.mockk.clearMocks
import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteStegKall
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.sats.satser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class SatsjusteringDagligReisePrivatBilTest(
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var satsPrivatBilProvider: SatsPrivatBilProvider

    val sisteBekreftedeSatsÅr = satser.filter { it.bekreftet }.maxOf { it.fom.year }
    val fom = UkeIÅr(2, sisteBekreftedeSatsÅr + 1).mandag()
    val tom = UkeIÅr(2, sisteBekreftedeSatsÅr + 1).søndag()

    private fun mockSatser() {
        val bekreftedeSatser = satsPrivatBilProvider.alleSatser.filter { it.bekreftet }
        val ubekreftetSats = satsPrivatBilProvider.alleSatser.first { !it.bekreftet }
        val nyBrekeftetSats =
            ubekreftetSats.copy(
                tom =
                    ubekreftetSats.fom
                        .toYearMonth()
                        .withMonth(12)
                        .atEndOfMonth(),
                bekreftet = true,
                beløp = 10.toBigDecimal(),
            )

        val nyUbrekeftetSats =
            ubekreftetSats.copy(
                fom = ubekreftetSats.fom.plusYears(1),
                beløp = 20.toBigDecimal(),
            )

        every {
            satsPrivatBilProvider.alleSatser
        } returns bekreftedeSatser + nyBrekeftetSats + nyUbrekeftetSats
    }

    @AfterEach
    fun resetMock() {
        clearMocks(satsPrivatBilProvider)
    }

    @BeforeEach
    fun settOppUnleash() {
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns true
    }

    @Test
    fun `dersom tidligere rammevedtak har ubekreftede satser skal disse oppdateres hvis tilgjengelig`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
            ) {
                defaultDagligReisePrivatBilTsrTestdata(
                    fom = fom,
                    tom = tom,
                )
            }

        val vedtak =
            kall.vedtak
                .hentVedtak(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    behandlingId = behandlingContext.behandlingId,
                ).expectOkWithBody<InnvilgelseDagligReiseResponse>()

        val rammevedtak = vedtak.rammevedtakPrivatBil!!.reiser.single()

        val sats =
            rammevedtak.delperioder
                .single()
                .satser
                .single()

        assertThat(sats.satsBekreftetVedVedtakstidspunkt).isFalse

        mockSatser()

        sendInnKjøreliste(
            kjøreliste =
                kjørelisteSkjema(
                    reiseId = rammevedtak.reiseId.toString(),
                    periode = Datoperiode(fom = fom, tom = tom),
                    dagerKjørt =
                        listOf(
                            KjørtDag(dato = fom, parkeringsutgift = 50),
                        ),
                ),
            ident = behandlingContext.ident,
        )

        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelisteBehandling.erFerdigstilt()).isTrue

        val tilkjenteYtelser = tilkjentYtelseRepository.findByBehandlingId(kjørelisteBehandling.id)

        assertThat(tilkjenteYtelser!!.andelerTilkjentYtelse).hasSize(1)

        val vedtakKjøreliste =
            kall.vedtak
                .hentVedtak(
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                    behandlingId = kjørelisteBehandling.id,
                ).expectOkWithBody<InnvilgelseDagligReiseResponse>()

        val rammevedtakKjøreliste = vedtakKjøreliste.rammevedtakPrivatBil!!.reiser.single()

        assertThat(rammevedtakKjøreliste.delperioder).hasSize(1)
        assertThat(
            rammevedtakKjøreliste.delperioder
                .single()
                .satser,
        ).hasSize(1)
        assertThat(
            rammevedtakKjøreliste.delperioder
                .single()
                .satser
                .single()
                .kilometersats,
        ).isEqualTo(10.toBigDecimal())
        assertThat(
            rammevedtakKjøreliste.delperioder
                .single()
                .satser
                .single()
                .satsBekreftetVedVedtakstidspunkt,
        ).isTrue
    }

    @Test
    fun `dersom det sendes inn kjøreliste og ingen satser er tilgjenglig skal steget feile`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    fom = fom,
                    tom = tom,
                )

                sendInnKjøreliste {
                    periode = Datoperiode(fom = fom, tom = tom)
                    kjørteDager =
                        listOf(
                            // For høy parkeringsutgift for å sette manuell behandling av kjøreliste
                            KjørtDag(dato = fom, parkeringsutgift = 5000),
                        )
                }
            }

        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelisteBehandling.erFerdigstilt()).isFalse

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelisteBehandling.id).single()

        val avklartUke = reisevurdering.uker.single()

        gjennomførKjørelisteBehandling(
            behandling = kjørelisteBehandling,
            tilSteg = StegType.KJØRELISTE,
        )

        kall.privatBil.oppdaterUke(
            behandlingId = kjørelisteBehandling.id,
            avklartUkeId = avklartUke.avklartUkeId!!,
            avklarteDager =
                avklartUke.dager.map {
                    val erDagenSomSkalAvklares = it.dato == fom

                    EndreAvklartDagRequest(
                        dato = it.dato,
                        godkjentGjennomførtKjøring =
                            if (erDagenSomSkalAvklares) {
                                GodkjentGjennomførtKjøring.JA
                            } else {
                                GodkjentGjennomførtKjøring.NEI
                            },
                        parkeringsutgift = it.kjørelisteDag!!.parkeringsutgift,
                        begrunnelse = if (erDagenSomSkalAvklares) "Begrunnelse" else null,
                    )
                },
        )

        gjennomførKjørelisteStegKall(
            behandlingId = kjørelisteBehandling.id,
        ).expectProblemDetail(
            forventetStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            forventetDetail = "Kjøreliste inneholder dager som ikke har tilgjengelig sats",
        )
    }
}
