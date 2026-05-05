package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseVedtakService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskKjørelisteBehandlingTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var vedtakService: DagligReiseVedtakService

    @Autowired
    private lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    private lateinit var vilkårService: DagligReiseVilkårService

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    val fom: LocalDate = 1 januar 2026
    val tom: LocalDate = 31 januar 2026

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns true
    }

    @Test
    fun `ta i mot kjøreliste uten avvik og opprett automatisk behandling`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 2 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 100),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 50),
                        )
                }
            }
        val behandlinger = behandlingService.hentBehandlinger(behandlingContext.fagsakId)

        val kjørelistebehandling = behandlinger.single { it.type == BehandlingType.KJØRELISTE }

        assertThat(behandlinger).hasSize(2)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.AUTOMATISK)
        assertThat(kjørelistebehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
    }

    @Test
    fun `ta i mot kjøreliste med avvik på parkeringsutgifter og opprett manuell behandling`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 2 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 150),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 150),
                        )
                }
            }
        val behandlinger = behandlingService.hentBehandlinger(behandlingContext.fagsakId)

        val kjørelistebehandling = behandlinger.single { it.type == BehandlingType.KJØRELISTE }

        assertThat(behandlinger).hasSize(2)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.MANUELL)
        assertThat(kjørelistebehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
    }

    @Test
    fun `ta i mot kjøreliste med avvik på helg og opprett manuell behandling`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 4 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 3 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 4 januar 2026, parkeringsutgift = 50),
                        )
                }
            }
        val behandlinger = behandlingService.hentBehandlinger(behandlingContext.fagsakId)

        val kjørelistebehandling = behandlinger.single { it.type == BehandlingType.KJØRELISTE }

        assertThat(behandlinger).hasSize(2)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.MANUELL)
        assertThat(kjørelistebehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)
    }
}
