package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.BehandlingContext
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.testdata.tilLagreDagligReiseDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.IverksettingDto
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class RevurderingPrivatBilIntegrationTest(
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val tilkjentYtelseService: TilkjentYtelseService,
    @Autowired private val avklartKjørelisteService: AvklartKjørelisteService,
) : CleanDatabaseIntegrationTest() {
    val fom = 1 januar 2026
    val tom = 31 desember 2026

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_OPPHØRE_PRIVAT_BIL) } returns true
        every { unleashService.isEnabled(Toggle.KAN_REVURDERE_PRIVAT_BIL) } returns true
    }

    @Nested
    inner class LeggeTilOgSlette {
        @Test
        fun `skal kunne legge til en ny reise i en revurdering`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext = innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1)

            // Revurder og legg til en ny reise
            val fomReise2 = 12 januar 2026
            val tomReise2 = 18 januar 2026

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        opprett {
                            privatBil(fomReise2, tomReise2)
                        }
                    }
                }

            val vedtak = hentInnvilgelse(revurderingId)
            val rammevedtak = vedtak.rammevedtakPrivatBil!!
            val beregningPrivatBil = vedtak.beregningsresultat.privatBil!!

            val reiserIRammevedtak = rammevedtak.reiser.sortedBy { it.grunnlag.fom }

            // a. Rammevedtak: to reiser
            assertThat(reiserIRammevedtak).extracting({ it.grunnlag.fom }, { it.grunnlag.tom }).containsExactly(
                Tuple.tuple(fomReise1, tomReise1),
                Tuple.tuple(fomReise2, tomReise2),
            )

            val reiseId1 = reiserIRammevedtak[0].reiseId
            val reiseId2 = reiserIRammevedtak[1].reiseId

            // b. Avklarte uker i revurderingen finnes kun for eksisterende reise
            val avklarteUkerRevurdering = avklartKjørelisteService.hentAvklarteUkerForBehandling(revurderingId)
            assertThat(avklarteUkerRevurdering.map { it.reiseId }.distinct()).containsOnly(reiseId1)

            // c. Beregningsresultat: perioder for reise1, tomt for ny reise2
            assertThat(beregningPrivatBil.reiser.single { it.reiseId == reiseId1 }.perioder).hasSize(1)
            assertThat(beregningPrivatBil.reiser.single { it.reiseId == reiseId2 }.perioder).isEmpty()

            // d. Ingen nye andeler opprettes for den nye reisen
            val andelerFraKjørelistebehandling =
                andelerFor(kjørelisteBehandling(førstegangsbehandlingContext.fagsakId).id)
            val andelerFraRevurdering = andelerFor(revurderingId)
            assertThat(andelerFraRevurdering).hasSameSizeAs(andelerFraKjørelistebehandling)
        }

        @Test
        fun `to reiser med innsendte kjøredager - sletting av én reise`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026
            val fomReise2 = 12 januar 2026
            val tomReise2 = 18 januar 2026

            val kontekst =
                innvilgetPrivatBilBehandlingMedToReiserOgKjørelister(fomReise1, tomReise1, fomReise2, tomReise2)

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = kontekst.behandlingContext.behandlingId,
                ) {
                    vilkår {
                        slettDagligReise { vilkår ->
                            vilkår.single { it.reiseId == kontekst.reiseId2 }.id to SlettVilkårRequestDto(kommentar = "Slett")
                        }
                    }
                }

            val vedtak = hentInnvilgelse(revurderingId)
            val rammevedtak = vedtak.rammevedtakPrivatBil!!
            val beregningPrivatBil = vedtak.beregningsresultat.privatBil!!

            // Rammevedtak: kun reise1
            assertThat(rammevedtak.reiser)
                .extracting({ it.grunnlag.fom }, { it.grunnlag.tom })
                .containsExactly(Tuple.tuple(fomReise1, tomReise1))

            // Avklarte uker for slettet reise er markert SLETTET
            val avklarteUkerRevurdering = avklartKjørelisteService.hentAvklarteUkerForBehandling(revurderingId)
            val ukerForReise2 = avklarteUkerRevurdering.filter { it.reiseId == kontekst.reiseId2 }
            assertThat(ukerForReise2).isNotEmpty()
            assertThat(ukerForReise2).allMatch { it.avklartKjørtUkeStatus == AvklartKjørtUkeStatus.SLETTET }

            // Avklarte uker for reise1 er ikke slettet
            val ukerForReise1 =
                avklartKjørelisteService
                    .hentAvklarteUkerForBehandling(revurderingId)
                    .filter { it.reiseId == kontekst.reiseId1 }
            assertThat(ukerForReise1).allMatch { it.avklartKjørtUkeStatus == AvklartKjørtUkeStatus.UENDRET }

            // Beregningsresultat: kun for reise1
            assertThat(beregningPrivatBil.reiser.map { it.reiseId }).containsOnly(kontekst.reiseId1)

            // Andeler fra revurdering er færre enn tidligere fordi en reise er fjernet
            val andelerFraKjørelistebehandling = andelerFor(sisteKjørelisteBehandling(kontekst.fagsakId).id).size
            val andelerFraRevurdering = andelerFor(revurderingId)
            assertThat(andelerFraRevurdering.size).isLessThan(andelerFraKjørelistebehandling)

            assertThat(andelerFraRevurdering.map { it.reiseId }.distinct()).containsOnly(kontekst.reiseId1)
        }
    }

    @Nested
    inner class EndringAvFakta {
        @Test
        fun `endring av bompenger per dag skal gi høyere dagsats i rammevedtaket`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext = innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1)
            val reiseId = førstegangsbehandlingContext.reiseId

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        endrePrivatBilFaktaDelperioder { it.copy(bompengerPerDag = 20.toBigDecimal()) }
                    }
                }

            val dagsatsUtenBompenger = hentInnvilgelse(førstegangsbehandlingContext.behandlingId).hentUtDagsats(reiseId)

            val dagsatsMedBompenger = hentInnvilgelse(revurderingId).hentUtDagsats(reiseId)

            // dagsatsUtenParkering inkluderer bompenger — skal være høyere etter endringen
            assertThat(dagsatsMedBompenger).isGreaterThan(dagsatsUtenBompenger)
            assertThat(dagsatsMedBompenger).isEqualTo(BigDecimal("78.80"))
        }

        @Test
        fun `endring av fergekostnader per dag skal gi høyere dagsats i rammevedtaket`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext = innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1)
            val reiseId = førstegangsbehandlingContext.reiseId

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        endrePrivatBilFaktaDelperioder { it.copy(fergekostnadPerDag = 20.toBigDecimal()) }
                    }
                }

            val dagsatsUtenFergekostnader = hentInnvilgelse(førstegangsbehandlingContext.behandlingId).hentUtDagsats(reiseId)

            val dagsatsMedFergekostnader = hentInnvilgelse(revurderingId).hentUtDagsats(reiseId)

            // dagsatsUtenParkering inkluderer fergekostnader — skal være høyere etter endringen
            assertThat(dagsatsMedFergekostnader).isGreaterThan(dagsatsUtenFergekostnader)
            assertThat(dagsatsMedFergekostnader).isEqualTo(BigDecimal("78.80"))
        }

        @Test
        fun `økning av reiseavstand fører til reberegning av beregningsresultatet`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext = innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1)
            val reiseId = førstegangsbehandlingContext.reiseId

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        endrePrivatBilFakta { copy(reiseavstandEnVei = "67".toBigDecimal()) }
                    }
                }

            val kjørelistebehandling = kjørelisteBehandling(førstegangsbehandlingContext.fagsakId)
            val dagsatsKjørelistebehandling = hentInnvilgelse(kjørelistebehandling.id).hentUtDagsats(reiseId)
            val dagsatsRevurdering = hentInnvilgelse(revurderingId).hentUtDagsats(reiseId)

            assertThat(dagsatsKjørelistebehandling).isLessThan(dagsatsRevurdering)

            val andelerKjørelistebehandling = andelerFor(kjørelistebehandling.id)
            val andelerRevurdering = andelerFor(revurderingId)

            assertThat(andelerKjørelistebehandling.sumOf { it.beløp }).isLessThan(andelerRevurdering.sumOf { it.beløp })
        }

        @Test
        fun `øking av reisedager per uke endrer kapasiteten i rammevedtaket men ikke historiske kjøredager`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext =
                innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1, reisedagerPerUke = 2)

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        endrePrivatBilFaktaDelperioder { it.copy(reisedagerPerUke = 5) }
                    }
                }

            val vedtakEtterRevurdering = hentInnvilgelse(revurderingId)
            val reise = vedtakEtterRevurdering.rammevedtakPrivatBil!!.reiser.single()

            // Rammevedtaket har ny kapasitet
            assertThat(
                reise.grunnlag.delperioder
                    .single()
                    .reisedagerPerUke,
            ).isEqualTo(5)

            // Historiske kjøredager er uendret (1 kjørt dag)
            val perioder =
                vedtakEtterRevurdering.beregningsresultat.privatBil!!
                    .reiser
                    .single()
                    .perioder
            assertThat(perioder).hasSize(1)
            assertThat(perioder.single().grunnlag.dager).hasSize(1)
        }

        // ─── Redusere reisedager (validering ikke implementert) ────────

        @Disabled("Validering for reduksjon av reisedager er ikke implementert enda — se TODO")
        @Test
        fun `reduksjon av reisedager per uke under antallet allerede kjørte dager kaster feil`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026

            val førstegangsbehandlingContext =
                innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1, reisedagerPerUke = 5)

            assertThrows<Exception> {
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        endrePrivatBilFaktaDelperioder { it.copy(reisedagerPerUke = 2) }
                    }
                }
            }
        }
    }

    @Nested
    inner class EndringAvFomOgTom {
        @Test
        fun `skal reberegne hele reisen dersom tom utvides`() {
            val fomReise1 = 5 januar 2026
            val tomReise1 = 11 januar 2026
            val nyTom = 15 januar 2026

            val førstegangsbehandlingContext = innvilgetPrivatBilBehandlingMedKjøreliste(fomReise1, tomReise1)

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(
                    fraBehandlingId = førstegangsbehandlingContext.behandlingId,
                ) {
                    vilkår {
                        oppdaterDatoPåEnesteDagligeReise(fom = fomReise1, tom = nyTom)
                    }
                }

            val vedtakFørRevurdering = hentInnvilgelse(førstegangsbehandlingContext.behandlingId)
            val vedtakEtterKjøreliste = hentInnvilgelse(kjørelisteBehandling(førstegangsbehandlingContext.fagsakId).id)
            val vedtakEtterRevurdering = hentInnvilgelse(revurderingId)

            // Rammevedtak og beregning finnes
            assertThat(vedtakEtterRevurdering.rammevedtakPrivatBil).isNotNull()
            assertThat(vedtakEtterRevurdering.rammevedtakPrivatBil!!.sisteTom()).isEqualTo(nyTom)
            assertThat(vedtakEtterRevurdering.beregningsresultat.privatBil).isNotNull()
            assertThat(vedtakEtterRevurdering.beregningsresultat.privatBil)
                .isNotEqualTo(vedtakFørRevurdering.beregningsresultat.privatBil)
                .isEqualTo(vedtakEtterKjøreliste.beregningsresultat.privatBil)

            // Skal finnes to utbetalinger. En for kjøreliste og en for revurdering
            val iverksettinger =
                KafkaFake
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                    .map { it.verdiEllerFeil<IverksettingDto>() }

            val eksternIdRevurdering = testoppsettService.hentSaksbehandling(revurderingId).eksternId
            val utbetalingKjørelistebehandling = iverksettinger.single { it.behandlingId.toLong() != eksternIdRevurdering }
            val utbetalingRevurdering = iverksettinger.single { it.behandlingId.toLong() == eksternIdRevurdering }

            // Utvidelse av rammevedtaket skal ikke endre utbetalinger
            assertThat(utbetalingKjørelistebehandling.utbetalinger).isEqualTo(utbetalingRevurdering.utbetalinger)
        }

        @Test
        fun `skal få tomt beregningsresultat om innsendte uker ikke lenger er innenfor rammevedtak`() {
            every { unleashService.isEnabled(Toggle.KAN_AUTOMATISK_BEHANDLE_KJØRELISTE) } returns true

            val fomKjøreliste = 5 januar 2026
            val tomKjøreliste = 11 januar 2026
            val nyFom = 12 januar 2026

            val førstegangsbehandlingContext =
                opprettBehandlingOgGjennomførBehandlingsløp(stønadstype = Stønadstype.DAGLIG_REISE_TSO) {
                    defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                    sendInnKjøreliste {
                        periode = Datoperiode(fomKjøreliste, tomKjøreliste)
                        kjørteDager = listOf(KjørtDag(dato = fomKjøreliste))
                    }
                }

            val kjørelistebehandling = kjørelisteBehandling(førstegangsbehandlingContext.fagsakId)
            testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)

            val revurderingId =
                opprettRevurderingOgGjennomførBehandlingsløp(kjørelistebehandling.id) {
                    vilkår {
                        oppdaterDatoPåEnesteDagligeReise(nyFom, tom)
                    }
                }

            val revurdering = testoppsettService.hentBehandling(revurderingId)

            val vedtakFørstegangsbehandling = hentInnvilgelse(førstegangsbehandlingContext.behandlingId)
            val vedtakKjørelistebehandling = hentInnvilgelse(kjørelistebehandling.id)
            val vedtakRevurdering = hentInnvilgelse(revurdering.id)

            assertThat(vedtakFørstegangsbehandling.rammevedtakPrivatBil).isEqualTo(vedtakKjørelistebehandling.rammevedtakPrivatBil)
            assertThat(vedtakFørstegangsbehandling.rammevedtakPrivatBil).isNotEqualTo(vedtakRevurdering.rammevedtakPrivatBil)

            assertThat(vedtakFørstegangsbehandling.beregningsresultat).isNotEqualTo(vedtakKjørelistebehandling.beregningsresultat)

            // Beregningsresultatene skal vise at det ikke skal utbetales noe.
            // Finnes ikke et beregningsresultat på førstegangsbehandling
            // TODO - Finnes beregningsresultat på revurdering, da det har kopiert fra kjørelistebehandling, men det er tomt
            assertThat(vedtakFørstegangsbehandling.beregningsresultat.privatBil).isNull()
            assertThat(
                vedtakRevurdering.beregningsresultat.privatBil
                    ?.reiser
                    ?.single()
                    ?.perioder,
            ).isNotNull.isEmpty()

            // Forventer to meldinger: én fra kjørelistebehandling og én fra revurdering
            val iverksettinger =
                KafkaFake
                    .sendteMeldinger()
                    .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 2)
                    .map { it.verdiEllerFeil<IverksettingDto>() }

            // Utbetaling i første iverksetting (kjørelistebehandling)
            assertThat(
                iverksettinger
                    .minBy { it.vedtakstidspunkt }
                    .utbetalinger
                    .single()
                    .perioder,
            ).isNotEmpty
            // Tom utbetaling som nuller ut kjørelistebehandling i revurdering
            assertThat(
                iverksettinger
                    .maxBy { it.vedtakstidspunkt }
                    .utbetalinger
                    .single()
                    .perioder,
            ).isEmpty()
        }

        // Endre fom frem og tilbake
        // Endre tom frem og tilbake
        // Flytte hele perioden vekk fra der den var
        // Alle caser må testes med og uten innsendte kjørelister
    }

    @Test
    fun `reise som ikke berøres av tidligste endring beholdes uendret når kun fremtidig reise endres`() {
        val fomReise1 = 5 januar 2026
        val tomReise1 = 11 januar 2026
        val fomReise2 = 26 januar 2026
        val tomReise2 = 1 februar 2026

        val førstegangsbehandlingContext =
            innvilgetPrivatBilBehandlingMedToReiserOgKjørelister(
                fomReise1 = fomReise1,
                tomReise1 = tomReise1,
                fomReise2 = fomReise2,
                tomReise2 = tomReise2,
            )

        val revurderingId2 =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingContext.behandlingId,
            ) {
                vilkår {
                    oppdaterDagligReise { perioder, _ ->
                        val reise2 = perioder.single { it.reiseId == førstegangsbehandlingContext.reiseId2 }
                        reise2.id to
                            reise2.tilLagreDagligReiseDto().copy(
                                fakta =
                                    (reise2.fakta as FaktaDagligReisePrivatBilDto).copy(
                                        faktaDelperioder =
                                            reise2.fakta.faktaDelperioder.map {
                                                it.copy(bompengerPerDag = 20.toBigDecimal())
                                            },
                                    ),
                            )
                    }
                }
            }

        val vedtakEtterRevurdering = hentInnvilgelse(revurderingId2)

        val dagsatsEtterBompengerEndring =
            vedtakEtterRevurdering.hentUtDagsats(reiseId = førstegangsbehandlingContext.reiseId2)
        assertThat(dagsatsEtterBompengerEndring).isEqualTo(BigDecimal("78.80"))

        val beregningPrivatBil = vedtakEtterRevurdering.beregningsresultat.privatBil!!

        // Beregningsresultat for reise1 er hentet fra forrige vedtak (fraTidligereVedtak = true)
        val perioderReise1 =
            beregningPrivatBil.reiser.single { it.reiseId == førstegangsbehandlingContext.reiseId1 }.perioder
        assertThat(perioderReise1).isNotEmpty()
        assertThat(perioderReise1).allMatch { it.fraTidligereVedtak }
    }

    /**
     * Oppretter en innvilget privatbil-behandling med én reise og behandler den tilhørende kjørelisten.
     */
    private fun innvilgetPrivatBilBehandlingMedKjøreliste(
        fomReise: LocalDate,
        tomReise: LocalDate,
        reisedagerPerUke: Int = 5,
    ): PrivatBilKontekst {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBil(fomReise, tomReise, reisedagerPerUke = reisedagerPerUke)
                    }
                }
                sendInnKjøreliste {
                    periode = Datoperiode(fomReise, tomReise)
                    kjørteDager = listOf(KjørtDag(dato = fomReise))
                }
            }

        gjennomførAlleKjørelisterOgSettAndelerOk(behandlingContext.fagsakId)

        return PrivatBilKontekst(behandlingContext, sorterteReiseIder(behandlingContext.behandlingId))
    }

    /**
     * Oppretter en innvilget privatbil-behandling med to reiser og behandler kjørelistene for begge.
     */
    private fun innvilgetPrivatBilBehandlingMedToReiserOgKjørelister(
        fomReise1: LocalDate,
        tomReise1: LocalDate,
        fomReise2: LocalDate,
        tomReise2: LocalDate,
    ): PrivatBilKontekst {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.DAGLIG_REISE_TSO) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom = fom, tom = tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom = fom, tom = tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBil(fomReise1, tomReise1)
                        privatBil(fomReise2, tomReise2)
                    }
                }
                sendInnKjøreliste {
                    periode = Datoperiode(fomReise1, tomReise1)
                    kjørteDager = listOf(KjørtDag(dato = fomReise1))
                    reiseId { it.first().reiseId }
                }
                sendInnKjøreliste {
                    periode = Datoperiode(fomReise2, tomReise2)
                    kjørteDager = listOf(KjørtDag(dato = fomReise2))
                    reiseId { it.last().reiseId }
                }
            }

        gjennomførAlleKjørelisterOgSettAndelerOk(behandlingContext.fagsakId)

        return PrivatBilKontekst(behandlingContext, sorterteReiseIder(behandlingContext.behandlingId))
    }

    /**
     * Kontekst for en innvilget privatbil-behandling. [reiseIder] er sortert på fom.
     */
    private data class PrivatBilKontekst(
        val behandlingContext: BehandlingContext,
        val reiseIder: List<ReiseId>,
    ) {
        val behandlingId get() = behandlingContext.behandlingId
        val fagsakId get() = behandlingContext.fagsakId
        val reiseId get() = reiseIder.single()
        val reiseId1 get() = reiseIder[0]
        val reiseId2 get() = reiseIder[1]
    }

    private fun sorterteReiseIder(behandlingId: BehandlingId): List<ReiseId> =
        hentInnvilgelse(behandlingId)
            .rammevedtakPrivatBil!!
            .reiser
            .sortedBy { it.grunnlag.fom }
            .map { it.reiseId }

    private fun gjennomførAlleKjørelisterOgSettAndelerOk(fagsakId: FagsakId) {
        kjørelisteBehandlinger(fagsakId).forEach { kjørelistebehandling ->
            gjennomførKjørelisteBehandling(kjørelistebehandling)
            testoppsettService.settAndelerTilOkForBehandling(kjørelistebehandling.id)
        }
    }

    private fun kjørelisteBehandlinger(fagsakId: FagsakId) =
        testoppsettService
            .hentBehandlinger(fagsakId)
            .filter { it.erKjørelisteBehandling() }

    private fun kjørelisteBehandling(fagsakId: FagsakId) = kjørelisteBehandlinger(fagsakId).single()

    private fun sisteKjørelisteBehandling(fagsakId: FagsakId) = kjørelisteBehandlinger(fagsakId).maxBy { it.sporbar.opprettetTid }

    private fun hentInnvilgelse(behandlingId: BehandlingId) = vedtakService.hentVedtak<InnvilgelseDagligReise>(behandlingId).data

    private fun andelerFor(behandlingId: BehandlingId) = tilkjentYtelseService.hentForBehandling(behandlingId).andelerTilkjentYtelse

    private fun InnvilgelseDagligReise.hentUtDagsats(reiseId: ReiseId): BigDecimal =
        this.rammevedtakPrivatBil!!
            .reiser
            .single { it.reiseId == reiseId }
            .grunnlag.delperioder
            .single()
            .satser
            .single()
            .dagsatsUtenParkering

    private fun RammevedtakPrivatBil.sisteTom() =
        this.reiser
            .map { it.grunnlag.tom }
            .maxOf { it }
}
