package no.nav.tilleggsstonader.sak.opplysninger.pdl

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPersonMedRelasjoner
import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.PersonMedAdresseBeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Adressebeskyttelse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.Familierelasjonsrolle
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.ForelderBarnRelasjon
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.metadataGjeldende
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlPersonKort
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class PersonServiceTest {
    val pdlClient = mockk<PdlClient>()
    val service = PersonService(pdlClient, ConcurrentMapCacheManager())

    val søkerIdent = "søker"
    val annenForeldreIdent = "annenForeldre"
    val barnIdent = "barnIdent"

    val forelderBarnRelasjon =
        ForelderBarnRelasjon(
            relatertPersonsIdent = barnIdent,
            relatertPersonsRolle = Familierelasjonsrolle.BARN,
            minRolleForPerson = Familierelasjonsrolle.MOR,
        )

    @Nested
    inner class HentAdressebeskyttelseForPersonOgRelasjoner {
        @Test
        fun `skal hente barn og andre foreldre til søker`() {
            every { pdlClient.hentSøker(søkerIdent) } returns
                pdlSøker(
                    adressebeskyttelse = adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND),
                    forelderBarnRelasjon = listOf(forelderBarnRelasjon),
                )
            every { pdlClient.hentBarn(listOf(barnIdent)) } returns
                mapOf(barnIdent to pdlBarn(AdressebeskyttelseGradering.FORTROLIG))
            val adressebeskyttelse = adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            every { pdlClient.hentPersonKortBolk(listOf(annenForeldreIdent)) } returns
                mapOf(annenForeldreIdent to pdlPersonKort(adressebeskyttelse = adressebeskyttelse))

            val adressebeskyttelser = service.hentAdressebeskyttelseForPersonOgRelasjoner(søkerIdent)

            val søker = PersonMedAdresseBeskyttelse(søkerIdent, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND)
            val andreForeldre =
                PersonMedAdresseBeskyttelse(annenForeldreIdent, AdressebeskyttelseGradering.STRENGT_FORTROLIG)
            val forventet =
                AdressebeskyttelseForPersonMedRelasjoner(
                    søker = søker,
                    barn = listOf(PersonMedAdresseBeskyttelse(barnIdent, AdressebeskyttelseGradering.FORTROLIG)),
                    andreForeldre = listOf(andreForeldre),
                )
            assertThat(adressebeskyttelser).isEqualTo(forventet)
        }

        private fun pdlBarn(adressebeskyttelse: AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT) =
            pdlBarn(
                adressebeskyttelse = adressebeskyttelse(adressebeskyttelse),
                forelderBarnRelasjon =
                    listOf(
                        ForelderBarnRelasjon(
                            relatertPersonsIdent = søkerIdent,
                            relatertPersonsRolle = Familierelasjonsrolle.MOR,
                            minRolleForPerson = Familierelasjonsrolle.BARN,
                        ),
                        ForelderBarnRelasjon(
                            relatertPersonsIdent = annenForeldreIdent,
                            relatertPersonsRolle = Familierelasjonsrolle.FAR,
                            minRolleForPerson = Familierelasjonsrolle.BARN,
                        ),
                    ),
            )
    }

    private fun adressebeskyttelse(gradering: AdressebeskyttelseGradering) = listOf(Adressebeskyttelse(gradering, metadataGjeldende))
}
