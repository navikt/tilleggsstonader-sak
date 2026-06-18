package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OverskridAntallDagerIRammevedtakIntegrationTest : IntegrationTest() {
    @Test
    fun `skal kunne overskride antall dager i rammevedtak når toggle aktiveres ved manuell godkjenning og utbetale behandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OVERSKRIDE_ANTALL_DAGER_I_RAMMEVEDTAK) } returns false
        val fom = 5 januar 2026
        val tom = 11 januar 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    fom = fom,
                    tom = tom,
                    delperioder =
                        listOf(
                            FaktaDelperiodePrivatBilDto(
                                fom = fom,
                                tom = tom,
                                reisedagerPerUke = 2,
                                bompengerPerDag = null,
                                fergekostnadPerDag = null,
                            ),
                        ),
                )
                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 9 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelistebehandling.erAktiv()).isTrue()
        tilordneÅpenBehandlingOppgaveForBehandling(kjørelistebehandling.id)

        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id).single()
        val ukeMedAvvik =
            reisevurdering.uker.single {
                it.avvik?.typeAvvik == TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK
            }
        assertThat(ukeMedAvvik.status).isEqualTo(UkeStatus.AVVIK)

        every { unleashService.isEnabled(Toggle.KAN_OVERSKRIDE_ANTALL_DAGER_I_RAMMEVEDTAK) } returns true

        val oppdatertUke =
            kall.privatBil.oppdaterUke(
                behandlingId = kjørelistebehandling.id,
                avklartUkeId = ukeMedAvvik.avklartUkeId!!,
                avklarteDager =
                    ukeMedAvvik.dager.map { dag ->
                        val kjørelisteDag = dag.kjørelisteDag
                        val harKjørt = kjørelisteDag?.harKjørt == true
                        EndreAvklartDagRequest(
                            dato = dag.dato,
                            godkjentGjennomførtKjøring =
                                if (harKjørt) {
                                    GodkjentGjennomførtKjøring.JA
                                } else {
                                    GodkjentGjennomførtKjøring.NEI
                                },
                            parkeringsutgift = if (harKjørt) checkNotNull(kjørelisteDag).parkeringsutgift else null,
                            begrunnelse = "Manuell vurdering av uke med avvik",
                        )
                    },
            )

        assertThat(oppdatertUke.status).isEqualTo(UkeStatus.OK_MANUELT)
        assertThat(oppdatertUke.avvik?.typeAvvik).isEqualTo(TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK)
        assertThat(
            oppdatertUke.dager.count {
                it.avklartDag?.godkjentGjennomførtKjøring == GodkjentGjennomførtKjøring.JA
            },
        ).isEqualTo(3)

        gjennomførKjørelisteBehandling(kjørelistebehandling)

        val ferdigstiltBehandling = testoppsettService.hentBehandling(kjørelistebehandling.id)
        assertThat(ferdigstiltBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(ferdigstiltBehandling.resultat).isEqualTo(BehandlingResultat.INNVILGET)
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 1)
    }
}
