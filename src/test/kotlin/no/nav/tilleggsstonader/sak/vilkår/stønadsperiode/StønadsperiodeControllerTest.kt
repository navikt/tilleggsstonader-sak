package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarAktivitetBarnetilsynDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.FaktaOgSvarMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeNy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange

class StønadsperiodeControllerTest : IntegrationTest() {

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    private val dagensDato = osloDateNow()

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente stønadsperioder`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

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
        vilkårperiodeService.opprettVilkårperiodeNy(
            LagreVilkårperiodeNy(
                type = MålgruppeType.AAP,
                fom = dagensDato,
                tom = dagensDato,
                faktaOgSvar = FaktaOgSvarMålgruppeDto(svarUtgifterDekketAvAnnetRegelverk = SvarJaNei.NEI),
                behandlingId = behandling.id,
            ),
        )

    private fun opprettAktivitet(behandling: Behandling): Vilkårperiode =
        vilkårperiodeService.opprettVilkårperiodeNy(
            LagreVilkårperiodeNy(
                type = AktivitetType.TILTAK,
                fom = dagensDato,
                tom = dagensDato,
                faktaOgSvar = FaktaOgSvarAktivitetBarnetilsynDto(
                    svarLønnet = SvarJaNei.NEI,
                    aktivitetsdager = 5,
                ),
                behandlingId = behandling.id,
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
        status = StønadsperiodeStatus.NY,
    )
}
