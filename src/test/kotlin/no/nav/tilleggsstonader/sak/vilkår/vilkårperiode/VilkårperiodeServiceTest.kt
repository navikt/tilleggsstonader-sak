package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.RegisterAktivitetClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.ArenaKontraktUtil.aktivitetArenaDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.lønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.opprettVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.DekketAvAnnetRegelverkVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MedlemskapVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.EnsligForsørgerStønadstype.OVERGANGSSTØNAD
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.EnsligForsørgerStønadstype.SKOLEPENGER
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagTestUtil.periodeGrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype as EnsligForsørgerStønadstypeKontrakter

class VilkårperiodeServiceTest : IntegrationTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var stønadsperiodeService: StønadsperiodeService

    @Autowired
    lateinit var vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository

    @Autowired
    lateinit var registerAktivitetClient: RegisterAktivitetClient

    @Autowired
    lateinit var ytelseClient: YtelseClient

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(registerAktivitetClient)
    }

    @Nested
    inner class OpprettVilkårperiode {

        @Test
        fun `skal opprette periode for målgruppe manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeMålgruppe(
                medlemskap = VurderingDto(SvarJaNei.NEI),
                begrunnelse = "begrunnelse målgruppe",
                behandlingId = behandling.id,
            )

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            assertThat(vilkårperiode.type).isEqualTo(opprettVilkårperiode.type)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(opprettVilkårperiode.fom)
            assertThat(vilkårperiode.tom).isEqualTo(opprettVilkårperiode.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo("begrunnelse målgruppe")

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(vilkårperiode.faktaOgVurdering.vurderinger).isInstanceOf(MedlemskapVurdering::class.java)
            assertThat(vilkårperiode.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            assertThat(vilkårperiode.faktaOgVurdering.vurderinger).isNotInstanceOf(DekketAvAnnetRegelverkVurdering::class.java)
        }

        @Test
        fun `skal opprette periode for aktivitet manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeAktivitet(
                lønnet = VurderingDto(SvarJaNei.NEI),
                begrunnelse = "begrunnelse aktivitet",
                behandlingId = behandling.id,
            )

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            assertThat(vilkårperiode.type).isEqualTo(opprettVilkårperiode.type)
            assertThat(vilkårperiode.kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(vilkårperiode.fom).isEqualTo(opprettVilkårperiode.fom)
            assertThat(vilkårperiode.tom).isEqualTo(opprettVilkårperiode.tom)
            assertThat(vilkårperiode.begrunnelse).isEqualTo("begrunnelse aktivitet")

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(vilkårperiode.lønnet.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(vilkårperiode.lønnet.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal kaste feil hvis kildeId ikke finnes blant aktivitetIder i grunnlag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val hentetInformasjon = HentetInformasjon(fom = now(), tom = now(), LocalDateTime.now())
            val aktivitet = GrunnlagAktivitet(emptyList())
            val grunnlag = VilkårperioderGrunnlag(aktivitet, GrunnlagYtelse(emptyList()), hentetInformasjon)

            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandling.id, grunnlag))

            val opprettVilkårperiode = opprettVilkårperiodeAktivitet(
                lønnet = VurderingDto(SvarJaNei.NEI),
                begrunnelse = "begrunnelse aktivitet",
                behandlingId = behandling.id,
                kildeId = "finnesIkke",
            )
            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            }.hasMessageContaining("Aktivitet med id=finnesIkke finnes ikke i grunnlag")
        }

        @Test
        fun `skal lagre kildeId på inngangsvilkår for aktivitet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val behandlingId = behandling.id
            val hentetInformasjon = HentetInformasjon(fom = now(), tom = now(), LocalDateTime.now())
            val aktivitet = GrunnlagAktivitet(listOf(periodeGrunnlagAktivitet("123")))
            val grunnlag = VilkårperioderGrunnlag(aktivitet, GrunnlagYtelse(emptyList()), hentetInformasjon)

            vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(behandlingId, grunnlag))

            val opprettVilkårperiode = opprettVilkårperiodeAktivitet(
                lønnet = VurderingDto(SvarJaNei.NEI),
                begrunnelse = "begrunnelse aktivitet",
                behandlingId = behandlingId,
                kildeId = "123",
            )
            vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            val vilkårperiode = vilkårperiodeService.hentVilkårperioder(behandlingId).aktiviteter.single()
            assertThat(vilkårperiode.kildeId).isEqualTo("123")
        }

        @Test
        fun `skal kaste feil hvis målgruppe er ugyldig for stønadstype`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val opprettVilkårperiode = opprettVilkårperiodeMålgruppe(
                type = MålgruppeType.DAGPENGER,
                medlemskap = VurderingDto(SvarJaNei.NEI),
                behandlingId = behandling.id,
            )

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(opprettVilkårperiode)
            }.hasMessageContaining("målgruppe=DAGPENGER er ikke gyldig for ${Stønadstype.BARNETILSYN}")
        }

        @Test
        fun `skal kaste feil ved opprettelse av vilkårperiode med vurdering av medlemskap uten begrunnelse`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeMålgruppe(
                        begrunnelse = "",
                        medlemskap = VurderingDto(SvarJaNei.NEI),
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for vurdering av medlemskap")
        }

        @Test
        fun `skal kaste feil ved opprettelse av vilkårperiode hvis ikke oppfylt delvilkår mangler begrunnelse - dekkes annet regelverk`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeMålgruppe(
                        type = MålgruppeType.AAP,
                        begrunnelse = "",
                        dekkesAvAnnetRegelverk = VurderingDto(SvarJaNei.JA),
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for utgifter dekt av annet regelverk")
        }

        @Test
        fun `skal kaste feil ved opprettelse av vilkårperiode hvis ikke oppfylt delvilkår mangler begrunnelse - lønnet`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeAktivitet(
                        begrunnelse = "",
                        lønnet = VurderingDto(SvarJaNei.JA),
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når målgruppe er nedsatt arbeidsevne`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeMålgruppe(
                        begrunnelse = "",
                        type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                        behandlingId = behandling.id,
                    ),
                )
            }.hasMessageContaining("Mangler begrunnelse for nedsatt arbeidsevne")
        }

        @Nested
        inner class IngenPeriodeGrunnlagAktivitetMålgruppe {
            @Test
            fun `skal kaste feil ved tom og null begrunnelse på ingen aktivitet`() {
                val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        opprettVilkårperiodeAktivitet(
                            begrunnelse = "",
                            type = AktivitetType.INGEN_AKTIVITET,
                            behandlingId = behandling.id,
                            aktivitetsdager = null,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen aktivitet")

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        opprettVilkårperiodeAktivitet(
                            begrunnelse = null,
                            type = AktivitetType.INGEN_AKTIVITET,
                            behandlingId = behandling.id,
                            aktivitetsdager = null,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen aktivitet")
            }

            @Test
            fun `skal kaste feil ved tom og null begrunnelse på ingen målgruppe`() {
                val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        opprettVilkårperiodeMålgruppe(
                            begrunnelse = "",
                            type = MålgruppeType.INGEN_MÅLGRUPPE,
                            behandlingId = behandling.id,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen målgruppe")

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        opprettVilkårperiodeMålgruppe(
                            begrunnelse = null,
                            type = MålgruppeType.INGEN_MÅLGRUPPE,
                            behandlingId = behandling.id,
                        ),
                    )
                }.hasMessageContaining("Mangler begrunnelse for ingen målgruppe")
            }

            @Test
            fun `skal kaste feil dersom aktivitetsdager registreres på aktivitet med type ingen aktivitet`() {
                val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

                assertThatThrownBy {
                    vilkårperiodeService.opprettVilkårperiode(
                        opprettVilkårperiodeAktivitet(
                            begrunnelse = "Begrunnelse",
                            type = AktivitetType.INGEN_AKTIVITET,
                            behandlingId = behandling.id,
                            aktivitetsdager = 5,
                        ),
                    )
                }.hasMessageContaining("Kan ikke registrere aktivitetsdager på ingen aktivitet")
            }
        }

        @Test
        fun `kan ikke opprette periode hvis periode begyner før revurderFra`() {
            val behandling = testoppsettService.oppdater(
                testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
            )

            assertThatThrownBy {
                vilkårperiodeService.opprettVilkårperiode(
                    opprettVilkårperiodeAktivitet(
                        begrunnelse = "Begrunnelse",
                        type = AktivitetType.INGEN_AKTIVITET,
                        behandlingId = behandling.id,
                        aktivitetsdager = 5,
                        fom = now().minusDays(1),
                    ),
                )
            }.hasMessageContaining("Kan ikke opprette periode")
        }
    }

    @Nested
    inner class OppdaterVilkårYtelsePeriode {

        @Test
        fun `skal oppdatere alle felter hvis periode er lagt til manuelt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(
                opprettVilkårperiodeMålgruppe(
                    medlemskap = null,
                    behandlingId = behandling.id,
                ),
            )

            val nyttDato = LocalDate.of(2020, 1, 1)
            val oppdatering = vilkårperiode.tilOppdatering().copy(
                fom = nyttDato,
                tom = nyttDato,
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(
                    medlemskap = VurderingDto(SvarJaNei.JA),
                    dekketAvAnnetRegelverk = null,
                ),
            )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.tom).isEqualTo(nyttDato)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.svar).isEqualTo(SvarJaNei.JA)
            assertThat(oppdatertPeriode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        }

        @Test
        fun `skal oppdatere felter for periode som er lagt til av system`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(
                opprettVilkårperiodeMålgruppe(
                    begrunnelse = "Begrunnelse",
                    medlemskap = VurderingDto(
                        SvarJaNei.JA,
                    ),
                    behandlingId = behandling.id,
                ),
            )

            val oppdatering = vilkårperiode.tilOppdatering().copy(
                begrunnelse = "Oppdatert begrunnelse",
                delvilkår = DelvilkårMålgruppeDto(
                    medlemskap = VurderingDto(SvarJaNei.NEI),
                    dekketAvAnnetRegelverk = null,
                ),
            )
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)

            assertThat(vilkårperiode.resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)

            assertThat(oppdatertPeriode.fom).isEqualTo(vilkårperiode.fom)
            assertThat(oppdatertPeriode.tom).isEqualTo(vilkårperiode.tom)
            assertThat(oppdatertPeriode.begrunnelse).isEqualTo("Oppdatert begrunnelse")
            assertThat(oppdatertPeriode.medlemskap.svar).isEqualTo(SvarJaNei.NEI)
            assertThat(oppdatertPeriode.medlemskap.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `endring av vilkårperioder opprettet i denne behandlingen skal beholde status NY`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(
                opprettVilkårperiodeMålgruppe(
                    medlemskap = null,
                    behandlingId = behandling.id,
                ),
            )
            val oppdatering = vilkårperiode.tilOppdatering()
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)
            assertThat(vilkårperiode.status).isEqualTo(Vilkårstatus.NY)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.NY)
        }

        @Test
        fun `endring av vilkårperioder opprettet kopiert fra tidligere behandling skal få status ENDRET`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val opprinneligVilkårperiode = vilkårperiodeRepository.insert(
                målgruppe(
                    behandlingId = revurdering.forrigeBehandlingId!!,
                ),
            )

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeBehandlingId!!, revurdering.id)

            val vilkårperiode = vilkårperiodeRepository.findByBehandlingId(revurdering.id).single()
            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(
                vilkårperiode.id,
                vilkårperiode.tilOppdatering(),
            )

            assertThat(opprinneligVilkårperiode.status).isEqualTo(Vilkårstatus.NY)
            assertThat(vilkårperiode.status).isEqualTo(Vilkårstatus.UENDRET)
            assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.ENDRET)
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når dekket av annet regelverk endres til ja`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(
                opprettVilkårperiodeMålgruppe(
                    type = MålgruppeType.UFØRETRYGD,
                    dekkesAvAnnetRegelverk = VurderingDto(
                        SvarJaNei.NEI,
                    ),
                    behandlingId = behandling.id,
                ),
            )

            val oppdatering = vilkårperiode.tilOppdatering().copy(
                begrunnelse = "",
                delvilkår = DelvilkårMålgruppeDto(
                    medlemskap = null,
                    dekketAvAnnetRegelverk = VurderingDto(SvarJaNei.JA),
                ),
            )
            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)
            }.hasMessageContaining("Mangler begrunnelse for utgifter dekt av annet regelverk")
        }

        @Test
        fun `skal feile dersom manglende begrunnelse når lønnet endres til ja`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val vilkårperiode = vilkårperiodeService.opprettVilkårperiode(
                opprettVilkårperiodeAktivitet(
                    lønnet = VurderingDto(
                        SvarJaNei.NEI,
                    ),
                    behandlingId = behandling.id,
                ),
            )

            val oppdatering = vilkårperiode.tilOppdatering().copy(
                begrunnelse = "",
                delvilkår = DelvilkårAktivitetDto(
                    lønnet = VurderingDto(SvarJaNei.JA),
                ),
            )
            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(vilkårperiode.id, oppdatering)
            }.hasMessageContaining("Mangler begrunnelse for ikke oppfylt vurdering av lønnet arbeid")
        }

        @Test
        fun `skal ikke kunne oppdatere kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            val målgruppe = målgruppe(
                behandlingId = behandling.id,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOppdatering(),
                )
            }.hasMessageContaining("Kan ikke opprette eller endre vilkårperiode når behandling er låst for videre redigering")
        }

        @Test
        fun `kan ikke oppdatere periode hvis periode begynner før revurderFra`() {
            val behandling = testoppsettService.oppdater(
                testoppsettService.lagBehandlingOgRevurdering().copy(revurderFra = now()),
            )

            val aktivitet = aktivitet(
                behandlingId = behandling.id,
                fom = now().minusMonths(1),
                tom = now().plusMonths(1),
            )
            val periode = vilkårperiodeRepository.insert(aktivitet)

            assertThatThrownBy {
                vilkårperiodeService.oppdaterVilkårperiode(
                    periode.id,
                    periode.tilOppdatering().copy(aktivitetsdager = 3),
                )
            }.hasMessageContaining("Ugyldig endring på periode")
        }

        private fun Vilkårperiode.tilOppdatering(): LagreVilkårperiode {
            return LagreVilkårperiode(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
                delvilkår = faktaOgVurdering.tilDelvilkårDto(),
                begrunnelse = begrunnelse,
                type = type,
                aktivitetsdager = faktaOgVurdering.fakta.takeIfFakta<FaktaAktivitetsdager>()?.aktivitetsdager,
            )
        }
    }

    @Nested
    inner class SlettVilkårperiode {
        @Test
        fun `skal ikke kunne slette vilkårperiode hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            val målgruppe = målgruppe(
                behandlingId = behandling.id,
            )

            val lagretPeriode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(behandling.id))
            }.hasMessageContaining("Kan ikke opprette eller endre vilkårperiode når behandling er låst for videre redigering")
        }

        @Nested
        inner class SlettVilkårperiodePermanent {
            lateinit var behandling: Behandling
            lateinit var lagretPeriode: Vilkårperiode

            @BeforeEach
            fun setUp() {
                behandling =
                    testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.UTREDES))

                val målgruppe = målgruppe(
                    behandlingId = behandling.id,
                )

                lagretPeriode = vilkårperiodeRepository.insert(målgruppe)
            }
        }

        @Nested
        inner class SlettGjenbruktVilkårperiode {
            lateinit var revurdering: Behandling
            lateinit var lagretPeriode: Vilkårperiode

            @BeforeEach
            fun setUp() {
                revurdering = testoppsettService.lagBehandlingOgRevurdering()

                val originalMålgruppe = målgruppe(
                    behandlingId = revurdering.forrigeBehandlingId!!,
                )

                vilkårperiodeRepository.insert(originalMålgruppe)

                val revurderingMålgruppe = originalMålgruppe.copy(
                    id = UUID.randomUUID(),
                    behandlingId = revurdering.id,
                    forrigeVilkårperiodeId = originalMålgruppe.id,
                )

                lagretPeriode = vilkårperiodeRepository.insert(revurderingMålgruppe)
            }

            @Test
            fun `skal ikke kunne slette gjenbrukt periode uten kommentar`() {
                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(revurdering.id, ""))
                }.hasMessageContaining("Mangler kommentar")

                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(lagretPeriode.id, SlettVikårperiode(revurdering.id))
                }.hasMessageContaining("Mangler kommentar")
            }

            @Test
            fun `skal slettemarkere gjenbrukt periode om kommentar er sendt med`() {
                val saksbehandler = "saksbehandlerX"

                testWithBrukerContext(saksbehandler) {
                    vilkårperiodeService.slettVilkårperiode(
                        lagretPeriode.id,
                        SlettVikårperiode(revurdering.id, "kommentar"),
                    )
                }

                val oppdatertPeriode = vilkårperiodeRepository.findByIdOrThrow(lagretPeriode.id)
                assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
                assertThat(oppdatertPeriode.sporbar.endret.endretAv).isEqualTo(saksbehandler)
                assertThat(oppdatertPeriode.status).isEqualTo(Vilkårstatus.SLETTET)
            }

            @Test
            fun `kan ikke slette periode hvis periode begynner før revurderFra`() {
                val behandling = testoppsettService.oppdater(revurdering.copy(revurderFra = now()))

                val aktivitet = aktivitet(
                    behandlingId = behandling.id,
                    fom = now().minusMonths(1),
                    tom = now().plusMonths(1),
                )
                val periode = vilkårperiodeRepository.insert(aktivitet)

                assertThatThrownBy {
                    vilkårperiodeService.slettVilkårperiode(
                        periode.id,
                        SlettVikårperiode(revurdering.id, "kommentar"),
                    )
                }.hasMessageContaining("Kan ikke slette periode")
            }
        }
    }

    @Nested
    inner class Validering {
        @Test
        fun `skal validere stønadsperioder ved opprettelse av vilkårperiode - ingen stønadsperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val periode = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = osloDateNow(),
                    tom = osloDateNow(),
                    delvilkår = VilkårperiodeTestUtil.delvilkårMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

            val response =
                vilkårperiodeService.validerOgLagResponse(behandlingId = behandling.id, periode = periode)

            assertThat(response.stønadsperiodeStatus).isEqualTo(Stønadsperiodestatus.OK)
            assertThat(response.stønadsperiodeFeil).isNull()
        }

        @Test
        fun `skal validere stønadsperioder ved oppdatering av vilkårperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val fom1 = LocalDate.of(2024, 1, 1)
            val tom1 = LocalDate.of(2024, 2, 1)

            val fom2 = LocalDate.of(2024, 2, 1)
            val tom2 = LocalDate.of(2024, 3, 1)

            val opprettetMålgruppe = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = fom1,
                    tom = tom1,
                    delvilkår = VilkårperiodeTestUtil.delvilkårMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

            assertThat(
                vilkårperiodeService.validerOgLagResponse(
                    behandlingId = behandling.id,
                    periode = opprettetMålgruppe,
                ).stønadsperiodeStatus,
            ).isEqualTo(
                Stønadsperiodestatus.OK,
            )

            val opprettetTiltakPeriode = vilkårperiodeService.opprettVilkårperiode(
                LagreVilkårperiode(
                    type = AktivitetType.TILTAK,
                    fom = fom1,
                    tom = tom1,
                    delvilkår = VilkårperiodeTestUtil.delvilkårAktivitetDto(),
                    behandlingId = behandling.id,
                    aktivitetsdager = 5,
                ),
            )
            assertThat(
                vilkårperiodeService.validerOgLagResponse(
                    behandlingId = behandling.id,
                    periode = opprettetTiltakPeriode,
                ).stønadsperiodeStatus,
            ).isEqualTo(
                Stønadsperiodestatus.OK,
            )
            stønadsperiodeService.lagreStønadsperioder(behandling.id, listOf(nyStønadsperiode(fom1, tom1)))

            val oppdatertPeriode = vilkårperiodeService.oppdaterVilkårperiode(
                id = opprettetTiltakPeriode.id,
                vilkårperiode = LagreVilkårperiode(
                    type = AktivitetType.TILTAK,
                    fom = fom2,
                    tom = tom2,
                    delvilkår = VilkårperiodeTestUtil.delvilkårAktivitetDto(),
                    behandlingId = behandling.id,
                    aktivitetsdager = 5,
                ),
            )
            vilkårperiodeService.validerOgLagResponse(behandlingId = behandling.id, periode = opprettetMålgruppe)

            assertThat(
                vilkårperiodeService.validerOgLagResponse(
                    behandlingId = behandling.id,
                    periode = opprettetMålgruppe,
                ).stønadsperiodeStatus,
            ).isEqualTo(
                Stønadsperiodestatus.FEIL,
            )
        }

        private fun nyStønadsperiode(fom: LocalDate = osloDateNow(), tom: LocalDate = osloDateNow()) =
            StønadsperiodeDto(
                id = null,
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                status = StønadsperiodeStatus.NY,
            )
    }

    @Nested
    inner class Grunnlag {

        @Test
        internal fun `skal lagre ned grunnlagsadata på aktiviteter når man henter vilkårsperioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)).isNull()

            val response = vilkårperiodeService.hentVilkårperioderResponse(behandling.id)

            assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)!!.grunnlag.tilDto()).isEqualTo(
                response.grunnlag,
            )
            assertThat(response.grunnlag!!.aktivitet.aktiviteter).isNotEmpty
            assertThat(response.grunnlag!!.ytelse?.perioder).isNotEmpty
        }

        @Test
        internal fun `skal ikke lagre ned grunnlagsadata for behandling som ikke er redigerbar`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))

            vilkårperiodeService.hentVilkårperioderResponse(behandling.id)

            assertThat(vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)).isNull()
        }

        @Test
        fun `skal kun ta med aktiviteter som er stønadsberettiget i grunnlaget`() {
            val idStønadsberettiget = "1"
            every {
                registerAktivitetClient.hentAktiviteter(any(), any(), any())
            } returns listOf(
                aktivitetArenaDto(id = idStønadsberettiget, erStønadsberettiget = true),
                aktivitetArenaDto(id = "2", erStønadsberettiget = false),
            )

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.hentVilkårperioderResponse(behandling.id)

            val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
            assertThat(grunnlag!!.grunnlag.aktivitet.aktiviteter.map { it.id }).hasSize(1)
                .contains(idStønadsberettiget)
        }

        @Test
        fun `veileder skal ikke hentes då det opprettes grunnlag når behandlingStatus=OPPRETTET`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val exception = catchThrowableOfType<ApiFeil> {
                testWithBrukerContext(groups = listOf(rolleConfig.veilederRolle)) {
                    vilkårperiodeService.hentVilkårperioderResponse(behandling.id)
                }
            }
            assertThat(exception.frontendFeilmelding).contains("Behandlingen er ikke påbegynt")
        }

        @Test
        fun `veileder skal ikke kunne hente behandlingen hvis statusen er annet enn UTREDES`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.UTREDES))

            val exception = catchThrowableOfType<ApiFeil> {
                testWithBrukerContext(groups = listOf(rolleConfig.veilederRolle)) {
                    vilkårperiodeService.hentVilkårperioderResponse(behandling.id)
                }
            }
            assertThat(exception.frontendFeilmelding).contains("Behandlingen er ikke påbegynt")
        }

        @Test
        fun `saksbehandler skal kunne opprette grunnlag hvis behandlingStatus=OPPRETTET`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
                vilkårperiodeService.hentVilkårperioderResponse(behandling.id)
            }
        }

        @Test
        fun `skal ikke ta ta med ferdig avklarte perioder fra AAP som grunnlag`() {
            val nesteDag = now().plusDays(1) // for å få 2 ulike AAP-perioder
            every {
                ytelseClient.hentYtelser(any())
            } returns YtelsePerioderDto(
                perioder = listOf(
                    YtelsePeriode(TypeYtelsePeriode.AAP, now(), now(), aapErFerdigAvklart = false),
                    YtelsePeriode(TypeYtelsePeriode.AAP, nesteDag, nesteDag, aapErFerdigAvklart = true),
                    YtelsePeriode(TypeYtelsePeriode.ENSLIG_FORSØRGER, now(), now(), aapErFerdigAvklart = null),
                ),
                hentetInformasjon = emptyList(),
            )

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.hentVilkårperioderResponse(behandling.id)

            val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
            val perioder = grunnlag!!.grunnlag.ytelse.perioder
            assertThat(perioder).containsExactlyInAnyOrder(
                PeriodeGrunnlagYtelse(TypeYtelsePeriode.AAP, now(), now()),
                PeriodeGrunnlagYtelse(TypeYtelsePeriode.ENSLIG_FORSØRGER, now(), now()),
            )
        }

        @Test
        fun `skal ikke ta med EF perioder med barnetilsyn som grunnlag`() {
            every {
                ytelseClient.hentYtelser(any())
            } returns YtelsePerioderDto(
                perioder = listOf(
                    YtelsePeriode(TypeYtelsePeriode.AAP, now(), now(), aapErFerdigAvklart = false),
                    YtelsePeriode(
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        now(),
                        now(),
                        aapErFerdigAvklart = null,
                        EnsligForsørgerStønadstypeKontrakter.BARNETILSYN,
                    ),
                    YtelsePeriode(
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        now(),
                        now(),
                        aapErFerdigAvklart = null,
                        EnsligForsørgerStønadstypeKontrakter.SKOLEPENGER,
                    ),
                    YtelsePeriode(
                        TypeYtelsePeriode.ENSLIG_FORSØRGER,
                        now(),
                        now(),
                        aapErFerdigAvklart = null,
                        EnsligForsørgerStønadstypeKontrakter.OVERGANGSSTØNAD,
                    ),
                ),
                hentetInformasjon = emptyList(),
            )

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            vilkårperiodeService.hentVilkårperioderResponse(behandling.id)

            val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandling.id)
            val perioder = grunnlag!!.grunnlag.ytelse.perioder
            assertThat(perioder).containsExactlyInAnyOrder(
                PeriodeGrunnlagYtelse(TypeYtelsePeriode.AAP, now(), now()),
                PeriodeGrunnlagYtelse(TypeYtelsePeriode.ENSLIG_FORSØRGER, now(), now(), SKOLEPENGER),
                PeriodeGrunnlagYtelse(TypeYtelsePeriode.ENSLIG_FORSØRGER, now(), now(), OVERGANGSSTØNAD),
            )
        }
    }

    @Nested
    inner class GjenbrukVilkårperioder {
        @Test
        fun `skal gjenbruke vilkår fra forrige behandling`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeVilkårperioder = listOf(
                målgruppe(behandlingId = revurdering.forrigeBehandlingId!!),
                aktivitet(behandlingId = revurdering.forrigeBehandlingId!!),
            )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeBehandlingId!!, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)

            // TODO "nullstiller" felter fordi usingRecursiveComparison ikke virker som forventet
            val nullstilteRes = res.map { oppdatert ->
                val forrige = eksisterendeVilkårperioder.single { it.id == oppdatert.forrigeVilkårperiodeId!! }
                oppdatert.copy(
                    id = forrige.id,
                    sporbar = forrige.sporbar,
                    behandlingId = forrige.behandlingId,
                    forrigeVilkårperiodeId = forrige.forrigeVilkårperiodeId,
                    status = forrige.status,
                )
            }
            assertThat(nullstilteRes).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                "id",
                "sporbar",
                "behandlingId",
                "forrigeVilkårperiodeId",
                "status",
            ).containsExactlyInAnyOrderElementsOf(eksisterendeVilkårperioder)
            assertThat(res.map { it.status }).containsOnly(Vilkårstatus.UENDRET)
        }

        @Test
        fun `skal ikke gjenbruke slettede vilkår fra forrige behandling`() {
            val revurdering = testoppsettService.lagBehandlingOgRevurdering()

            val eksisterendeVilkårperioder = listOf(
                målgruppe(behandlingId = revurdering.forrigeBehandlingId!!),
                målgruppe(
                    behandlingId = revurdering.forrigeBehandlingId!!,
                    resultat = ResultatVilkårperiode.SLETTET,
                    slettetKommentar = "slettet",
                ),
                aktivitet(behandlingId = revurdering.forrigeBehandlingId!!),
                aktivitet(
                    behandlingId = revurdering.forrigeBehandlingId!!,
                    resultat = ResultatVilkårperiode.SLETTET,
                    slettetKommentar = "slettet",
                ),
            )

            vilkårperiodeRepository.insertAll(eksisterendeVilkårperioder)

            vilkårperiodeService.gjenbrukVilkårperioder(revurdering.forrigeBehandlingId!!, revurdering.id)

            val res = vilkårperiodeRepository.findByBehandlingId(revurdering.id)
            assertThat(res).hasSize(2)
            res.map { assertThat(it.resultat).isNotEqualTo(ResultatVilkårperiode.SLETTET) }
        }
    }

    @Nested
    inner class OppdaterGrunnlag {

        @Test
        fun `skal kunne oppdatere grunnlaget når en behandling redigerbar og i riktig steg`() {
            every {
                registerAktivitetClient.hentAktiviteter(any(), any(), any())
            } answers {
                listOf(aktivitetArenaDto(id = "1", erStønadsberettiget = true))
            } andThenAnswer {
                listOf(aktivitetArenaDto(id = "2", erStønadsberettiget = true))
            }

            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.INNGANGSVILKÅR))
            val grunnlag = vilkårperiodeService.hentVilkårperioderResponse(behandling.id).grunnlag!!
            val grunnlag2 = vilkårperiodeService.hentVilkårperioderResponse(behandling.id).grunnlag!!

            testWithBrukerContext(groups = listOf(rolleConfig.saksbehandlerRolle)) {
                vilkårperiodeService.oppdaterGrunnlag(behandling.id)
            }

            val grunnlag3 = vilkårperiodeService.hentVilkårperioderResponse(behandling.id).grunnlag!!

            assertThat(grunnlag.aktivitet.aktiviteter.map { it.id }).containsExactly("1")
            assertThat(grunnlag2.aktivitet.aktiviteter.map { it.id }).containsExactly("1")

            // Oppdatert grunnlag inneholder kun id=2
            assertThat(grunnlag3.aktivitet.aktiviteter.map { it.id }).containsExactly("2")
        }

        @Test
        fun `skal bruke tidligere hentet informasjon sin fom og bruke tom fra dagens dato`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.INNGANGSVILKÅR))
            val fomFørsteGangHentet = now().minusYears(3).minusDays(1)
            behandling.let {
                val hentetInformasjon =
                    HentetInformasjon(fom = fomFørsteGangHentet, tom = now(), LocalDateTime.now())
                val aktivitet = GrunnlagAktivitet(emptyList())
                val grunnlag = VilkårperioderGrunnlag(aktivitet, GrunnlagYtelse(emptyList()), hentetInformasjon)
                vilkårperioderGrunnlagRepository.insert(VilkårperioderGrunnlagDomain(it.id, grunnlag))
            }
            testWithBrukerContext("beh1", listOf(rolleConfig.saksbehandlerRolle)) {
                vilkårperiodeService.oppdaterGrunnlag(behandling.id)
            }
            with(vilkårperioderGrunnlagRepository.findByIdOrThrow(behandling.id)) {
                assertThat(sporbar.opprettetAv).isEqualTo("VL")
                assertThat(sporbar.endret.endretAv).isEqualTo("beh1")
                assertThat(grunnlag.hentetInformasjon.fom).isEqualTo(fomFørsteGangHentet)
                assertThat(grunnlag.hentetInformasjon.tom)
                    .isEqualTo(YearMonth.now().plusYears(1).atEndOfMonth())
            }
        }

        @Test
        fun `skal ikke kunne oppdatere dataen når behandlingen har feil steg`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(steg = StegType.VILKÅR))
            val feil = assertThrows<Feil> { vilkårperiodeService.oppdaterGrunnlag(behandling.id) }
            assertThat(feil.frontendFeilmelding)
                .isEqualTo("Kan ikke oppdatere grunnlag når behandlingen er i annet steg enn vilkår.")
        }

        @Test
        fun `skal ikke kunne oppdatere dataen når behandlingen er låst`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))
            val feil = assertThrows<Feil> { vilkårperiodeService.oppdaterGrunnlag(behandling.id) }
            assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke oppdatere grunnlag når behandlingen er låst")
        }
    }
}
