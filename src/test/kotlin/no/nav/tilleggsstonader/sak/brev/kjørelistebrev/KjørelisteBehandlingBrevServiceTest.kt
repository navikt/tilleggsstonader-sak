package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KjørelisteBehandlingBrevServiceTest {
    private val kjørelisteBehandlingBrevRepository = mockk<KjørelisteBehandlingBrevRepository>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val htmlifyClient = mockk<HtmlifyClient>()
    private val personService = mockk<PersonService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingService = mockk<BehandlingService>()

    private val service =
        KjørelisteBehandlingBrevService(
            kjørelisteBehandlingBrevRepository = kjørelisteBehandlingBrevRepository,
            familieDokumentClient = familieDokumentClient,
            htmlifyClient = htmlifyClient,
            personService = personService,
            vedtakService = vedtakService,
            behandlingService = behandlingService,
        )

    private val reiseId = ReiseId.random()
    private val fomUke1 = 5 januar 2026
    private val tomUke1 = 11 januar 2026
    private val fomUke2 = 12 januar 2026
    private val tomUke2 = 18 januar 2026

    private val saksbehandling =
        saksbehandling(
            fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSR),
            status = BehandlingStatus.OPPRETTET,
            behandlingMetode = BehandlingMetode.AUTOMATISK,
        )

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
        every { behandlingService.hentSaksbehandling(saksbehandling.id) } returns saksbehandling
        every { personService.hentVisningsnavnForPerson(any()) } returns "Test Person"
        every { familieDokumentClient.genererPdf(any()) } returns ByteArray(0)
        every { kjørelisteBehandlingBrevRepository.findByBehandlingId(saksbehandling.id) } returns null
        every { kjørelisteBehandlingBrevRepository.insert(any()) } answers { firstArg() }
    }

    @Test
    fun `filtrer bort perioder fra tidligere vedtak i brev`() {
        val periodeFraTidligereVedtak = lagPeriode(fomUke1, tomUke1, fraTidligereVedtak = true)
        val nyPeriode = lagPeriode(fomUke2, tomUke2, fraTidligereVedtak = false)

        val slot = slot<KjørelisteBehandlingBrevRequest>()
        every { htmlifyClient.genererKjørelisteBehandlingBrev(capture(slot)) } returns "<html/>"

        mockVedtak(listOf(periodeFraTidligereVedtak, nyPeriode))
        service.hentEllerGenererBrev(saksbehandling.id)

        val perioder =
            slot.captured.beregning.reiser
                .single()
                .perioder
        assertThat(perioder).hasSize(1)
        assertThat(perioder.single().fraTidligereVedtak).isFalse()
        assertThat(perioder.single().fom).isEqualTo(fomUke2)
    }

    @Test
    fun `ta med alle perioder når ingen er fra tidligere vedtak`() {
        val periode1 = lagPeriode(fomUke1, tomUke1, fraTidligereVedtak = false)
        val periode2 = lagPeriode(fomUke2, tomUke2, fraTidligereVedtak = false)

        val slot = slot<KjørelisteBehandlingBrevRequest>()
        every { htmlifyClient.genererKjørelisteBehandlingBrev(capture(slot)) } returns "<html/>"

        mockVedtak(listOf(periode1, periode2))
        service.hentEllerGenererBrev(saksbehandling.id)

        val perioder =
            slot.captured.beregning.reiser
                .single()
                .perioder
        assertThat(perioder).hasSize(2)
        assertThat(perioder.none { it.fraTidligereVedtak }).isTrue()
    }

    @Test
    fun `reise med kun perioder fra tidligere vedtak filtreres bort fra brev`() {
        val periode1 = lagPeriode(fomUke1, tomUke1, fraTidligereVedtak = true)
        val periode2 = lagPeriode(fomUke2, tomUke2, fraTidligereVedtak = true)

        val slot = slot<KjørelisteBehandlingBrevRequest>()
        every { htmlifyClient.genererKjørelisteBehandlingBrev(capture(slot)) } returns "<html/>"

        mockVedtak(listOf(periode1, periode2))
        service.hentEllerGenererBrev(saksbehandling.id)

        assertThat(slot.captured.beregning.reiser).isEmpty()
    }

    @Test
    fun `satser er tomme når alle perioder er fra tidligere vedtak`() {
        val periode1 = lagPeriode(fomUke1, tomUke1, fraTidligereVedtak = true)
        val periode2 = lagPeriode(fomUke2, tomUke2, fraTidligereVedtak = true)

        val slot = slot<KjørelisteBehandlingBrevRequest>()
        every { htmlifyClient.genererKjørelisteBehandlingBrev(capture(slot)) } returns "<html/>"

        mockVedtak(listOf(periode1, periode2))
        service.hentEllerGenererBrev(saksbehandling.id)

        assertThat(slot.captured.satser).isEmpty()
    }

    private fun lagPeriode(
        fom: LocalDate,
        tom: LocalDate,
        fraTidligereVedtak: Boolean,
    ) = BeregningsresultatForReisePrivatBilPeriode(
        fom = fom,
        tom = tom,
        grunnlag =
            BeregningsresultatForReisePrivatBilGrunnlag(
                dager =
                    listOf(
                        BeregningsresultatForReisePrivatBilDag(
                            dato = fom,
                            parkeringskostnad = 0,
                            dagsatsUtenParkering = 100.toBigDecimal(),
                            stønadsbeløpForDag = 100.toBigDecimal(),
                        ),
                    ),
            ),
        stønadsbeløp = 100.toBigDecimal(),
        brukersNavKontor = null,
        fraTidligereVedtak = fraTidligereVedtak,
    )

    private fun mockVedtak(perioder: List<BeregningsresultatForReisePrivatBilPeriode>) {
        val beregningsresultat =
            BeregningsresultatPrivatBil(
                reiser =
                    listOf(
                        BeregningsresultatForReisePrivatBil(
                            reiseId = reiseId,
                            perioder = perioder,
                        ),
                    ),
            )
        val rammevedtak =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = fomUke1,
                tom = tomUke2,
            )
        val vedtak =
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                data =
                    InnvilgelseDagligReise(
                        beregningsresultat =
                            BeregningsresultatDagligReise(
                                offentligTransport = null,
                                privatBil = beregningsresultat,
                            ),
                        rammevedtakPrivatBil = rammevedtak,
                        vedtaksperioder = emptyList(),
                        beregningsplan = Beregningsplan(omfang = Beregningsomfang.ALLE_PERIODER),
                    ),
                gitVersjon = null,
                tidligsteEndring = null,
            )
        every { vedtakService.hentVedtakEllerFeil(saksbehandling.id) } returns vedtak
    }
}
