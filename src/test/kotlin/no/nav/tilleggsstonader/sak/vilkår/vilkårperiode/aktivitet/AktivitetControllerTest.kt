package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.aktivitet.LagreAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.util.UUID

class AktivitetControllerTest : IntegrationTest() {

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        sendOpprettAktivitetRequest(ulønnetTiltak.copy(behandlingId = behandling.id))

        val lagretAktivitet = vilkårperiodeRepository.findByBehandlingId(behandling.id)

        assertThat(lagretAktivitet).hasSize(1)

        val aktivitet = lagretAktivitet.first()
        assertThat(aktivitet.type).isEqualTo(AktivitetType.TILTAK)
        assertThat(aktivitet.faktaOgVurdering.fakta).isEqualTo(FaktaAktivitetTilsynBarn(5))
    }

    @Test
    fun `skal ikke kunne oppdatere aktivitetstypen`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val eksisterendeAktivitet = VilkårperiodeTestUtil.aktivitet(behandlingId = behandling.id)
        vilkårperiodeRepository.insert(eksisterendeAktivitet)

        catchProblemDetailException {
            sendOppdaterAktivitetRequest(
                lagreAktivitet = ulønnetTiltak.copy(behandlingId = behandling.id, type = AktivitetType.UTDANNING),
                aktivitetId = eksisterendeAktivitet.id,
            )
        }
    }

    @Test
    fun `skal kunne oppdatere eksisterende aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val eksisterendeAktivitet = VilkårperiodeTestUtil.aktivitet(behandlingId = behandling.id)
        vilkårperiodeRepository.insert(eksisterendeAktivitet)

        val nyFom = osloDateNow()

        sendOppdaterAktivitetRequest(
            lagreAktivitet = ulønnetTiltak.copy(behandlingId = behandling.id, fom = nyFom),
            aktivitetId = eksisterendeAktivitet.id,
        )

        val oppdatertAktivitet = vilkårperiodeRepository.findByBehandlingId(behandling.id)

        assertThat(oppdatertAktivitet).hasSize(1)

        assertThat(oppdatertAktivitet.first().fom).isEqualTo(nyFom)
    }

    @Test
    fun `skal ikke kunne oppdatere aktivitet som har en annen behandlingId enn den som sendes inn`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingForAnnenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
            testoppsettService.lagre(behandling(it))
        }

        val eksisterendeAktivitet = VilkårperiodeTestUtil.aktivitet(behandlingId = behandling.id)
        vilkårperiodeRepository.insert(eksisterendeAktivitet)

        val exception = catchProblemDetailException {
            sendOppdaterAktivitetRequest(
                lagreAktivitet = ulønnetTiltak.copy(behandlingId = behandlingForAnnenFagsak.id),
                aktivitetId = eksisterendeAktivitet.id,
            )
        }
        assertThat(exception.detail.detail).contains("BehandlingId er ikke lik")
    }

    private fun sendOpprettAktivitetRequest(
        lagreAktivitet: LagreAktivitet,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode2/aktivitet"),
        HttpMethod.POST,
        HttpEntity(lagreAktivitet, headers),
    ).body!!

    private fun sendOppdaterAktivitetRequest(
        lagreAktivitet: LagreAktivitet,
        aktivitetId: UUID,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode2/aktivitet/$aktivitetId"),
        HttpMethod.POST,
        HttpEntity(lagreAktivitet, headers),
    ).body!!
}

private val ulønnetTiltak = LagreAktivitet(
    type = AktivitetType.TILTAK,
    behandlingId = BehandlingId(UUID.randomUUID()),
    fom = osloDateNow(),
    tom = osloDateNow(),
    aktivitetsdager = 5,
    svarLønnet = SvarJaNei.NEI,
)
