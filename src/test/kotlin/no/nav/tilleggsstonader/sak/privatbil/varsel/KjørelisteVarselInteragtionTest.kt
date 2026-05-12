package no.nav.tilleggsstonader.sak.privatbil.varsel

import io.mockk.every
import net.javacrumbs.shedlock.core.LockAssert
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class KjørelisteVarselInteragtionTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var kjørelisteVarselScheduledService: KjørelisteVarselScheduledService

    val dagensDato = LocalDate.now()
    val denneUka = dagensDato.tilUkeIÅr()
    val enUkeTilbake = dagensDato.minusWeeks(1).tilUkeIÅr()
    val toUkerTilbake = dagensDato.minusWeeks(2).tilUkeIÅr()

    @BeforeEach
    fun `ikke lås shedlock`() {
        LockAssert.TestHelper.makeAllAssertsPass(true)
    }

    @Test
    fun `skal sende kjørelistevarsel hvis ingen uker er sendt inn`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = dagensDato.minusWeeks(3)
        val tom = dagensDato.plusWeeks(3)

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        // Et varsel sendt ved vedtakstidspunkt
        assertAntallVarslingerErSendt(1)
        kjørKjørelistevarselJobb()
        // Et varsel sendt av jobb
        assertAntallVarslingerErSendt(2)
    }

    @Test
    fun `skal sende kjørelistevarsel hvis forrige uke ikke er sendt inn`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = toUkerTilbake.mandag()
        val tom = denneUka.søndag()

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)

            sendInnKjøreliste {
                periode = Datoperiode(toUkerTilbake.mandag(), toUkerTilbake.søndag())
                kjørteDager = listOf(KjørtDag(dato = toUkerTilbake.mandag()))
            }
        }

        assertAntallVarslingerErSendt(1)
        kjørKjørelistevarselJobb()
        assertAntallVarslingerErSendt(2)
    }

    @Test
    fun `skal ikke sende kjørelistevarsel hvis forrige uke er sendt inn`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = toUkerTilbake.mandag()
        val tom = denneUka.søndag()

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)

            sendInnKjøreliste {
                periode = Datoperiode(toUkerTilbake.mandag(), enUkeTilbake.søndag())
                kjørteDager = listOf(KjørtDag(dato = toUkerTilbake.mandag()))
            }
        }

        assertAntallVarslingerErSendt(1)
        kjørKjørelistevarselJobb()
        assertAntallVarslingerErSendt(1)
    }

    @Test
    fun `skal sende kjørelistevarsel hvis et av to overlappende rammevedtak mangler kjøreliste forrige uke`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = enUkeTilbake.mandag()
        val tom = enUkeTilbake.søndag()

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            aktivitet {
                opprett {
                    aktivitetTiltakTso(fom, tom)
                }
            }
            målgruppe {
                opprett {
                    målgruppeAAP(fom, tom)
                }
            }
            vilkår {
                opprett {
                    // To rammevedtak
                    privatBil(fom, tom)
                    privatBil(fom, tom)
                }
            }

            // Sender inn kjøreliste på ett av rammevedtakene
            sendInnKjøreliste {
                reiseId { it.first().reiseId }
                periode = Datoperiode(fom, tom)
                kjørteDager =
                    listOf(
                        KjørtDag(fom),
                        KjørtDag(tom),
                    )
            }
        }

        assertAntallVarslingerErSendt(1)
        kjørKjørelistevarselJobb()
        assertAntallVarslingerErSendt(2)
    }

    @Test
    fun `skal ikke sende kjørelistevarsel hvis ikke har rammevedtak forrige uke`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = denneUka.mandag()
        val tom = denneUka.søndag()

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        assertAntallVarslingerErSendt(0)
        kjørKjørelistevarselJobb()
        assertAntallVarslingerErSendt(0)
    }

    private fun kjørKjørelistevarselJobb() {
        kjørelisteVarselScheduledService.sendVarselOmKjørelister()
        kjørTasksKlareForProsessering()
    }

    private fun assertAntallVarslingerErSendt(forventetAntall: Int) {
        KafkaFake
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.dittnav, forventetAntall)
    }
}
