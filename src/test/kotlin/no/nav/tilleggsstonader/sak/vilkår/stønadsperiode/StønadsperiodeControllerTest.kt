package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OpprettVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate

class StønadsperiodeControllerTest : IntegrationTest() {

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    private val dagensDato = LocalDate.now()

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente stønadsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.barnFnr))
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.barn2Fnr))

        opprettOppfylteInngangsvilkår(behandling)

        val nyeStønadsperioder = listOf(nyStønadsperiode())
        val opprettStønadsperiodeResponse = lagreStønadsperioder(behandling, nyeStønadsperioder)

        val hentedeStønadsperioder = hentStønadsperioder(behandling)

        assertThat(opprettStønadsperiodeResponse).hasSize(1)
        assertThat(opprettStønadsperiodeResponse[0].id).isNotNull()
        assertThat(hentedeStønadsperioder).containsExactlyElementsOf(opprettStønadsperiodeResponse)
    }

    private fun opprettOppfylteInngangsvilkår(behandling: Behandling) {
        opprettMålgruppe(behandling)
        opprettAktivitet(behandling)
    }

    private fun opprettMålgruppe(behandling: Behandling): Vilkårperiode =
        vilkårperiodeService.opprettVilkårperiode(
            behandling.id,
            OpprettVilkårperiode(
                type = MålgruppeType.AAP,
                fom = dagensDato,
                tom = dagensDato,
                delvilkår = DelvilkårMålgruppeDto(
                    medlemskap = SvarJaNei.JA,
                ),
            ),
        )

    private fun opprettAktivitet(behandling: Behandling): Vilkårperiode =
        vilkårperiodeService.opprettVilkårperiode(
            behandling.id,
            OpprettVilkårperiode(
                type = AktivitetType.TILTAK,
                fom = dagensDato,
                tom = dagensDato,
                delvilkår = DelvilkårAktivitetDto(
                    lønnet = SvarJaNei.NEI,
                    mottarSykepenger = SvarJaNei.NEI,
                ),
            ),
        )

    private fun hentStønadsperioder(behandling: Behandling) =
        restTemplate.exchange<List<StønadsperiodeDto>>(
            localhost("api/stonadsperiode/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun lagreStønadsperioder(
        behandling: Behandling,
        nyeStønadsperioder: List<StønadsperiodeDto>,
    ) = restTemplate.exchange<List<StønadsperiodeDto>>(
        localhost("api/stonadsperiode/${behandling.id}"),
        HttpMethod.POST,
        HttpEntity(nyeStønadsperioder, headers),
    ).body!!

    private fun nyStønadsperiode() = StønadsperiodeDto(
        id = null,
        fom = dagensDato,
        tom = dagensDato,
        målgruppe = MålgruppeType.AAP,
        aktivitet = AktivitetType.TILTAK,
    )
}
