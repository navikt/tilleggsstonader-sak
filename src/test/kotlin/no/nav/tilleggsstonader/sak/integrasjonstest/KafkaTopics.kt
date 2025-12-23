package no.nav.tilleggsstonader.sak.integrasjonstest

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "topics")
data class KafkaTopics(
    val journalhendelser: String,
    val pdlPersonhendelser: String,
    val oppgavehendelser: String,
    val utbetaling: String,
    val utbetalingStatus: String,
    val dvhBehandling: String,
)
