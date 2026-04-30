package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.BehandlingContext
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.TypeAvvikUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class UtledAvklartKjørtUkeTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
    }

    @Test
    fun `happy case - skal vurdere uke til automatisk ok dersom dager er innenfor ramme og parkering er under 100kr`() {
        val rammebehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    1 januar 2026,
                    4 januar 2026,
                    delperioder =
                        listOf(
                            FaktaDelperiodePrivatBilDto(
                                fom = 1 januar 2026,
                                tom = 4 januar 2026,
                                reisedagerPerUke = 2,
                                bompengerPerDag = null,
                                fergekostnadPerDag = null,
                            ),
                        ),
                )

                sendInnKjøreliste {
                    periode = Datoperiode(1 januar 2026, 4 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val innsendtUke = finnInnsendtUkeIKjørelistebehandling(rammebehandlingId)

        assertThat(innsendtUke.avvik).isNull()
        assertThat(innsendtUke.status).isEqualTo(UkeStatus.OK_AUTOMATISK)
        assertThat(innsendtUke.avklartUkeId).isNotNull()
        assertThat(innsendtUke.behandletDato).isNull() // TODO: Skal denne settes? = innsendt dato for kjøreliste?

        val forventedeDager =
            listOf(
                avklartKjørtDag(
                    1 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = 50,
                ),
                avklartKjørtDag(
                    2 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = 50,
                ),
                avklartKjørtDag(
                    3 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = null,
                ),
                avklartKjørtDag(
                    4 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = null,
                ),
            )

        sammenlignDager(faktiskeDager = innsendtUke.dager, forventedeDager = forventedeDager)
    }

    @Test
    fun `skal melde avvik om antall dager i uke overskriver antall tillatte dager`() {
        val rammebehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    5 januar 2026,
                    31 januar 2026,
                    delperioder =
                        listOf(
                            FaktaDelperiodePrivatBilDto(
                                fom = 5 januar 2026,
                                tom = 31 januar 2026,
                                reisedagerPerUke = 3,
                                bompengerPerDag = null,
                                fergekostnadPerDag = null,
                            ),
                        ),
                )

                sendInnKjøreliste {
                    periode = Datoperiode(5 januar 2026, 11 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 8 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val innsendtUke = finnInnsendtUkeIKjørelistebehandling(rammebehandlingId)

        assertThat(innsendtUke.avvik!!.typeAvvik).isEqualTo(TypeAvvikUke.FLERE_REISEDAGER_ENN_I_RAMMEVEDTAK)
        assertThat(innsendtUke.status).isEqualTo(UkeStatus.AVVIK)
        assertThat(innsendtUke.avklartUkeId).isNotNull()
        assertThat(innsendtUke.behandletDato).isNull()

        val forventedeDager =
            listOf(
                avklartKjørtDag(
                    5 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    6 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    7 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    8 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    9 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    10 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
                avklartKjørtDag(
                    11 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.NEI,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                ),
            )

        sammenlignDager(faktiskeDager = innsendtUke.dager, forventedeDager = forventedeDager)
    }

    @Test
    fun `skal melde avvik på dag om parkeringsutgift overstiger 100 kr`() {
        val rammebehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(1 januar 2026, 2 januar 2026)

                sendInnKjøreliste {
                    periode = Datoperiode(1 januar 2026, 2 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 150),
                        )
                }
            }

        val innsendtUke = finnInnsendtUkeIKjørelistebehandling(rammebehandlingId)

        assertThat(innsendtUke.avvik).isNull()
        assertThat(innsendtUke.status).isEqualTo(UkeStatus.AVVIK)
        assertThat(innsendtUke.avklartUkeId).isNotNull()
        assertThat(innsendtUke.behandletDato).isNull()

        val forventedeDager =
            listOf(
                avklartKjørtDag(
                    1 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = 50,
                ),
                avklartKjørtDag(
                    2 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.AVVIK,
                    avvik = listOf(TypeAvvikDag.FOR_HØY_PARKERINGSUTGIFT),
                    parkeringsutgift = null,
                ),
            )

        sammenlignDager(faktiskeDager = innsendtUke.dager, forventedeDager = forventedeDager)
    }

    @Test
    fun `skal melde avvik på dag dersom det er en helgedag`() {
        val rammebehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(1 januar 2026, 4 januar 2026)

                sendInnKjøreliste {
                    periode = Datoperiode(1 januar 2026, 4 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 1 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 2 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 3 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 4 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val innsendtUke = finnInnsendtUkeIKjørelistebehandling(rammebehandlingId)

        assertThat(innsendtUke.avvik).isNull()
        assertThat(innsendtUke.status).isEqualTo(UkeStatus.AVVIK)
        assertThat(innsendtUke.avklartUkeId).isNotNull()
        assertThat(innsendtUke.behandletDato).isNull()

        val forventedeDager =
            listOf(
                avklartKjørtDag(
                    1 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = 50,
                ),
                avklartKjørtDag(
                    2 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                    parkeringsutgift = 50,
                ),
                avklartKjørtDag(
                    3 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    parkeringsutgift = null,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.AVVIK,
                    avvik = listOf(TypeAvvikDag.HELLIDAG_ELLER_HELG),
                ),
                avklartKjørtDag(
                    4 januar 2026,
                    godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.IKKE_VURDERT,
                    parkeringsutgift = null,
                    automatiskVurdering = UtfyltDagAutomatiskVurdering.AVVIK,
                    avvik = listOf(TypeAvvikDag.HELLIDAG_ELLER_HELG),
                ),
            )

        sammenlignDager(faktiskeDager = innsendtUke.dager, forventedeDager = forventedeDager)
    }

    @Test
    fun `kun status skal være satt for alle uker hvor kjøreliste ikke er sendt inn`() {
        val rammebehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    5 januar 2026,
                    31 januar 2026,
                    delperioder =
                        listOf(
                            FaktaDelperiodePrivatBilDto(
                                fom = 5 januar 2026,
                                tom = 31 januar 2026,
                                reisedagerPerUke = 3,
                                bompengerPerDag = null,
                                fergekostnadPerDag = null,
                            ),
                        ),
                )

                sendInnKjøreliste {
                    periode = Datoperiode(5 januar 2026, 11 januar 2026)
                    kjørteDager =
                        listOf(
                            KjørtDag(dato = 5 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 6 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 7 januar 2026, parkeringsutgift = 50),
                            KjørtDag(dato = 8 januar 2026, parkeringsutgift = 50),
                        )
                }
            }

        val kjørelistebehandling = finnKjørelistebehandling(rammebehandlingId)
        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id).single()

        val ikkeInnsendteUker = reisevurdering.uker.filter { it.kjørelisteId == null }

        ikkeInnsendteUker.forEach { uke ->
            assertThat(uke.status).isEqualTo(UkeStatus.IKKE_MOTTATT_KJØRELISTE)
            assertThat(uke.avvik).isNull()
            assertThat(uke.kjørelisteId).isNull()
            assertThat(uke.avklartUkeId).isNull()
            assertThat(uke.behandletDato).isNull()

            uke.dager.forEach { dag ->
                assertThat(dag.avklartDag).isNull()
                assertThat(dag.kjørelisteDag).isNull()
            }
        }
    }

    private fun finnKjørelistebehandling(behandlingContext: BehandlingContext): Behandling {
        val rammebehandling = behandlingService.hentSaksbehandling(behandlingContext.behandlingId)

        return behandlingService
            .hentBehandlinger(rammebehandling.fagsakId)
            .first { it.type == BehandlingType.KJØRELISTE }
    }

    private fun finnInnsendtUkeIKjørelistebehandling(rammevedtakBehandlingContext: BehandlingContext): UkeVurderingDto {
        val kjørelistebehandling = finnKjørelistebehandling(rammevedtakBehandlingContext)
        val reisevurdering = kall.privatBil.hentReisevurderingForBehandling(kjørelistebehandling.id).single()

        return reisevurdering.uker.single { it.kjørelisteId != null }
    }

    private fun sammenlignDager(
        faktiskeDager: List<DagDto>,
        forventedeDager: List<AvklartKjørtDag>,
    ) {
        assertThat(faktiskeDager).hasSameSizeAs(forventedeDager)

        faktiskeDager.zip(forventedeDager) { faktisk, forventet ->
            assertThat(faktisk.avklartDag?.godkjentGjennomførtKjøring).isEqualTo(forventet.godkjentGjennomførtKjøring)
            assertThat(faktisk.avklartDag?.parkeringsutgift).isEqualTo(forventet.parkeringsutgift)
            assertThat(faktisk.avklartDag?.automatiskVurdering).isEqualTo(forventet.automatiskVurdering)
            assertThat(faktisk.avklartDag?.avvik).isEqualTo(forventet.avvik)
            assertThat(faktisk.avklartDag?.begrunnelse).isEqualTo(forventet.begrunnelse)
        }
    }

    private fun avklartKjørtDag(
        dato: LocalDate,
        godkjentGjennomførtKjøring: GodkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
        parkeringsutgift: Int? = null,
        automatiskVurdering: UtfyltDagAutomatiskVurdering,
        avvik: List<TypeAvvikDag> = emptyList(),
    ) = AvklartKjørtDag(
        dato = dato,
        godkjentGjennomførtKjøring = godkjentGjennomførtKjøring,
        parkeringsutgift = parkeringsutgift,
        begrunnelse = null,
        avvik = avvik,
        automatiskVurdering = automatiskVurdering,
    )
}
