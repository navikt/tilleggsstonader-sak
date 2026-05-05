package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.fjernTilordningPåÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MottaKjørelisteIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `daglig-reise tso sak med innvilget rammevedtak, mottar kjøreliste, verifiser blir journalført og lagret`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 1 januar 2026
        val tom = 14 januar 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingContext.behandlingId)

        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)
        val reiseId = rammevedtak.single().reiseId

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.single().fom).isEqualTo(fom)
        assertThat(rammevedtak.single().tom).isEqualTo(tom)

        val dagerKjørt =
            listOf(
                KjørtDag(dato = 1 januar 2026, parkeringsutgift = 50),
                KjørtDag(dato = 4 januar 2026, parkeringsutgift = 70),
                KjørtDag(dato = 9 januar 2026, parkeringsutgift = 40),
                KjørtDag(dato = 11 januar 2026, parkeringsutgift = 90),
            )
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId,
                periode = Datoperiode(fom, tom),
                dagerKjørt = dagerKjørt,
            )

        val journalpostId = sendInnKjøreliste(kjøreliste, behandlingContext.ident)

        val lagredeKjørelister = kjørelisteRepository.findByFagsakId(saksbehandling.fagsakId)
        assertThat(lagredeKjørelister).hasSize(1)
        val lagretKjøreliste = lagredeKjørelister.single()
        assertThat(lagretKjøreliste.fagsakId).isEqualTo(saksbehandling.fagsakId)
        assertThat(lagretKjøreliste.journalpostId).isEqualTo(journalpostId)
        assertThat(lagretKjøreliste.data.reiseId.toString()).isEqualTo(reiseId)
        assertLagretKjørelisteInneholderDager(innsendtKjøreliste = kjøreliste, lagretKjøreliste = lagretKjøreliste)

        val behandlingerPåFagsak = behandlingRepository.findByFagsakId(saksbehandling.fagsakId)
        val kjørelisteBehandling = behandlingerPåFagsak.single { it.årsak == BehandlingÅrsak.KJØRELISTE }
        val oppgaver = oppgaveRepository.findByBehandlingId(kjørelisteBehandling.id)
        assertThat(oppgaver).hasSize(1)
        assertThat(oppgaver.single().type).isEqualTo(Oppgavetype.BehandleKjøreliste)
    }

    @Test
    fun `skal kun opprettes en kjørelistebehandling om det kommer inn to kjørelister etter hverandre`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 mars 2026
        val tom = 15 mars 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 8 mars 2026)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)
        val reiseId = rammevedtak.single().reiseId

        assertThat(
            testoppsettService.hentBehandlinger(fagsakId = behandlingContext.fagsakId).filter {
                it.type == BehandlingType.KJØRELISTE
            },
        ).hasSize(1)

        sendInnKjøreliste(
            kjøreliste =
                kjørelisteSkjema(
                    reiseId = reiseId,
                    periode = Datoperiode(9 mars 2026, tom),
                    dagerKjørt =
                        listOf(
                            KjørtDag(dato = 9 mars 2026, parkeringsutgift = 50),
                        ),
                ),
            ident = behandlingContext.ident,
        )

        val kjørelistebehandlinger =
            testoppsettService.hentBehandlinger(fagsakId = behandlingContext.fagsakId).filter {
                it.type ==
                    BehandlingType.KJØRELISTE
            }
        assertThat(kjørelistebehandlinger).hasSize(1)
        val kjørelistebehandling = kjørelistebehandlinger.single()

        val reisevurderingForReise =
            kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id).single {
                it.reiseId ==
                    ReiseId.fromString(reiseId)
            }
        assertThat(reisevurderingForReise.uker).allMatch { it.kjørelisteInnsendtDato != null }
    }

    @Test
    fun `skal opprette to kjørelistebehandlinger om den første ikke er ferdigstilt men er påbegynt`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 mars 2026
        val tom = 15 mars 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 8 mars 2026)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)
        val reiseId = rammevedtak.single().reiseId

        val kjørelistebehandling =
            testoppsettService.hentBehandlinger(fagsakId = behandlingContext.fagsakId).single {
                it.type ==
                    BehandlingType.KJØRELISTE
            }

        tilordneÅpenBehandlingOppgaveForBehandling(kjørelistebehandling.id)

        sendInnKjøreliste(
            kjøreliste =
                kjørelisteSkjema(
                    reiseId = reiseId,
                    periode = Datoperiode(9 mars 2026, tom),
                    dagerKjørt =
                        listOf(
                            KjørtDag(dato = 9 mars 2026, parkeringsutgift = 50),
                        ),
                ),
            ident = behandlingContext.ident,
        )

        assertThat(
            testoppsettService.hentBehandlinger(fagsakId = behandlingContext.fagsakId).filter {
                it.type == BehandlingType.KJØRELISTE
            },
        ).hasSize(2)
    }

    @Test
    fun `skal opprette ny kjørelistebehandling når første er manuelt endret og ikke lenger tilordnet`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 2 mars 2026
        val tom = 15 mars 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 8 mars 2026)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        val rammevedtak = kall.privatBil.hentRammevedtak(behandlingContext.ident)
        val reiseId = rammevedtak.single().reiseId

        val førsteKjørelistebehandling =
            testoppsettService
                .hentBehandlinger(fagsakId = behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        tilordneÅpenBehandlingOppgaveForBehandling(førsteKjørelistebehandling.id)

        val ukeForManuellEndring =
            kall.privatBil
                .hentReisevurderingForBehandling(førsteKjørelistebehandling.id)
                .single { it.reiseId == ReiseId.fromString(reiseId) }
                .uker
                .first { it.avklartUkeId != null }
        val dagerSomKanOppdateres = ukeForManuellEndring.dager.filter { it.avklartDag != null }
        val dagForManuellEndring = dagerSomKanOppdateres.first { it.kjørelisteDag?.harKjørt == true }

        val oppdaterteDager =
            dagerSomKanOppdateres.map { dag ->
                val avklartDag = dag.avklartDag!!
                if (dag.dato == dagForManuellEndring.dato) {
                    EndreAvklartDagRequest(
                        dato = dag.dato,
                        godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                        begrunnelse = "Manuelt vurdert",
                        parkeringsutgift = null,
                    )
                } else {
                    EndreAvklartDagRequest(
                        dato = dag.dato,
                        godkjentGjennomførtKjøring = avklartDag.godkjentGjennomførtKjøring,
                        begrunnelse = avklartDag.begrunnelse,
                        parkeringsutgift = avklartDag.parkeringsutgift,
                    )
                }
            }

        kall.privatBil.oppdaterUke(
            behandlingId = førsteKjørelistebehandling.id,
            avklartUkeId = ukeForManuellEndring.avklartUkeId!!,
            avklarteDager = oppdaterteDager,
        )

        fjernTilordningPåÅpenBehandlingOppgaveForBehandling(førsteKjørelistebehandling.id)

        sendInnKjøreliste(
            kjøreliste =
                kjørelisteSkjema(
                    reiseId = reiseId,
                    periode = Datoperiode(9 mars 2026, tom),
                    dagerKjørt =
                        listOf(
                            KjørtDag(dato = 9 mars 2026, parkeringsutgift = 50),
                        ),
                ),
            ident = behandlingContext.ident,
        )

        assertThat(
            testoppsettService.hentBehandlinger(fagsakId = behandlingContext.fagsakId).filter {
                it.type == BehandlingType.KJØRELISTE
            },
        ).hasSize(2)
    }

    private fun assertLagretKjørelisteInneholderDager(
        innsendtKjøreliste: KjørelisteSkjema,
        lagretKjøreliste: Kjøreliste,
    ) {
        val innsendteReisedager = innsendtKjøreliste.reisedagerPerUkeAvsnitt.flatMap { it.reisedager }
        val lagredeReisedager = lagretKjøreliste.data.reisedager
        assertThat(innsendteReisedager).hasSameSizeAs(lagredeReisedager)

        innsendteReisedager
            .forEach { innsendtReisedag ->
                val lagretDag = lagredeReisedager.singleOrNull { it.dato == innsendtReisedag.dato.verdi }
                assertThat(lagretDag).isNotNull
                assertThat(lagretDag!!.parkeringsutgift).isEqualTo(innsendtReisedag.parkeringsutgift.verdi)
                assertThat(lagretDag.harKjørt).isEqualTo(innsendtReisedag.harKjørt)
            }
    }
}
