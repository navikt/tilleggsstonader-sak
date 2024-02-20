package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.tilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

internal class TilkjentYtelseRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var repository: TilkjentYtelseRepository

    @Test
    fun `Opprett og hent tilkjent ytelse`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = tilkjentYtelse(behandling.id)
        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!

        assertThat(hentetTilkjentYtelse.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse).isNotEmpty
    }

    @Test
    fun `Skal kunne oppdatere tilkjent ytelse med nye andeler`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = repository.insert(tilkjentYtelse(behandling.id))

        repository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = setOf()))
        val tilkjentYtelseUtenAndeler = repository.findByBehandlingId(behandling.id)!!
        assertThat(tilkjentYtelseUtenAndeler.andelerTilkjentYtelse).isEmpty()
    }

    @Test
    fun `Opprett og hent andeler tilkjent ytelse`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andeler = arrayOf(andelTilkjentYtelse(behandling.id), andelTilkjentYtelse(behandling.id))
        val tilkjentYtelse = tilkjentYtelse(behandling.id, null, *andeler)

        val tilkjentYtelseId = repository.insert(tilkjentYtelse).id

        val hentetTilkjentYtelse = repository.findByIdOrNull(tilkjentYtelseId)!!
        assertThat(hentetTilkjentYtelse.andelerTilkjentYtelse.size).isEqualTo(2)
    }

    @Test
    fun `Finn tilkjent ytelse på behandlingId`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val tilkjentYtelse = tilkjentYtelse(behandling.id)
        val lagretTilkjentYtelse = repository.insert(tilkjentYtelse)

        val hentetTilkjentYtelse = repository.findByBehandlingId(behandling.id)

        assertThat(hentetTilkjentYtelse).isEqualTo(lagretTilkjentYtelse)
    }
}
