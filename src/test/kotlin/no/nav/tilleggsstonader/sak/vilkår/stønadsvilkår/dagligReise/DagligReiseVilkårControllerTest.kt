package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.hentReglerDagligReise
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.opprettVilkårDagligReise
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DagligReiseVilkårControllerTest : IntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO)
    val behandling = behandling(fagsak)

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svarId = SvarId.JA),
            RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svarId = SvarId.JA),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre ned et nytt vilkår for daglig reise`() {
        val nyttVilkår =
            LagreDagligReiseDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )

        val resultat = opprettVilkårDagligReise(nyttVilkår, behandling.id)
        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.delvilkårsett).hasSize(1)
        assertThat(resultat.fakta).isNotNull()
        assertThat(resultat.fakta!!.type).isEqualTo(TypeDagligReise.OFFENTLIG_TRANSPORT)
    }

    @Test
    fun `skal hente alle regler som tilhører daglig reise`() {
        val resultat = hentReglerDagligReise()

        FileUtil.assertFileIsEqual("vilkår/regelstruktur/DAGLIG_REISE_OFFENTLIG_TRANSPORT.json", resultat)
    }

    private fun faktaOffentligTransport(
        reisedagerPerUke: Int = 5,
        prisEnkelbillett: Int? = 40,
        prisSyvdagersbillett: Int? = null,
        prisTrettidagersbillett: Int? = 800,
    ) = FaktaDagligReiseOffentligTransportDto(
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
    )
}
