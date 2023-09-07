package no.nav.tilleggsstonader.sak.opplysninger.søknad.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("soknad_barn")
data class SøknadBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val navn: String,
    @Column("fodselsnummer")
    val fødselsnummer: String,
)
