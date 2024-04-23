package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class AndelTilkjentYtelseRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Test
    fun `skal kunne oppdatere en enkelt andel`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andel1 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 100,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        val andel2 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 200,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = setOf(andel1, andel2),
            ),
        )

        val iverksattAndel2 = andel2.copy(
            statusIverksetting = StatusIverksetting.SENDT,
            iverksetting = Iverksetting(UUID.randomUUID(), SporbarUtils.now()),
        )
        andelTilkjentYtelseRepository.update(iverksattAndel2)

        val tyFraDb = tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!
        assertThat(tyFraDb.andelerTilkjentYtelse.toList())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "endretTid")
            .containsExactlyInAnyOrder(andel1, iverksattAndel2.copy(version = 1))

        val andel2FraDb = andelTilkjentYtelseRepository.findByIdOrThrow(andel2.id)
        assertThat(andel2FraDb)
            .usingRecursiveComparison()
            .ignoringFields("endretTid")
            .isEqualTo(iverksattAndel2.copy(version = 1))
    }

    @Test
    fun `skal oppdatere endret tidspunkt vid oppdateringer`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val andel1 = andelTilkjentYtelse(
            kildeBehandlingId = behandling.id,
            beløp = 100,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 1),
        )
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandling.id,
                andelerTilkjentYtelse = setOf(andel1),
            ),
        )

        jdbcTemplate.update(
            "UPDATE andel_tilkjent_ytelse SET endret_tid = :tid",
            mapOf("tid" to LocalDate.of(2023, 1, 1).atStartOfDay().truncatedTo(ChronoUnit.MILLIS)),
        )

        val andel = andelTilkjentYtelseRepository.findByIdOrThrow(andel1.id)
        andelTilkjentYtelseRepository.update(andel.copy(statusIverksetting = StatusIverksetting.OK))
        val andelEtterOppdatering = andelTilkjentYtelseRepository.findByIdOrThrow(andel1.id)

        assertThat(andel.endretTid.toLocalDate()).isEqualTo(LocalDate.of(2023, 1, 1))
        assertThat(andelEtterOppdatering.endretTid.toLocalDate()).isEqualTo(LocalDate.now())
    }

    @Nested
    inner class FinnBehandlingerForIverksetting {

        @Test
        fun `skal ikke finne andeler for en behandling som ikke er iverksatt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            opprettTilkjentYtelseMedEnAndel(behandling)

            val behandlinger = andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(LocalDate.now())

            assertThat(behandlinger).isEmpty()
        }

        @Test
        fun `skal ikke finne behandlinger der alle andeler allerede er iverksatt`() {
            val behandling1 = behandling(status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling1)
            opprettTilkjentYtelseMedEnAndel(behandling, StatusIverksetting.OK)

            val behandlinger = andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(LocalDate.now())

            assertThat(behandlinger).isEmpty()
        }

        @Test
        fun `skal finne behandlinger med andeler som ikke har blitt iverksatt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )
            val fagsak2 = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("16"))))
            val behandling2 = testoppsettService.lagre(
                behandling(
                    fagsak2,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.OPPHØRT,
                ),
            )

            opprettTilkjentYtelseMedEnAndel(behandling)
            opprettTilkjentYtelseMedEnAndel(behandling2)

            val behandlinger = andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(LocalDate.now())

            assertThat(behandlinger).containsExactlyInAnyOrder(behandling.id, behandling2.id)
        }

        private fun opprettTilkjentYtelseMedEnAndel(
            behandling: Behandling,
            statusIverksetting: StatusIverksetting = StatusIverksetting.UBEHANDLET,
        ) {
            val andel1 = andelTilkjentYtelse(
                kildeBehandlingId = behandling.id,
                beløp = 100,
                fom = LocalDate.of(2023, 1, 1),
                tom = LocalDate.of(2023, 1, 1),
                statusIverksetting = statusIverksetting,
            )
            tilkjentYtelseRepository.insert(
                TilkjentYtelse(
                    behandlingId = behandling.id,
                    andelerTilkjentYtelse = setOf(andel1),
                ),
            )
        }
    }
}
