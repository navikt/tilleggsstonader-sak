package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerEndring
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerKilde
import no.nav.tilleggsstonader.sak.behandling.domain.NyeOpplysningerMetadata
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.henlagtBehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OpprettRevurderingBehandlingServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    lateinit var service: OpprettRevurderingBehandlingService

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        BrukerContextUtil.clearBrukerContext()
        PdlClientConfig.opprettPdlSøker()
    }

    @Nested
    inner class OpprettBehandling {
        @Test
        fun `skal feile hvis forrige behandlingen ikke er ferdigstilt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)

            assertThatThrownBy {
                service.opprettBehandling(opprettRevurdering(fagsakId = behandling.fagsakId))
            }.hasMessage("Det finnes en behandling på fagsaken som hverken er ferdigstilt eller satt på vent")
        }

        @Test
        fun `ny behandling skal peke til forrige innvilgede behandling fordi iverksetting skal peke til forrige iverksatte behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.INNVILGET,
                    ),
                    opprettGrunnlagsdata = false,
                )

            vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.PASS_BARN))

            val nyBehandlingId =
                service.opprettBehandling(opprettRevurdering(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)
        }

        /**
         * ny behandling skal ikke peke til forrige henlagde behandling,
         * fordi iverksetting skal peke til forrige iverksatte behandling.
         */
        @Test
        fun `ny behandling skal ikke peke riktig`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    henlagtBehandling(),
                    opprettGrunnlagsdata = false,
                )

            vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.PASS_BARN))

            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf(PdlClientConfig.BARN_FNR),
                )
            val nyBehandlingId = service.opprettBehandling(opprettRevurdering)

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeIverksatteBehandlingId).isNull()
        }

        /*
         * ny behandling skal ikke peke til forrige avslåtte behandling fordi iverksetting skal peke til forrige
         * iverksatte behandling.
         */
        @Test
        fun `ny behandling skal ikke peke til forrige avslåtte behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.AVSLÅTT,
                    ),
                    opprettGrunnlagsdata = false,
                )

            vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.PASS_BARN))

            val nyBehandlingId =
                service.opprettBehandling(opprettRevurdering(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeIverksatteBehandlingId).isNull()
        }

        @Test
        fun `ny behandling med årsak NYE_OPPLYSNINGER skal kreve kilde og endringer`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    henlagtBehandling(),
                    stønadstype = Stønadstype.LÆREMIDLER,
                    opprettGrunnlagsdata = false,
                )

            assertThatThrownBy {
                service.opprettBehandling(
                    opprettRevurdering(
                        fagsakId = behandling.fagsakId,
                        årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    ).copy(nyeOpplysningerMetadata = null),
                )
            }.hasMessage("Krever metadata ved behandlingsårsak NYE_OPPLYSNINGER")
        }

        @Test
        fun `ny behandling med årsak NYE_OPPLYSNINGER med metadata blir lagret korrekt`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    henlagtBehandling(),
                    stønadstype = Stønadstype.LÆREMIDLER,
                    opprettGrunnlagsdata = false,
                )

            val opprettBehandlingDto =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                )
            val nyBehandlingId = service.opprettBehandling(opprettBehandlingDto)

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.nyeOpplysningerMetadata).isEqualTo(
                NyeOpplysningerMetadata(
                    kilde = opprettBehandlingDto.nyeOpplysningerMetadata!!.kilde,
                    endringer = opprettBehandlingDto.nyeOpplysningerMetadata.endringer,
                    beskrivelse = opprettBehandlingDto.nyeOpplysningerMetadata.beskrivelse,
                ),
            )
        }
    }

    @Nested
    inner class GjenbrukDataFraForrigeBehandling {
        var tidligereBehandling: Behandling? = null
        val barnIdent = PdlClientConfig.BARN_FNR
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        @BeforeEach
        fun setUp() {
            tidligereBehandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.INNVILGET,
                    ),
                    opprettGrunnlagsdata = false,
                )

            val barn =
                barnService.opprettBarn(
                    listOf(
                        behandlingBarn(
                            behandlingId = tidligereBehandling!!.id,
                            personIdent = barnIdent,
                        ),
                    ),
                )

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(behandlingId = tidligereBehandling!!.id, fom = fom, tom = tom),
                    aktivitet(behandlingId = tidligereBehandling!!.id, fom = fom, tom = tom),
                ),
            )

            vilkårRepository.insertAll(
                barn.map {
                    vilkår(
                        behandlingId = tidligereBehandling!!.id,
                        barnId = it.id,
                        type = VilkårType.PASS_BARN,
                    )
                },
            )
        }

        @Test
        fun `skal gjenbruke barn fra forrige behandlingen`() {
            val nyBehandlingId =
                service.opprettBehandling(opprettRevurdering(fagsakId = tidligereBehandling!!.fagsakId))

            with(barnService.finnBarnPåBehandling(tidligereBehandling!!.id)) {
                assertThat(this).hasSize(1)
            }

            with(barnService.finnBarnPåBehandling(nyBehandlingId)) {
                assertThat(this).hasSize(1)
                assertThat(this.single().ident).isEqualTo(barnIdent)
            }
        }

        @Test
        fun `skal gjenbruke informasjon fra forrige behandling`() {
            val revurderingId =
                service.opprettBehandling(opprettRevurdering(fagsakId = tidligereBehandling!!.fagsakId))

            assertThat(vilkårperiodeRepository.findByBehandlingId(revurderingId)).hasSize(2)
            assertThat(vilkårRepository.findByBehandlingId(revurderingId)).hasSize(1)
        }
    }

    @Nested
    inner class HåndteringAvBarn {
        val behandling =
            behandling(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
            )

        val eksisterendeBarn = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.BARN_FNR)

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
            val barn = barnService.opprettBarn(listOf(eksisterendeBarn))
            val vilkår =
                barn.map {
                    vilkår(
                        behandlingId = behandling.id,
                        barnId = it.id,
                        type = VilkårType.PASS_BARN,
                    )
                }
            vilkårRepository.insertAll(vilkår)
        }

        @Test
        fun `hentBarnTilRevurdering - skal markere barn som finnes med på forrige behandling`() {
            val barnTilRevurdering = service.hentBarnTilRevurdering(behandling.fagsakId)

            assertThat(barnTilRevurdering.barn).hasSize(2)
            assertThat(barnTilRevurdering.barn.single { it.ident == eksisterendeBarn.ident }.finnesPåForrigeBehandling)
                .isTrue()

            assertThat(barnTilRevurdering.barn.single { it.ident == PdlClientConfig.BARN2_FNR }.finnesPåForrigeBehandling)
                .isFalse()
        }

        @Test
        fun `skal opprette behandling med nytt barn`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf(PdlClientConfig.BARN2_FNR),
                )
            val behandlingIdRevurdering = service.opprettBehandling(opprettRevurdering)

            with(barnService.finnBarnPåBehandling(behandlingIdRevurdering)) {
                assertThat(this).hasSize(2)
                assertThat(this.map { it.ident })
                    .containsExactlyInAnyOrder(PdlClientConfig.BARN_FNR, PdlClientConfig.BARN2_FNR)
            }
        }

        @Test
        fun `hvis man ikke sender inn noen barn skal man kun beholde barn fra forrige behandling`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf(),
                )
            val behandlingIdRevurdering = service.opprettBehandling(opprettRevurdering)

            with(barnService.finnBarnPåBehandling(behandlingIdRevurdering)) {
                assertThat(this).hasSize(1)
                assertThat(this.map { it.ident }).containsExactlyInAnyOrder(PdlClientConfig.BARN_FNR)
            }
        }

        @Test
        fun `skal feile hvis man sender inn barn på årsak nye opplysninger`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    valgteBarn = setOf(PdlClientConfig.BARN2_FNR),
                )

            assertThatThrownBy {
                service.opprettBehandling(opprettRevurdering)
            }.hasMessage("Kan ikke sende med barn på annet enn årsak Søknad")
        }

        @Test
        fun `skal feile hvis man prøver å sende inn barn som ikke finnes på personen`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf("ukjent ident"),
                )

            assertThatThrownBy {
                service.opprettBehandling(opprettRevurdering)
            }.hasMessage("Kan ikke velge barn som ikke er valgbare.")
        }

        @Test
        fun `skal feile hvis man prøver å sende inn barn som allerede finnes på behandlingen`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf(PdlClientConfig.BARN_FNR),
                )

            assertThatThrownBy {
                service.opprettBehandling(opprettRevurdering)
            }.hasMessage("Kan ikke velge barn som ikke er valgbare.")
        }
    }

    @Nested
    inner class HåndteringAvBarnFørsteBehandlingErHenlagt {
        val henlagtBehandling = henlagtBehandling()

        @Test
        fun `må minumum velge 1 barn i tilfelle første behandling er henlagt`() {
            val opprettRevurdering =
                opprettRevurdering(
                    fagsakId = henlagtBehandling.fagsakId,
                    årsak = BehandlingÅrsak.SØKNAD,
                    valgteBarn = setOf(),
                )

            testoppsettService.opprettBehandlingMedFagsak(henlagtBehandling, opprettGrunnlagsdata = false)

            assertThatThrownBy {
                service.opprettBehandling(opprettRevurdering)
            }.hasMessage(
                "Behandling må opprettes med minimum 1 barn. Dersom alle tidligere behandlinger er henlagt, må ny behandling opprettes som søknad eller papirsøknad.",
            )
        }
    }

    @Nested
    inner class OpprettOppgave {
        @Test
        fun `skal ikke opprette oppgave hvis skalOppretteOppgave=false`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.AVSLÅTT,
                    ),
                    opprettGrunnlagsdata = false,
                )

            vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.PASS_BARN))

            service.opprettBehandling(
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    skalOppretteOppgave = false,
                ),
            )
            assertThat(taskService.finnAlleTaskerMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE))
                .isEmpty()
        }

        @Test
        fun `skal opprette oppgave hvis skalOppretteOppgave=true`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(
                    behandling(
                        status = BehandlingStatus.FERDIGSTILT,
                        resultat = BehandlingResultat.AVSLÅTT,
                    ),
                    opprettGrunnlagsdata = false,
                )

            vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.PASS_BARN))

            service.opprettBehandling(
                opprettRevurdering(
                    fagsakId = behandling.fagsakId,
                    skalOppretteOppgave = true,
                ),
            )
            assertThat(taskService.finnAlleTaskerMedType(OpprettOppgaveForOpprettetBehandlingTask.TYPE))
                .hasSize(1)
        }
    }

    private fun opprettRevurdering(
        fagsakId: FagsakId,
        årsak: BehandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        valgteBarn: Set<String> = emptySet(),
        skalOppretteOppgave: Boolean = true,
    ) = OpprettRevurdering(
        fagsakId = fagsakId,
        årsak = årsak,
        valgteBarn = valgteBarn,
        kravMottatt = null,
        nyeOpplysningerMetadata = if (årsak == BehandlingÅrsak.NYE_OPPLYSNINGER) opprettNyeOpplysningerMetadata() else null,
        skalOppretteOppgave = skalOppretteOppgave,
    )

    private fun opprettNyeOpplysningerMetadata() =
        NyeOpplysningerMetadata(
            kilde = NyeOpplysningerKilde.ETTERSENDING,
            endringer = listOf(NyeOpplysningerEndring.AKTIVITET, NyeOpplysningerEndring.MÅLGRUPPE),
            beskrivelse = "Tittei",
        )
}
