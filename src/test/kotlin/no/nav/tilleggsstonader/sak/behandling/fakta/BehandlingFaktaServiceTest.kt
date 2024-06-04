package no.nav.tilleggsstonader.sak.behandling.fakta

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KodeverkServiceUtil.mockedKodeverkService
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.arenaStatusDto
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagNavn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagDokumentasjon
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSøknadBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.søknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BehandlingFaktaServiceTest {

    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val søknadService = mockk<SøknadService>()
    val barnService = mockk<BarnService>()
    val faktaArbeidOgOppholdMapper = FaktaArbeidOgOppholdMapper(mockedKodeverkService())
    val arenaService = mockk<ArenaService>()

    val service = BehandlingFaktaService(
        grunnlagsdataService,
        søknadService,
        barnService,
        faktaArbeidOgOppholdMapper,
        arenaService,
    )

    val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
        every { arenaService.hentStatus(any()) } returns arenaStatusDto()
    }

    @Test
    fun `skal mappe søknad og grunnlag`() {
        every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns grunnlagsdataDomain()
        every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn()
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(behandlingBarn(personIdent = "1", id = UUID.fromString("60921c76-f8ef-4000-9824-f127a50a575e")))

        val data = service.hentFakta(behandlingId)
        assertFileIsEqual("vilkår/vilkårGrunnlagDto.json", data)
    }

    @Nested
    inner class FaktaBarnTest {

        @Test
        fun `skal mappe søknadsgrunnlag for de barn som fantes i søknaden`() {
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns grunnlagsdataDomain(
                grunnlag = lagGrunnlagsdata(
                    barn = listOf(lagGrunnlagsdataBarn("1"), lagGrunnlagsdataBarn("2")),
                ),
            )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                barn = setOf(lagSøknadBarn(ident = "1")),
            )

            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(personIdent = "1"),
                behandlingBarn(personIdent = "2"),
            )

            val data = service.hentFakta(behandlingId)

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
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns grunnlagsdataDomain(
                grunnlag = lagGrunnlagsdata(
                    barn = listOf(lagGrunnlagsdataBarn("1")),
                ),
            )
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                barn = setOf(lagSøknadBarn(ident = "1"), lagSøknadBarn(ident = "2"), lagSøknadBarn(ident = "3")),
            )
            assertThatThrownBy {
                service.hentFakta(behandlingId)
            }.hasMessage("Mangler grunnlagsdata for barn i søknad (2,3)")
        }
    }

    @Nested
    inner class FaktaDokumentasjonTest {

        @Test
        fun `skal mappe dokumentasjon`() {
            val dokumentasjon = lagDokumentasjon()
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns
                grunnlagsdataDomain(grunnlag = lagGrunnlagsdata(barn = emptyList()))
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                journalpostId = "journalpostId2",
                barn = emptySet(),
                data = lagSkjemaBarnetilsyn(dokumentasjon = listOf(dokumentasjon)),
            )

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
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns grunnlagsdataDomain(
                grunnlag = lagGrunnlagsdata(barn = listOf(lagGrunnlagsdataBarn("1", navn = navn))),
            )
            val dokumentasjon = lagDokumentasjon(identBarn = "1")
            every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknadBarnetilsyn(
                data = lagSkjemaBarnetilsyn(dokumentasjon = listOf(dokumentasjon)),
                barn = setOf(lagSøknadBarn(ident = "1")),
            )
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(personIdent = "1"),
            )

            val fakta = service.hentFakta(behandlingId)

            with(fakta.dokumentasjon!!.dokumentasjon.single()) {
                assertThat(type).isEqualTo("${dokumentasjon.type.tittel} - Fornavn barn1")
                assertThat(identBarn).isEqualTo("1")
            }
        }
    }
}
