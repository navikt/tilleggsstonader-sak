package no.nav.tilleggsstonader.sak.opplysninger.søknad.domain

import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.TypeBarnepass
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.ÅrsakBarnepass
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.Utgifter as UtgifterKontrakter

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
    val utgifter: Utgifter?,
    val årsak: ÅrsakBarnepass?,
)

data class Utgifter(
    val harUtgifterTilPassHelePerioden: JaNei,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

fun UtgifterKontrakter?.tilDomene(): Utgifter? =
    this?.let {
        Utgifter(
            harUtgifterTilPassHelePerioden = it.harUtgifterTilPass.verdi,
            fom = it.fom?.verdi,
            tom = it.tom?.verdi,
        )
    }
