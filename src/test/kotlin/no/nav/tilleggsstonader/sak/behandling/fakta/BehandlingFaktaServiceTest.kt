package no.nav.tilleggsstonader.sak.behandling.fakta

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataMedMetadata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.lagSøknadBarn
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil.søknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BehandlingFaktaServiceTest {

    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val søknadService = mockk<SøknadService>()
    val barnService = mockk<BarnService>()
    val service = BehandlingFaktaService(
        grunnlagsdataService,
        søknadService,
        barnService,
    )

    val behandlingId = UUID.randomUUID()

    @Test
    fun `skal mappe søknad og grunnlag`() {
        every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata()
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
            every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata(
                grunnlagsdata = lagGrunnlagsdata(
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
            every { grunnlagsdataService.hentFraRegister(behandlingId) } returns grunnlagsdataMedMetadata(
                grunnlagsdata = lagGrunnlagsdata(
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
}
