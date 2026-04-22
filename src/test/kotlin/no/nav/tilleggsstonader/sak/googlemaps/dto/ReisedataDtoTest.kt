package no.nav.tilleggsstonader.sak.googlemaps.dto

import no.nav.tilleggsstonader.sak.googlemaps.KollektivDetaljer
import no.nav.tilleggsstonader.sak.googlemaps.Lokasjon
import no.nav.tilleggsstonader.sak.googlemaps.Operatør
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.LinjeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ReisedataDtoTest {
    private val startLokasjon = Lokasjon(lat = 59.9139, lng = 10.7522)
    private val sluttLokasjon = Lokasjon(lat = 59.9200, lng = 10.7600)

    private fun kollektivDetaljer(
        operatørNavn: String,
        operatørUrl: String = "https://ruter.no/fa-hjelp-og-kontakt/kontakt-oss",
    ) = KollektivDetaljer(
        startHoldeplass = "Jernbanetorget",
        startHoldeplassLokasjon = startLokasjon,
        sluttHoldeplass = "Nationaltheatret",
        sluttHoldeplassLokasjon = sluttLokasjon,
        linjeNavn = "T1",
        linjeType = LinjeType.SUBWAY,
        operatør = listOf(Operatør(navn = operatørNavn, url = operatørUrl)),
    )

    @Nested
    inner class OperatørUrlForRuter {
        @Test
        fun `Ruter-operatør får overstyrt default Ruter-url med reise-url`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            assertThat(url).startsWith("https://reise.ruter.no/")
            assertThat(url).isNotEqualTo("https://ruter.no/fa-hjelp-og-kontakt/kontakt-oss")
        }

        @Test
        fun `Ruter-url inneholder forventet fromName med holdeplassnavn`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            val dekodetUrl = URLDecoder.decode(url, StandardCharsets.UTF_8)
            assertThat(dekodetUrl).contains("fromName=__Jernbanetorget__")
        }

        @Test
        fun `Ruter-url inneholder forventet toName med holdeplassnavn`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            val dekodetUrl = URLDecoder.decode(url, StandardCharsets.UTF_8)
            assertThat(dekodetUrl).contains("toName=__Nationaltheatret__")
        }

        @Test
        fun `Ruter-url inneholder koordinater for start- og sluttlokasjon`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            assertThat(url).contains("fromLatitude=${startLokasjon.lat}")
            assertThat(url).contains("fromLongitude=${startLokasjon.lng}")
            assertThat(url).contains("toLatitude=${sluttLokasjon.lat}")
            assertThat(url).contains("toLongitude=${sluttLokasjon.lng}")
        }

        @Test
        fun `Ruter-url inneholder midtpunkt-koordinater mellom start og slutt`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            val forventetMapLat = (startLokasjon.lat + sluttLokasjon.lat) / 2
            val forventetMapLng = (startLokasjon.lng + sluttLokasjon.lng) / 2
            assertThat(url).contains("mapLatitude=$forventetMapLat")
            assertThat(url).contains("mapLongitude=$forventetMapLng")
        }

        @Test
        fun `Ruter-url inneholder forventet stoptype og zoom`() {
            val detaljer = kollektivDetaljer("Ruter")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            val dekodetUrl = URLDecoder.decode(url, StandardCharsets.UTF_8)
            assertThat(dekodetUrl).contains("fromType=__STOP_PLACE__")
            assertThat(dekodetUrl).contains("toType=__STOP_PLACE__")
            assertThat(url).contains("mapZoom=13")
        }

        @Test
        fun `holdeplassnavn med spesialtegn blir URL-enkodet i Ruter-url`() {
            val detaljer =
                KollektivDetaljer(
                    startHoldeplass = "Ås stasjon",
                    startHoldeplassLokasjon = startLokasjon,
                    sluttHoldeplass = "Østerås",
                    sluttHoldeplassLokasjon = sluttLokasjon,
                    linjeNavn = "L1",
                    linjeType = LinjeType.SUBWAY,
                    operatør = listOf(Operatør(navn = "Ruter", url = "")),
                )

            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            // Rå URL skal inneholde enkodede tegn
            assertThat(url).doesNotContain("Ås stasjon")
            assertThat(url).doesNotContain("Østerås")
            // Dekoded skal inneholde originale verdier
            val dekodetUrl = URLDecoder.decode(url, StandardCharsets.UTF_8)
            assertThat(dekodetUrl).contains("Ås stasjon")
            assertThat(dekodetUrl).contains("Østerås")
        }
    }

    @Nested
    inner class OperatørUrlForAndreOperatører {
        @Test
        fun `ikke-Ruter-operatør beholder sin originale url`() {
            val originalUrl = "https://www.vy.no/tog"
            val detaljer = kollektivDetaljer("Vy", operatørUrl = originalUrl)
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            assertThat(url).isEqualTo(originalUrl)
        }

        @Test
        fun `ikke-Ruter-operatør url peker ikke til reise ruter no`() {
            val detaljer = kollektivDetaljer("Vy", operatørUrl = "https://www.vy.no")
            val url =
                detaljer
                    .tilDto()
                    .operatør
                    .single()
                    .url

            assertThat(url).doesNotStartWith("https://reise.ruter.no/")
        }
    }
}
