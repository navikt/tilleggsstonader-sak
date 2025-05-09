package no.nav.tilleggsstonader.sak.hendelser.personhendelse

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil.lagConsumerRecord
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHendelse
import no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall.DødsfallHåndterer
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate
import java.util.UUID

class PersonhendelseKafkaListenerTest {
    private val ack = mockk<Acknowledgment>()
    private val dødsfallHåndterer = mockk<DødsfallHåndterer>()
    private val personhendelseKafkaListener = PersonhendelseKafkaListener(dødsfallHåndterer)

    @Test
    fun `motta hendelser om dødsfall og annet, verifiser dødsfall blir håndtert`() {
        every { dødsfallHåndterer.håndterDødsfall(any()) } just runs
        every { ack.acknowledge() } just runs

        val dødsfallHendelse =
            Personhendelse().apply {
                personidenter = listOf("12345678901")
                doedsfall =
                    Doedsfall().apply {
                        doedsdato = LocalDate.now()
                    }
            }

        // Vi har ikke definert andre hendelser i vår avro, så objektet vil være ganske tomt
        val annenHendelse =
            Personhendelse().apply {
                personidenter = listOf("10987654321")
            }

        personhendelseKafkaListener.listen(
            listOf(
                lagConsumerRecord(UUID.randomUUID().toString(), dødsfallHendelse),
                lagConsumerRecord(UUID.randomUUID().toString(), annenHendelse),
            ),
            ack,
        )

        val forventetDødsfallHendelse = DødsfallHendelse(dødsfallHendelse.doedsfall.doedsdato, dødsfallHendelse.personidenter.toSet())
        verify(exactly = 1) { dødsfallHåndterer.håndterDødsfall(listOf(forventetDødsfallHendelse)) }
        verify(exactly = 1) { ack.acknowledge() }
    }
}
