package no.nav.tilleggsstonader.sak.behandling.fakta

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KodeverkServiceUtil.mockedKodeverkService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagNavn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagBarnMedBarnepass
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagDokumentasjon
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSøknadBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.søknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingFaktaServiceTest {
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val søknadService = mockk<SøknadService>()
    val barnService = mockk<BarnService>()
    val faktaArbeidOgOppholdMapper = FaktaArbeidOgOppholdMapper(mockedKodeverkService())
    val fagsakService = mockk<FagsakService>()

    val service =
        BehandlingFaktaService(
            grunnlagsdataService,
            søknadService,
            barnService,
            faktaArbeidOgOppholdMapper,
            fagsakService,
        )

    val behandlingId = BehandlingId.random()

    @BeforeEach
    fun setUp() {
        every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
    }

    @Test
    fun `skal mappe søknad og grunnlag`() {
        every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns grunnlagsdataDomain()
        every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn()
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(behandlingBarn(personIdent = "1", id = BarnId.fromString("60921c76-f8ef-4000-9824-f127a50a575e")))

        val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

        val data = service.hentFakta(behandlingId)
        assertFileIsEqual("vilkår/vilkårGrunnlagDto.json", data)
    }

    @Nested
    inner class FaktaBarnTest {
        @Test
        fun `skal mappe søknadsgrunnlag for de barn som fantes i søknaden`() {
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            barn = listOf(lagGrunnlagsdataBarn("1"), lagGrunnlagsdataBarn("2")),
                        ),
                )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    barn = setOf(lagSøknadBarn(ident = "1")),
                )

            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(personIdent = "1"),
                    behandlingBarn(personIdent = "2"),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            val data = service.hentFakta(behandlingId) as BehandlingFaktaTilsynBarnDto

            assertThat(data.barn).hasSize(2)
            data.barn[0].let {
                assertThat(it.ident).isEqualTo("1")
                assertThat(it.søknadgrunnlag).isNotNull
            }
            data.barn[1].let {
                assertThat(it.ident).isEqualTo("2")
                assertThat(it.søknadgrunnlag).isNull()
            }
        }

        @Test
        fun `skal kaste feil hvis ikke alle barnen fra søknaden har grunnlagsdata`() {
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            barn = listOf(lagGrunnlagsdataBarn("1")),
                        ),
                )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    barn = setOf(lagSøknadBarn(ident = "1"), lagSøknadBarn(ident = "2"), lagSøknadBarn(ident = "3")),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            assertThatThrownBy {
                service.hentFakta(behandlingId)
            }.hasMessage("Mangler grunnlagsdata for barn i søknad (2,3)")
        }

        @Test
        fun `hvis barnet er under 9 år så har man ikke fullført fjerdetrinn for å kunne automatisk prefylle delvilkår i frontend`() {
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            barn = listOf(lagGrunnlagsdataBarn("1", fødselsdato = LocalDate.now().minusYears(8))),
                        ),
                )
            val barnMedBarnepass = lagBarnMedBarnepass(startetIFemte = null, årsak = null)
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    barn = setOf(lagSøknadBarn(ident = "1", data = barnMedBarnepass)),
                )
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(personIdent = "1"),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            val fakta = service.hentFakta(behandlingId) as BehandlingFaktaTilsynBarnDto

            assertThat(
                fakta.barn
                    .single()
                    .vilkårFakta.harFullførtFjerdetrinn,
            ).isEqualTo(JaNei.NEI)
        }

        @Test
        fun `hvis barnet er over 11 år så vet man ikke om barnet fullført fjerdetrinn`() {
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(
                    grunnlag =
                        lagGrunnlagsdata(
                            barn = listOf(lagGrunnlagsdataBarn("1", fødselsdato = LocalDate.now().minusYears(11))),
                        ),
                )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    barn = setOf(lagSøknadBarn(ident = "1")),
                )
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(personIdent = "1"),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            val fakta = service.hentFakta(behandlingId) as BehandlingFaktaTilsynBarnDto

            assertThat(
                fakta.barn
                    .single()
                    .vilkårFakta.harFullførtFjerdetrinn,
            ).isNull()
        }
    }

    @Nested
    inner class FaktaDokumentasjonTest {
        @Test
        fun `skal mappe dokumentasjon`() {
            val dokumentasjon = lagDokumentasjon()
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = emptyList()))
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    journalpostId = "journalpostId2",
                    barn = emptySet(),
                    data = lagSkjemaBarnetilsyn(dokumentasjon = listOf(dokumentasjon)),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            val fakta = service.hentFakta(behandlingId)

            assertThat(fakta.dokumentasjon?.journalpostId).contains("journalpostId2")
            with(fakta.dokumentasjon!!.dokumentasjon.single()) {
                assertThat(type).isEqualTo(dokumentasjon.type.tittel)
                assertThat(dokumenter.map { it.dokumentInfoId })
                    .containsExactlyElementsOf(dokumentasjon.dokumenter.map { it.dokumentInfoId })
                assertThat(identBarn).isNull()
            }
        }

        @Test
        fun `skal returnere returnere navnet på barnet hvis barnIdent finnes`() {
            val navn = lagNavn("Fornavn barn1")
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(
                    grunnlag = lagGrunnlagsdata(barn = listOf(lagGrunnlagsdataBarn("1", navn = navn))),
                )
            val dokumentasjon = lagDokumentasjon(identBarn = "1")
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns
                søknadBarnetilsyn(
                    data = lagSkjemaBarnetilsyn(dokumentasjon = listOf(dokumentasjon)),
                    barn = setOf(lagSøknadBarn(ident = "1")),
                )
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(personIdent = "1"),
                )

            val fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN)

            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak

            val fakta = service.hentFakta(behandlingId)

            with(fakta.dokumentasjon!!.dokumentasjon.single()) {
                assertThat(type).isEqualTo("${dokumentasjon.type.tittel} - Fornavn barn1")
                assertThat(identBarn).isEqualTo("1")
            }
        }
    }
}
