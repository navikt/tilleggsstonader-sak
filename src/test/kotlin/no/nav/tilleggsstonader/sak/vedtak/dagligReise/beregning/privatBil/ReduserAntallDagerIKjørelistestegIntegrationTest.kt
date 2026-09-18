package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteSteg
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.privatbil.UkeVurderingDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tester at en reduksjon av antall dager i kjøreliste-steget til en REVURDERING fører til at de
 * endrede ukene blir reberegnet i beregningsresultatet.
 *
 * Oppsett:
 * 1. Innvilger daglig reise TSR med privat bil over 8 uker.
 * 2. Bruker sender inn kjøreliste for de 4 første ukene med riktig antall dager (5/uke) og ingen
 *    parkeringsutgifter -> kjørelistebehandlingen blir automatisk ferdigstilt uten avvik.
 * 3. Det opprettes en revurdering UTEN endringer på vilkår, som stoppes rett før kjøreliste-steget.
 * 4. I kjøreliste-steget reduseres antall godkjente dager i de 4 innsendte ukene fra 5 til 3.
 * 5. Kjøreliste-steget fullføres og det forventes at beregningsresultatet for de endrede ukene blir
 *    reberegnet (færre dager og lavere stønadsbeløp).
 */
class ReduserAntallDagerIKjørelistestegIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
) : CleanDatabaseIntegrationTest() {
    private val fom = 5 januar 2026 // mandag
    private val tom = fom.plusWeeks(8).minusDays(1)

    private val fomKjøreliste = fom
    private val tomKjøreliste = fom.plusWeeks(4).minusDays(1)

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns true
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true
    }

    @Test
    fun `reduksjon av dager i kjøreliste-steget til en revurdering skal føre til reberegning av de endrede ukene`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSR) {
                defaultDagligReisePrivatBilTsrTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fomKjøreliste, tomKjøreliste)
                    kjørteDager = virkedager(fomKjøreliste, tomKjøreliste).map { KjørtDag(it) }
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        // Ingen avvik i innsendt kjøreliste -> automatisk ferdigstilt
        assertThat(kjørelistebehandling.behandlingMetode).isEqualTo(BehandlingMetode.AUTOMATISK)
        assertThat(kjørelistebehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)

        val perioderFørRevurdering = hentPerioder(kjørelistebehandling.id)

        // Oppretter en revurdering uten endringer på vilkår, og stopper rett før kjøreliste-steget
        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = kjørelistebehandling.id,
                tilSteg = StegType.KJØRELISTE,
            ) {}

        val ukerIRevurdering =
            kall.privatBil
                .hentReisevurderingForBehandling(revurderingId)
                .single()
                .uker
                .filter { it.kjørelisteId != null }
        assertThat(ukerIRevurdering).hasSize(4)

        // Reduserer antall godkjente dager fra 5 til 3 i alle innsendte uker for de siste tre ukene
        ukerIRevurdering.takeLast(3).forEach { uke ->
            kall.privatBil.oppdaterUke(
                behandlingId = revurderingId,
                avklartUkeId = uke.avklartUkeId!!,
                avklarteDager = reduserAntallDagerIUken(uke, behold = 3),
            )
        }

        val nesteSteg = gjennomførKjørelisteSteg(revurderingId)
        assertThat(nesteSteg).isEqualTo(StegType.BEREGNING)

        val perioderEtterRevurdering = hentPerioder(revurderingId)

        ukerIRevurdering.forEachIndexed { indeks, uke ->
            val periodeFør = perioderFørRevurdering.getValue(uke.fraDato)
            val periodeEtter = perioderEtterRevurdering.getValue(uke.fraDato)

            assertThat(periodeFør.grunnlag.dager).hasSize(5)

            // Den første uken er ikke oppdatert
            if (indeks == 0) {
                assertThat(periodeEtter.fraTidligereVedtak).isTrue
                // Sammenligner at alt er likt utenom fraTidligereVedtak
                assertThat(periodeEtter.copy(fraTidligereVedtak = true)).isEqualTo(periodeFør.copy(fraTidligereVedtak = true))
            } else {
                // Ukene er endret i kjøreliste-steget og skal derfor reberegnes, ikke gjenbrukes fra forrige vedtak
                assertThat(periodeEtter.fraTidligereVedtak).isFalse()
                assertThat(periodeEtter.grunnlag.dager).hasSize(3)
                assertThat(periodeEtter.stønadsbeløp).isLessThan(periodeFør.stønadsbeløp)
            }
        }
    }

    private fun hentPerioder(behandlingId: BehandlingId) =
        vedtakService
            .hentVedtak<InnvilgelseDagligReise>(behandlingId)
            .data.beregningsresultat.privatBil!!
            .reiser
            .single()
            .perioder
            .associateBy { it.fom }

    /**
     * Beholder de [behold] første dagene i uken som godkjente (JA), og markerer resten som ikke godkjent (NEI)
     * med en begrunnelse, slik AvklartKjørelisteValidering krever når en dag med `harKjørt=true` reduseres.
     */
    private fun reduserAntallDagerIUken(
        uke: UkeVurderingDto,
        behold: Int,
    ): List<EndreAvklartDagRequest> =
        uke.dager
            .filter { !it.erDagSlettet }
            .sortedBy { it.dato }
            .mapIndexed { indeks, dag ->
                EndreAvklartDagRequest(
                    dato = dag.dato,
                    godkjentGjennomførtKjøring =
                        if (indeks < behold) {
                            GodkjentGjennomførtKjøring.JA
                        } else {
                            GodkjentGjennomførtKjøring.NEI
                        },
                    parkeringsutgift = dag.kjørelisteDag?.parkeringsutgift,
                    begrunnelse = if (indeks >= behold) "Redusert antall dager i kjøreliste-steget" else null,
                )
            }

    private fun virkedager(
        fom: LocalDate,
        tom: LocalDate,
    ): List<LocalDate> =
        generateSequence(fom) { it.plusDays(1) }
            .takeWhile { !it.isAfter(tom) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .toList()
}
