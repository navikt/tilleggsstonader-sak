package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Adresse
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Personopplysninger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingFaktaUtilsTest {
    @Test
    fun `skal mappe personopplysninger`() {
        val personopplysninger =
            Personopplysninger(
                adresse =
                    Adresse(
                        gyldigFraOgMed = LocalDate.of(2025, 1, 1),
                        adresse = "Rundgata",
                        postnummer = "1234",
                        poststed = "Oslo",
                        landkode = "no",
                    ),
                fødselsdatoPersonUtenFødselsnummer = "2025-01-01",
            )

        val mappetPersonopplysninger = mapPersonopplysninger(personopplysninger)

        assertThat(mappetPersonopplysninger.søknadsgrunnlag!!.adresse).isEqualTo("Rundgata, 1234, Oslo")
        assertThat(mappetPersonopplysninger.søknadsgrunnlag.fødselsdatPersonUtenPersonnummer).isEqualTo("2025-01-01")
    }

    @Test
    fun `skal mappe personopplysninger hvor alle felter er null`() {
        val personopplysninger =
            Personopplysninger(
                adresse =
                    Adresse(
                        gyldigFraOgMed = null,
                        adresse = null,
                        postnummer = null,
                        poststed = null,
                        landkode = null,
                    ),
                fødselsdatoPersonUtenFødselsnummer = null,
            )

        val mappetPersonopplysninger = mapPersonopplysninger(personopplysninger)

        assertThat(mappetPersonopplysninger.søknadsgrunnlag!!.adresse).isNull()
        assertThat(mappetPersonopplysninger.søknadsgrunnlag.fødselsdatPersonUtenPersonnummer).isNull()
    }
}
