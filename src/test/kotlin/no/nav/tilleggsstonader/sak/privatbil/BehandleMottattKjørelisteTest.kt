package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.Oppgavestatus
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseVedtakService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class BehandleMottattKjørelisteTest : CleanDatabaseIntegrationTest() {
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

    @Test
    fun `ta i mot kjøreliste uten avvik og opprett automatisk behandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns true

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
        val behandling = behandlingService.hentSaksbehandling(behandlingContext.behandlingId)
        val alleBehandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)

        val kjørelistebehandling = alleBehandlinger.first { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelistebehandling).isNotNull()
        assertThat(kjørelistebehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)
        assertThat(kjørelistebehandling.steg).isIn(
            StegType.FULLFØR_KJØRELISTE,
            StegType.FERDIGSTILLE_BEHANDLING,
            StegType.BEHANDLING_FERDIGSTILT,
        )
        assertThat(kjørelistebehandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(kjørelistebehandling.årsak).isEqualTo(BehandlingÅrsak.KJØRELISTE)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.AUTOMATISK)

        val kjørelisteoppgaver = oppgaveService.finnAlleOppgaveDomainForBehandling(kjørelistebehandling.id)
        assertThat(kjørelisteoppgaver).isEmpty()

        val vedtakForrigeBehandling = vedtakService.hentInnvilgelseEllerOpphørVedtak(behandlingContext.behandlingId)
        val vedtakKjørelistebehandling = vedtakService.hentInnvilgelseEllerOpphørVedtak(kjørelistebehandling.id)

        assertThat(vedtakKjørelistebehandling.type).isEqualTo(vedtakForrigeBehandling.type)
        assertThat(vedtakKjørelistebehandling.data.rammevedtakPrivatBil).isEqualTo(vedtakForrigeBehandling.data.rammevedtakPrivatBil)
        assertThat(vedtakKjørelistebehandling.data.vedtaksperioder).isEqualTo(vedtakForrigeBehandling.data.vedtaksperioder)

        val vilkårperioderForrigeBehandling = vilkårperiodeService.hentVilkårperioder(behandling.id)
        val vilkårperioderKjørelistebehandling = vilkårperiodeService.hentVilkårperioder(kjørelistebehandling.id)

        assertThat(vilkårperioderKjørelistebehandling.målgrupper).hasSameSizeAs(vilkårperioderForrigeBehandling.målgrupper)
        assertThat(vilkårperioderKjørelistebehandling.aktiviteter).hasSameSizeAs(vilkårperioderForrigeBehandling.aktiviteter)

        val vilkårForrigeBehandling = vilkårService.hentVilkårForBehandling(behandling.id)
        val vilkårKjørelistebehandling = vilkårService.hentVilkårForBehandling(kjørelistebehandling.id)

        assertThat(vilkårKjørelistebehandling).hasSameSizeAs(vilkårForrigeBehandling)
    }

    @Test
    fun `ta i mot kjøreliste uten avvik og ikke opprett automatisk behandling når bryter er false`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns false

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

        val behandling = behandlingService.hentSaksbehandling(behandlingContext.behandlingId)
        val alleBehandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)

        val kjørelistebehandling = alleBehandlinger.first { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelistebehandling.steg).isEqualTo(StegType.KJØRELISTE)
        assertThat(kjørelistebehandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(kjørelistebehandling.årsak).isEqualTo(BehandlingÅrsak.KJØRELISTE)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.MANUELL)

        val kjørelisteoppgaver = oppgaveService.finnAlleOppgaveDomainForBehandling(kjørelistebehandling.id)
        assertThat(kjørelisteoppgaver).hasSize(1)
        assertThat(kjørelisteoppgaver.single().status).isEqualTo(Oppgavestatus.ÅPEN)
    }

    @Test
    fun `ta i mot kjøreliste med parkeringsutgift 101 kroner og krev manuell behandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns false
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 2 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 101),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 50),
                        )
                }
            }
        val behandling = behandlingService.hentSaksbehandling(behandlingContext.behandlingId)
        val alleBehandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)

        val kjørelistebehandling = alleBehandlinger.first { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelistebehandling.steg).isEqualTo(StegType.KJØRELISTE)
        assertThat(kjørelistebehandling.type).isEqualTo(BehandlingType.KJØRELISTE)
        assertThat(kjørelistebehandling.årsak).isEqualTo(BehandlingÅrsak.KJØRELISTE)
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.MANUELL)

        val kjørelisteoppgaver = oppgaveService.finnAlleOppgaveDomainForBehandling(kjørelistebehandling.id)
        assertThat(kjørelisteoppgaver).hasSize(1)
        assertThat(kjørelisteoppgaver.single().status).isEqualTo(Oppgavestatus.ÅPEN)
    }
}
