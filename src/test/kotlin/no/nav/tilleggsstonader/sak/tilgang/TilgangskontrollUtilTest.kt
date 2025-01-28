package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.UGRADERT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TilgangskontrollUtilTest {
    @Test
    fun `høyesteGraderingen skal returnere høyeste gradering fra barn`() {
        val person =
            lagPersonMedRelasjoner(
                adressebeskyttelse = UGRADERT,
                barn = STRENGT_FORTROLIG,
            )

        assertThat(TilgangskontrollUtil.høyesteGraderingen(person)).isEqualTo(STRENGT_FORTROLIG)
    }

    @Test
    fun `STRENGT_FORTROLIG trumfer FORTROLIG`() {
        val person =
            lagPersonMedRelasjoner(
                adressebeskyttelse = FORTROLIG,
                barn = STRENGT_FORTROLIG,
            )

        assertThat(TilgangskontrollUtil.høyesteGraderingen(person)).isEqualTo(STRENGT_FORTROLIG)
    }

    @Test
    fun `høyesteGraderingen skal returnere høyeste gradering fra hovedperson`() {
        val person =
            lagPersonMedRelasjoner(
                adressebeskyttelse = STRENGT_FORTROLIG,
                barn = UGRADERT,
            )

        assertThat(TilgangskontrollUtil.høyesteGraderingen(person)).isEqualTo(STRENGT_FORTROLIG)
    }

    @Test
    fun `høyesteGraderingen skal returnere ugradert `() {
        val person =
            lagPersonMedRelasjoner(
                adressebeskyttelse = UGRADERT,
                barn = UGRADERT,
            )

        assertThat(TilgangskontrollUtil.høyesteGraderingen(person)).isEqualTo(UGRADERT)
    }

    @Test
    fun `høyesteGraderingen skal returnere strengeste gradering hvis mange`() {
        val person =
            lagPersonMedRelasjoner(
                adressebeskyttelse = UGRADERT,
                barn = STRENGT_FORTROLIG_UTLAND,
            )

        assertThat(TilgangskontrollUtil.høyesteGraderingen(person)).isEqualTo(STRENGT_FORTROLIG_UTLAND)
    }

    private fun lagPersonMedRelasjoner(
        adressebeskyttelse: AdressebeskyttelseGradering = UGRADERT,
        barn: AdressebeskyttelseGradering = UGRADERT,
    ): PersonMedRelasjoner =
        PersonMedRelasjoner(
            personIdent = "",
            adressebeskyttelse = adressebeskyttelse,
            barn = lagPersonMedBeskyttelse(barn, "barn"),
        )

    private fun lagPersonMedBeskyttelse(
        gradering: AdressebeskyttelseGradering?,
        personIdent: String,
    ) = gradering?.let { listOf(PersonMedAdresseBeskyttelse(personIdent, it)) } ?: emptyList()
}
