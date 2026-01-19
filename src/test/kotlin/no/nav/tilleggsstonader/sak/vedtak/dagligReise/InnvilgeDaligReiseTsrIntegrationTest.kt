package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.familie.prosessering.domene.Status
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurdering
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.journalpostDagligReiseTsr
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreDagligReiseDto
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTask
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.lagreDagligReiseDto
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.util.lagreVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetDagligReiseTsrDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InnvilgeDaligReiseTsrIntegrationTest : CleanDatabaseIntegrationTest() {
    val fom = 1 september 2025
    val tom = 30 september 2025

    @Autowired
    lateinit var ytelseClient: YtelseClient

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var utbetalingStatusHåndterer: UtbetalingStatusHåndterer

    @Test
    fun `innvilge daglig reise med gyldig tiltak, verifiser type andel og brukers nav kontor sendt til økonomi`() {
        // For at det skal opprettes sak med stønadstype DAGLIG_REISE_TSR
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                fraJournalpost =
                    journalpost(
                        journalpostId = "1",
                        journalstatus = Journalstatus.MOTTATT,
                        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
                        bruker = Bruker("12345678910", BrukerIdType.FNR),
                        tema = Tema.TSR.name,
                    ),
                medAktivitet = { behandlingId ->
                    lagreVilkårperiodeAktivitet(
                        behandlingId = behandlingId,
                        aktivitetType = AktivitetType.TILTAK,
                        typeAktivitet = TypeAktivitet.ENKELAMO,
                        fom = fom,
                        tom = tom,
                        faktaOgSvar =
                            FaktaOgSvarAktivitetDagligReiseTsrDto(
                                svarHarUtgifter = SvarJaNei.JA,
                            ),
                    )
                },
                medMålgruppe = { behandlingId ->
                    lagreVilkårperiodeMålgruppe(
                        behandlingId = behandlingId,
                        målgruppeType = MålgruppeType.TILTAKSPENGER,
                        fom = fom,
                        tom = tom,
                    )
                },
                medVilkår = listOf(lagreDagligReiseDto(fom = fom, tom = tom)),
            )

        validerAndeler(behandlingId)
        assertThat(taskService.finnAlleTaskerMedType(InterntVedtakTask.TYPE)).allMatch { it.status == Status.FERDIG }
        validerUtebalingerPåKafka(antallUtbetalinger = 1)
    }

    @Test
    fun `innvilge revurdering daglig reise med gyldig tiltak`() {
        // For at det skal opprettes sak med stønadstype DAGLIG_REISE_TSR
        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        // FØRSTEGANGSBEHANDLING
        val førstegangsbehandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(fraJournalpost = journalpostDagligReiseTsr) {
                defaultDagligReiseTsrTestdata()
            }

        validerAndeler(førstegangsbehandlingId)
        assertThat(taskService.finnAlleTaskerMedType(InterntVedtakTask.TYPE)).allMatch { it.status == Status.FERDIG }
        validerUtebalingerPåKafka(antallUtbetalinger = 1)

        sendOkPåAndelerFraØkonomi(førstegangsbehandlingId)

        // REVURDERING
        val førstegangsbehandling = behandlingService.hentBehandling(førstegangsbehandlingId)

        val revurderingId =
            opprettRevurdering(
                opprettBehandlingDto =
                    OpprettBehandlingDto(
                        fagsakId = førstegangsbehandling.fagsakId,
                        årsak = BehandlingÅrsak.SØKNAD,
                        nyeOpplysningerMetadata = null,
                        kravMottatt = fom,
                    ),
            )

        val vilkårsperioderFørstegangsbehandling = vilkårperiodeService.hentVilkårperioder(førstegangsbehandlingId)
        val vilkårsperioderRevurdering = vilkårperiodeService.hentVilkårperioder(revurderingId)
        // TODO valider vilkår
        validerErInngangsvilkårErLike(
            vilkårsperioderFørstegangsbehandling = vilkårsperioderFørstegangsbehandling,
            vilkårsperioderRevurdering = vilkårsperioderRevurdering,
        )

        gjennomførBehandlingsløp(revurderingId) {
            aktivitet {
                oppdater { aktiviteter, behandlingId ->
                    with(aktiviteter.single()) {
                        id to this.tilLagreVilkårperiodeAktivitet(behandlingId).copy(tom = 28 februar 2026)
                    }
                }
            }
            målgruppe {
                oppdater { målgrupper, behandlingId ->
                    with(målgrupper.single()) {
                        id to this.tilLagreVilkårperiodeMålgruppe(behandlingId).copy(tom = 28 februar 2026)
                    }
                }
            }
            vilkår {
                oppdaterDagligReise { vilkår ->
                    with(vilkår.single()) {
                        id to this.tilLagreDagligReiseDto().copy(tom = 28 februar 2026)
                    }
                }
            }
        }

        validerAndeler(revurderingId)
        assertThat(taskService.finnAlleTaskerMedType(InterntVedtakTask.TYPE)).allMatch { it.status == Status.FERDIG }
        validerUtebalingerPåKafka(antallUtbetalinger = 2)
    }

    private fun sendOkPåAndelerFraØkonomi(behandlingId: BehandlingId) {
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId = behandlingId.toString(),
            melding =
                UtbetalingStatusRecord(
                    status = UtbetalingStatus.OK,
                    detaljer = null,
                    error = null,
                ),
            utbetalingGjelderFagsystem = UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )
    }

    private fun validerAndeler(behandlingId: BehandlingId) {
        val andeler = tilkjentYtelseRepository.findByBehandlingId(behandlingId)!!.andelerTilkjentYtelse

        assertThat(andeler).allMatch { it.type == TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO }
        assertThat(andeler).allMatch { it.brukersNavKontor != null }
    }

    private fun validerUtebalingerPåKafka(antallUtbetalinger: Int) {
        val utbetalingRecord =
            KafkaTestConfig
                .sendteMeldinger()
                .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, antallUtbetalinger)
                .map { it.verdiEllerFeil<IverksettingDto>() }

        // Betalende enhet skal alltid være satt for TSR/tiltaksenheten
        assertThat(
            utbetalingRecord.flatMap { record ->
                record.utbetalinger.flatMap { it.perioder }
            },
        ).allMatch { it.betalendeEnhet != null }
    }

    private fun validerErInngangsvilkårErLike(
        vilkårsperioderFørstegangsbehandling: Vilkårperioder,
        vilkårsperioderRevurdering: Vilkårperioder,
    ) {
        val målgruppeFørstegangsbehandling = vilkårsperioderFørstegangsbehandling.målgrupper.single()
        val målgruppeRevurdering = vilkårsperioderRevurdering.målgrupper.single()

        assertThat(målgruppeFørstegangsbehandling.type).isEqualTo(målgruppeRevurdering.type)
        assertThat(målgruppeFørstegangsbehandling.typeAktivitet).isEqualTo(målgruppeRevurdering.typeAktivitet)
        assertThat(målgruppeFørstegangsbehandling.fom).isEqualTo(målgruppeRevurdering.fom)
        assertThat(målgruppeFørstegangsbehandling.tom).isEqualTo(målgruppeRevurdering.tom)
        assertThat(målgruppeFørstegangsbehandling.faktaOgVurdering).isEqualTo(målgruppeRevurdering.faktaOgVurdering)
        assertThat(målgruppeFørstegangsbehandling.begrunnelse).isEqualTo(målgruppeRevurdering.begrunnelse)
        assertThat(målgruppeFørstegangsbehandling.resultat).isEqualTo(målgruppeRevurdering.resultat)
        assertThat(målgruppeFørstegangsbehandling.kilde).isEqualTo(målgruppeRevurdering.kilde)

        val aktivitetFørstegangsbehandling = vilkårsperioderFørstegangsbehandling.aktiviteter.single()
        val aktivitetRevurdering = vilkårsperioderRevurdering.aktiviteter.single()

        assertThat(aktivitetFørstegangsbehandling.type).isEqualTo(aktivitetRevurdering.type)
        assertThat(aktivitetFørstegangsbehandling.typeAktivitet).isEqualTo(aktivitetRevurdering.typeAktivitet)
        assertThat(aktivitetFørstegangsbehandling.fom).isEqualTo(aktivitetRevurdering.fom)
        assertThat(aktivitetFørstegangsbehandling.tom).isEqualTo(aktivitetRevurdering.tom)
        assertThat(aktivitetFørstegangsbehandling.faktaOgVurdering).isEqualTo(aktivitetRevurdering.faktaOgVurdering)
        assertThat(aktivitetFørstegangsbehandling.begrunnelse).isEqualTo(aktivitetRevurdering.begrunnelse)
        assertThat(aktivitetFørstegangsbehandling.resultat).isEqualTo(aktivitetRevurdering.resultat)
        assertThat(aktivitetFørstegangsbehandling.kilde).isEqualTo(aktivitetRevurdering.kilde)
    }
}
