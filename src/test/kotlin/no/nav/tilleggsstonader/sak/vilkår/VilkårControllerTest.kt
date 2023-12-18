package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barn2Fnr
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barnFnr
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate

class VilkårControllerTest : IntegrationTest() {

    @Autowired
    lateinit var barnRepository: BarnRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = barnFnr))
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = barn2Fnr))

        opprettVilkårperiode(
            behandling,
            OpprettVilkårperiode(MålgruppeType.AAP, fom = LocalDate.now(), tom = LocalDate.now()),
        )

        val hentedeVilkårperioder = hentVilkårperioder(behandling)

        assertThat(hentedeVilkårperioder.målgrupper).hasSize(1)
        assertThat(hentedeVilkårperioder.aktiviteter).isEmpty()

        val målgruppe = hentedeVilkårperioder.målgrupper[0]
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)
        assertThat(målgruppe.vilkår.vilkårType).isEqualTo(VilkårType.MÅLGRUPPE_AAP)
        assertThat(målgruppe.vilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(målgruppe.vilkår.delvilkårsett).isEmpty()
    }

    private fun hentVilkårperioder(behandling: Behandling) =
        restTemplate.exchange<Vilkårperioder>(
            localhost("api/vilkar/${behandling.id}/periode"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun opprettVilkårperiode(
        behandling: Behandling,
        opprettVilkårperiode: OpprettVilkårperiode,
    ) = restTemplate.exchange<VilkårperiodeDto>(
        localhost("api/vilkar/${behandling.id}/periode"),
        HttpMethod.POST,
        HttpEntity(opprettVilkårperiode, headers),
    ).body!!
}
