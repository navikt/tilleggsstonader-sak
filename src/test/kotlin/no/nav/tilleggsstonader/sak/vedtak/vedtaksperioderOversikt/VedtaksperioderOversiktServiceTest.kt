package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPerson
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VedtaksperioderOversiktServiceTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var vedtaksperioderOversiktService: VedtaksperioderOversiktService

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Test
    fun `skal returnere vedtaksperiodeoversikt for alle stønadstyper`() {
        val fagsakPerson = testoppsettService.opprettPerson("123")

        opprettBehandlingOgVedtakTilsynBarn(fagsakPerson)
        opprettBehandlingOgVedtakLæremidler(fagsakPerson)
        opprettBehandlingOgVedtakBoutgifter(fagsakPerson)
        opprettBehandlingOgVedtakDagligReiseTso(fagsakPerson)
        opprettBehandlingOgVedtakDagligReiseTsr(fagsakPerson)

        val res = vedtaksperioderOversiktService.hentVedtaksperioderOversikt(fagsakPersonId = fagsakPerson.id)

        assertThat(res.tilsynBarn).isNotEmpty()
        assertThat(res.læremidler).isNotEmpty()
        assertThat(res.boutgifter).isNotEmpty()
        assertThat(res.dagligReiseTso).isNotEmpty()
        assertThat(res.dagligReiseTsr).isNotEmpty()
    }

    @Test
    fun `skal vise vedtaksperioder fra annen enhet`() {
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val fomTiltaksenheten = 1 september 2025
        val tomTiltaksenheten = 30 september 2025

        // Gjennomfører behandling for Tiltaksenheten
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        ) {
            defaultDagligReiseTsrTestdata(fomTiltaksenheten, tomTiltaksenheten)
        }

        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()

        // Gjennomfører behandling for Nay
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.INNGANGSVILKÅR,
            ) {}

        val forventetDetaljertVedtaksperiodeTsr =
            listOf(
                DetaljertVedtaksperiodeDagligReise(
                    fom = fomTiltaksenheten,
                    tom = tomTiltaksenheten,
                    aktivitet = AktivitetType.TILTAK,
                    typeAktivtet = TypeAktivitet.GRUPPEAMO,
                    målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
                    typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                    stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                ),
            )

        assertThat(vedtaksperioderOversiktService.hentDetaljerteVedtaksperioderForBehandling(behandlingId)).isEqualTo(
            forventetDetaljertVedtaksperiodeTsr,
        )
    }

    private fun opprettBehandlingOgVedtakTilsynBarn(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.BARNETILSYN,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(TilsynBarnTestUtil.innvilgetVedtak(behandlingId = behandling.id))
    }

    private fun opprettBehandlingOgVedtakLæremidler(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.LÆREMIDLER,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(LæremidlerTestUtil.innvilgelse(behandlingId = behandling.id))
    }

    private fun opprettBehandlingOgVedtakBoutgifter(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.BOUTGIFTER,
                identer = fagsakPerson.identer,
            )

        val beregningsresultat =
            BoutgifterTestUtil.lagBeregningsresultatMåned(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                utgifter =
                    mapOf(
                        TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                            listOf(
                                lagUtgiftBeregningBoutgifter(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                    utgift = 1000,
                                ),
                            ),
                    ),
            )

        val vedtak =
            BoutgifterTestUtil.innvilgelseBoutgifter(
                behandlingId = behandling.id,
                vedtaksperioder = emptyList(),
                beregningsresultat = BeregningsresultatBoutgifter(listOf(beregningsresultat)),
            )

        vedtakRepository.insert(vedtak)
    }

    private fun opprettBehandlingOgVedtakDagligReiseTso(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))
    }

    private fun opprettBehandlingOgVedtakDagligReiseTsr(fagsakPerson: FagsakPerson) {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET),
                stønadstype = Stønadstype.DAGLIG_REISE_TSR,
                identer = fagsakPerson.identer,
            )

        vedtakRepository.insert(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))
    }
}
