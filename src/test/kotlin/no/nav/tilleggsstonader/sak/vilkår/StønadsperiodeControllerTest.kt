package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
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
    lateinit var vilkårService: VilkårService

    @Autowired
    lateinit var vilkårStegService: VilkårStegService

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
        opprettOppfylltInngangsvilkårForAktivitet(behandling)
    }

    private fun opprettOppfylltInngangsvilkårForAktivitet(behandling: Behandling) {
        val aktivitet = opprettAktivitet(behandling)
        val delvilkår1 = DelvilkårDto(
            Vilkårsresultat.OPPFYLT,
            listOf(
                VurderingDto(RegelId.LØNN_GJENNOM_TILTAK, SvarId.NEI),
            ),
        )
        val delvilkår2 = DelvilkårDto(
            Vilkårsresultat.OPPFYLT,
            listOf(
                VurderingDto(RegelId.MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET, SvarId.NEI),
            ),
        )
        vilkårStegService.oppdaterVilkår(
            SvarPåVilkårDto(
                aktivitet.vilkår.id,
                behandling.id,
                listOf(delvilkår1, delvilkår2),
            ),
        )
    }

    private fun opprettMålgruppe(behandling: Behandling): VilkårperiodeDto =
        vilkårService.opprettVilkårperiode(
            behandling.id,
            OpprettVilkårperiode(
                type = MålgruppeType.AAP,
                fom = dagensDato,
                tom = dagensDato,
                detaljer = DetaljerMålgruppe(
                    medlemskap = SvarJaNei.JA,
                ),
            ),
        )

    private fun opprettAktivitet(behandling: Behandling): VilkårperiodeDto =
        vilkårService.opprettVilkårperiode(
            behandling.id,
            OpprettVilkårperiode(
                type = AktivitetType.TILTAK,
                fom = dagensDato,
                tom = dagensDato,
                detaljer = DetaljerAktivitet(
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
