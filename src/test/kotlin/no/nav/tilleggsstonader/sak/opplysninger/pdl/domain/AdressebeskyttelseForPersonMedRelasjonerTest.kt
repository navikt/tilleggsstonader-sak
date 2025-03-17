package no.nav.tilleggsstonader.sak.opplysninger.pdl.domain

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AdressebeskyttelseForPersonMedRelasjonerTest {
    val søkerIdent = "søker"
    val annenForeldreIdent = "annenForeldre"
    val barnIdent = "barnIdent"

    val søker = PersonMedAdresseBeskyttelse(søkerIdent, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)
    val annenForeldre =
        PersonMedAdresseBeskyttelse(annenForeldreIdent, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
    val adressebeskyttelseForPersonMedRelasjoner =
        AdressebeskyttelseForPersonMedRelasjoner(
            søker = søker,
            barn = listOf(PersonMedAdresseBeskyttelse(barnIdent, AdressebeskyttelseGradering.FORTROLIG)),
            andreForeldre = listOf(annenForeldre),
        )

    @Nested
    inner class IdenterForEgenAnsattKontroll {
        @Test
        fun `skal inneholde adressebeskyttelser til søker barn og andre foreldre`() {
            assertThat(adressebeskyttelseForPersonMedRelasjoner.identerForEgenAnsattKontroll())
                .containsExactlyInAnyOrder(søkerIdent, annenForeldreIdent)
        }
    }

    @Nested
    inner class Adressebeskyttelser {
        @Test
        fun `skal inneholde adressebeskyttelser til søker barn og andre foreldre`() {
            assertThat(adressebeskyttelseForPersonMedRelasjoner.adressebeskyttelser()).containsExactlyInAnyOrder(
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                AdressebeskyttelseGradering.FORTROLIG,
            )
        }
    }
}
