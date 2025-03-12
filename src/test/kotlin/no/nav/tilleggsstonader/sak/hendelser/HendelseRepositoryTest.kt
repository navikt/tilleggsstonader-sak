package no.nav.tilleggsstonader.sak.hendelser

import no.nav.tilleggsstonader.sak.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class HendelseRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var hendelseRepository: HendelseRepository

    @Test
    fun `skal kunne lagre og hente hendelse`() {
        val id = UUID.randomUUID().toString()
        val hendelse = Hendelse(TypeHendelse.JOURNALPOST, id = id)
        hendelseRepository.insert(hendelse)

        assertThat(hendelseRepository.findAll().single()).isEqualTo(hendelse)
    }

    @Test
    fun `skal kunne lagre og hente hendelse med metadata`() {
        val id = UUID.randomUUID().toString()
        val hendelse = Hendelse(TypeHendelse.JOURNALPOST, id = id, metadata = mapOf("journalpostId" to "123"))
        hendelseRepository.insert(hendelse)

        assertThat(
            hendelseRepository
                .findAll()
                .single()
                .metadata
                ?.json,
        ).isEqualTo("""{"journalpostId": "123"}""")
    }

    @Nested
    inner class ExistsByTypeAndId {
        @Test
        fun `skal finne hendelse på type og id`() {
            val id = UUID.randomUUID().toString()
            val type = TypeHendelse.JOURNALPOST
            val hendelse = Hendelse(type, id = id)
            hendelseRepository.insert(hendelse)

            assertThat(hendelseRepository.existsByTypeAndId(type, id = id)).isTrue()
            assertThat(hendelseRepository.existsByTypeAndId(type, id = UUID.randomUUID().toString())).isFalse()
        }
    }
}
