package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EndreAvklarteUkerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var behandlingService: BehandlingService

    val fom = 5 januar 2026
    val tom = 11 januar 2026

    @Test
    fun `ta i mot kjøreliste og opprett behandling med kopierte verdier`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager =
                        listOf(
                            5 januar 2026 to 50,
                            6 januar 2026 to 50,
                            7 januar 2026 to 50,
                            8 januar 2026 to 50,
                            9 januar 2026 to 50,
                            10 januar 2026 to 50,
                            11 januar 2026 to 50,
                        )
                }
            }

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val kjørelistebehandling =
            behandlingService.hentBehandlinger(behandling.fagsakId).first { it.type == BehandlingType.REVURDERING }

        gjennomførBehandlingsløp(behandlingId = kjørelistebehandling.id, tilSteg = StegType.KJØRELISTE) {}

        val behandlingOppdatert = behandlingService.hentSaksbehandling(kjørelistebehandling.id)

        assertThat(behandlingOppdatert.steg).isEqualTo(StegType.KJØRELISTE)

        val test = kall.privatBil.hentKjørelisteForBehandling(kjørelistebehandling.id)
        assertThat(test).hasSize(1)

        val innsendtUke = test.first().uker.single()

        val oppdatertUke =
            kall.privatBil.oppdaterUke(
                behandlingId = kjørelistebehandling.id,
                avklartUkeId = innsendtUke.avklartUkeId!!,
                listOf(
                    EndreAvklartDagRequest(
                        dato = 5 januar 2026,
                        godkjentGjennomførtKjøring = true,
                        parkeringsutgift = 50,
                    ),
                    EndreAvklartDagRequest(
                        dato = 6 januar 2026,
                        godkjentGjennomførtKjøring = true,
                        parkeringsutgift = 50,
                    ),
                    EndreAvklartDagRequest(
                        dato = 7 januar 2026,
                        godkjentGjennomførtKjøring = true,
                        parkeringsutgift = 50,
                    ),
                    EndreAvklartDagRequest(
                        dato = 8 januar 2026,
                        godkjentGjennomførtKjøring = true,
                        parkeringsutgift = 50,
                    ),
                    EndreAvklartDagRequest(
                        dato = 9 januar 2026,
                        godkjentGjennomførtKjøring = true,
                        parkeringsutgift = 50,
                    ),
                    EndreAvklartDagRequest(
                        dato = 10 januar 2026,
                        godkjentGjennomførtKjøring = false,
                        parkeringsutgift = null,
                        begrunnelse = "helg",
                    ),
                    EndreAvklartDagRequest(
                        dato = 11 januar 2026,
                        godkjentGjennomførtKjøring = false,
                        parkeringsutgift = null,
                        begrunnelse = "helg",
                    ),
                ),
            )

        assertThat(oppdatertUke.status).isEqualTo(UkeStatus.OK_MANUELT)
    }
}
