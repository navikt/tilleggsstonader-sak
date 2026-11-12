package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.util.KPostgreSQLContainer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TotalReiseavstandMigreringTest {
    private val postgres = KPostgreSQLContainer("postgres:17")
    private lateinit var connection: Connection
    private val migrering =
        requireNotNull(javaClass.getResource("/db/migration/V157__daglig_reise_total_reiseavstand.sql")).readText()

    @BeforeAll
    fun startPostgres() {
        postgres.start()
    }

    @AfterAll
    fun stoppPostgres() {
        postgres.stop()
    }

    @BeforeEach
    fun opprettTabeller() {
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        connection.autoCommit = false
        connection.createStatement().use {
            it.execute(
                """
                CREATE TEMP TABLE vilkar (id INTEGER PRIMARY KEY, fakta JSON, endret_av TEXT DEFAULT 'saksbehandler');
                CREATE TEMP TABLE vedtak (id INTEGER PRIMARY KEY, data JSONB, endret_av TEXT DEFAULT 'saksbehandler');
                """.trimIndent(),
            )
        }
        connection.commit()
    }

    @AfterEach
    fun lukkTilkobling() {
        connection.close()
    }

    @Test
    fun `konverterer vilkår og historiske rammer uten å endre beløp eller rekkefølge`() {
        val fakta = """{"type":"DAGLIG_REISE_PRIVAT_BIL","reiseavstandEnVei":10.5,"faktaDelperioder":[]}"""
        val vedtak =
            """
            {"type":"INNVILGELSE_DAGLIG_REISE","status":"FERDIGSTILT","belop":123,
             "rammevedtakPrivatBil":{"reiser":[
              {"reiseId":"reise-1","grunnlag":{"reiseavstandEnVei":10.5,"dagsatsUtenParkering":61.74}},
              {"reiseId":"reise-2","grunnlag":{"reiseavstandEnVei":12,"dagsatsUtenParkering":70.56}}]}}
            """.trimIndent()
        lagre("vilkar", 1, fakta)
        lagre("vedtak", 1, vedtak)
        lagre("vedtak", 2, vedtak.replace("INNVILGELSE", "OPPHØR"))
        connection.commit()

        migrer()

        assertJson("vilkar", 1, fakta.replace("\"reiseavstandEnVei\":10.5", "\"totalReiseavstand\":21"))
        val forventet =
            vedtak
                .replace("\"reiseavstandEnVei\":10.5", "\"totalReiseavstand\":21")
                .replace("\"reiseavstandEnVei\":12", "\"totalReiseavstand\":24")
        assertJson("vedtak", 1, forventet)
        assertJson("vedtak", 2, forventet.replace("INNVILGELSE", "OPPHØR"))

        migrer()
        assertJson("vedtak", 1, forventet)
        assertJson("vilkar", 1, fakta.replace("\"reiseavstandEnVei\":10.5", "\"totalReiseavstand\":21"))
    }

    @Test
    fun `beholder andre ytelser og tomme eller manglende rammer`() {
        val offentligTransport = """{"type":"DAGLIG_REISE_OFFENTLIG_TRANSPORT","prisEnkelbillett":40}"""
        val samling = """{"type":"REISE_TIL_SAMLING_PRIVAT_BIL","reiseavstand":40}"""
        val tomRamme = """{"type":"INNVILGELSE_DAGLIG_REISE","rammevedtakPrivatBil":{"reiser":[]}}"""
        val nullRamme = """{"type":"INNVILGELSE_DAGLIG_REISE","rammevedtakPrivatBil":null}"""
        val ingenRamme = """{"type":"INNVILGELSE_DAGLIG_REISE","beregningsresultat":{"belop":100}}"""
        lagre("vilkar", 1, offentligTransport)
        lagre("vilkar", 2, samling)
        lagre("vedtak", 1, tomRamme)
        lagre("vedtak", 2, nullRamme)
        lagre("vedtak", 3, ingenRamme)
        connection.commit()

        migrer()

        assertJson("vilkar", 1, offentligTransport)
        assertJson("vilkar", 2, samling)
        assertJson("vedtak", 1, tomRamme)
        assertJson("vedtak", 2, nullRamme)
        assertJson("vedtak", 3, ingenRamme)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"reiseavstandEnVei":null}""",
            """{"reiseavstandEnVei":"10"}""",
            """{"reiseavstandEnVei":0}""",
            """{"reiseavstandEnVei":10,"totalReiseavstand":40}""",
            """{}""",
        ],
    )
    fun `ugyldig ramme avbryter hele migreringen`(grunnlag: String) {
        val fakta = """{"type":"DAGLIG_REISE_PRIVAT_BIL","reiseavstandEnVei":10}"""
        lagre("vilkar", 1, fakta)
        lagre(
            "vedtak",
            1,
            """{"type":"INNVILGELSE_DAGLIG_REISE","rammevedtakPrivatBil":{"reiser":[{"grunnlag":$grunnlag}]}}""",
        )
        connection.commit()

        assertThatThrownBy { migrer() }.isInstanceOf(SQLException::class.java)
        connection.rollback()

        assertJson("vilkar", 1, fakta)
    }

    private fun migrer() {
        connection.createStatement().use { it.execute(migrering) }
    }

    private fun lagre(
        tabell: String,
        id: Int,
        json: String,
    ) {
        val sql =
            when (tabell) {
                "vilkar" -> "INSERT INTO vilkar (id, fakta) VALUES (?, ?::json)"
                "vedtak" -> "INSERT INTO vedtak (id, data) VALUES (?, ?::jsonb)"
                else -> error("Ukjent testtabell")
            }
        connection.prepareStatement(sql).use {
            it.setInt(1, id)
            it.setString(2, json)
            it.executeUpdate()
        }
    }

    private fun assertJson(
        tabell: String,
        id: Int,
        forventet: String,
    ) {
        val sql =
            when (tabell) {
                "vilkar" -> "SELECT fakta::jsonb = ?::jsonb, endret_av FROM vilkar WHERE id = ?"
                "vedtak" -> "SELECT data = ?::jsonb, endret_av FROM vedtak WHERE id = ?"
                else -> error("Ukjent testtabell")
            }
        connection.prepareStatement(sql).use {
            it.setString(1, forventet)
            it.setInt(2, id)
            it.executeQuery().use { result ->
                assertThat(result.next()).isTrue()
                assertThat(result.getBoolean(1)).isTrue()
                assertThat(result.getString(2)).isEqualTo("saksbehandler")
            }
        }
    }
}
