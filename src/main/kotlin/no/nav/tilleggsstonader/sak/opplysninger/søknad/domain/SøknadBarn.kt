package no.nav.tilleggsstonader.sak.opplysninger.søknad.domain

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("soknad_barn")
data class SøknadBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val ident: String,
    val data: BarnMedBarnepass,
)

data class BarnMedBarnepass(
    val type: TypeBarnepass,
    val startetIFemte: JaNei?,
    val årsak: ÅrsakBarnepass?,
)
