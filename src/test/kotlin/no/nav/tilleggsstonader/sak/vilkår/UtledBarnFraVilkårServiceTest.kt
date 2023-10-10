package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UtledBarnFraVilkårServiceTest {

    @Test
    internal fun `skal finne ny barnId for barn for barn fra søknaden`() {
        val søknadBarnId = UUID.randomUUID()
        val tidligereBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarn)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarn)
        val utledetBarnIdMap = VilkårService.byggBarnMapFraTidligereTilNyId(
            alleBarnPåForrigeBehandling,
            alleBarnPåGjeldendeBehandling,
        )

        assertThat(utledetBarnIdMap[tidligereBarn.id]?.id).isEqualTo(nyttBarn.id)
    }

    @Test
    internal fun `skal finne ny barnId for barn for barn fra søknaden og ikke for nytt barn`() {
        val søknadBarnId = UUID.randomUUID()
        val tidligereBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarnA = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarnB = opprettBarn(søknadBarnId = søknadBarnId)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarn)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarnA, nyttBarnB)
        val utledetBarnIdMap = VilkårService.byggBarnMapFraTidligereTilNyId(
            alleBarnPåForrigeBehandling,
            alleBarnPåGjeldendeBehandling,
        )

        assertThat(utledetBarnIdMap[tidligereBarn.id]?.id).isEqualTo(nyttBarnA.id)
        assertThat(utledetBarnIdMap).hasSize(1)
    }

    @Test
    internal fun `skal finne ny barnId for barn for barn basert på personIdent`() {
        val personIdentA = "1234123412"
        val personIdentB = "99988877721"
        val tidligereBarnA = opprettBarn(personIdent = personIdentA)
        val tidligereBarnB = opprettBarn(personIdent = personIdentB)
        val nyttBarnA = opprettBarn(personIdent = personIdentA)
        val nyttBarnB = opprettBarn(personIdent = personIdentB)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarnA, tidligereBarnB)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarnB, nyttBarnA)
        val utledetBarnIdMap = VilkårService.byggBarnMapFraTidligereTilNyId(
            alleBarnPåForrigeBehandling,
            alleBarnPåGjeldendeBehandling,
        )

        assertThat(utledetBarnIdMap[tidligereBarnA.id]?.id).isEqualTo(nyttBarnA.id)
        assertThat(utledetBarnIdMap[tidligereBarnB.id]?.id).isEqualTo(nyttBarnB.id)
    }

    private fun opprettBarn(
        søknadBarnId: UUID? = null,
        navn: String = "navn",
        personIdent: String = "barnid",
    ): BehandlingBarn = BehandlingBarn(
        behandlingId = UUID.randomUUID(),
        søknadBarnId = søknadBarnId,
        ident = personIdent,
    )
}
