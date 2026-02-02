package no.nav.tilleggsstonader.sak.googlemaps

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.googlemaps.dto.KollektivDetaljerDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.LokasjonDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.OperatørDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.ReisedataDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.RuteDto
import no.nav.tilleggsstonader.sak.googlemaps.dto.StrekningDto
import no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.GooglePlaceDetailsClient
import no.nav.tilleggsstonader.sak.googlemaps.placeDetailsApi.PlaceDetailsResponse
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Address
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.GoogleRoutesClient
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.LinjeType
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Polyline
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.Reisetype
import no.nav.tilleggsstonader.sak.googlemaps.routesApi.RuteResponse
import no.nav.tilleggsstonader.sak.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class GooglemapsServiceTest {
    val googleRoutesClient = mockk<GoogleRoutesClient>()
    val googlePlaceDetailsClient = mockk<GooglePlaceDetailsClient>()
    val googlemapsService =
        GooglemapsService(
            googleRoutesClient = googleRoutesClient,
            googlePlaceDetailsClient = googlePlaceDetailsClient,
        )

    val fraAdresse = "Myravegen 2, 6360 Åfarnes"
    val fraId = "ChIJtS1aVfb4E0YRRJobI6t2Mdw"

    val tilAdresse = "Veøy Church, Fv64 40, 6456 Skåla"
    val tilId = "ChIJ6fHTti_5E0YRJhSJLbVl_Rk"

    @BeforeEach
    fun setUp() {
        every { googlePlaceDetailsClient.finnStedDetaljer(fraId) } returns
            PlaceDetailsResponse(
                id = "testId",
                formattedAddress = "Myravegen 2, 6360 Åfarnes, Norway",
                displayName = null,
            )
        every { googlePlaceDetailsClient.finnStedDetaljer(tilId) } returns
            PlaceDetailsResponse(
                id = "testId",
                formattedAddress = "Fv64 40, 6456 Skåla, Norway",
                displayName = null,
            )
    }

    @Test
    fun `skal hente kjøreruter fra google`() {
        val ruteJson = FileUtil.readFile("no/nav/tilleggsstonader/sak/googlemaps/kjørerute_response.json")
        val ruteResponse = jsonMapper.readValue<RuteResponse>(ruteJson)
        every { googleRoutesClient.hentRuter(any()) } returns ruteResponse

        val forventet =
            ReisedataDto(
                reiserute =
                    RuteDto(
                        polyline =
                            Polyline(
                                encodedPolyline =
                                    "{bm}Jukxl@o@jAwA^]M_AMoBs@kAg@sAyBQSYz@?PH\\rBtDLx@b@" +
                                        "v@h@ZUz@Yx@eBuBUr@MFYKIIi@{@{DcHi@gA[gAg@_DS_AACMQ}AuBaA}@[KE?[?_@RUXiErKgGpO}HzRm" +
                                        "Vxm@gYps@kn@v}AUhAGp@CpCAvB?rE@fBFtMB~CAtCA~BMnD[tEK`AQxA]zBWt@]b@A@OLUF",
                            ),
                        avstandMeter = 4693,
                        avstandUtenFerje = 1485,
                        varighetSekunder = 2407.0,
                        strekninger =
                            listOf(
                                StrekningDto(varighetSekunder = 243.0, reisetype = Reisetype.DRIVE, kollektivDetaljer = null),
                                StrekningDto(varighetSekunder = 2088.0, reisetype = Reisetype.DRIVE, kollektivDetaljer = null),
                                StrekningDto(varighetSekunder = 76.0, reisetype = Reisetype.DRIVE, kollektivDetaljer = null),
                            ),
                        startLokasjon = LokasjonDto(lat = 62.659176200000005, lng = 7.502830500000001),
                        sluttLokasjon = LokasjonDto(lat = 62.6857511, lng = 7.454786800000001),
                        startAdresse = "Myravegen 2, 6360 Åfarnes, Norway",
                        sluttAdresse = "Fv64 40, 6456 Skåla, Norway",
                    ),
            )

        assertThat(googlemapsService.hentKjøreruter(Address(address = fraAdresse), Address(tilAdresse))).isEqualTo(
            forventet,
        )
    }

    @Test
    fun `skal hente kollektivrute fra google`() {
        val ruteJson = FileUtil.readFile("no/nav/tilleggsstonader/sak/googlemaps/kollektivrute_response.json")
        val ruteResponse = jsonMapper.readValue<RuteResponse>(ruteJson)
        every { googleRoutesClient.hentRuter(any()) } returns ruteResponse

        val forventet =
            ReisedataDto(
                reiserute =
                    RuteDto(
                        polyline =
                            Polyline(
                                encodedPolyline =
                                    "{bm}Jukxl@o@jAwA^]M_AMoBs@kAg@sAyBQSYz@?PH\\rBtDLx@{Ag" +
                                        "@cGuF_A}Cc@eCYgACMQt@OUETLPMY{AwBaA}@a@K[?_@RUXqxBdrFUhAGp@EhGDb@@?AjGL~QFp@?tBI|@K" +
                                        "tEY~Ec@bE_@zBWx@_@d@QNWH",
                            ),
                        avstandMeter = 4579,
                        avstandUtenFerje = 4579,
                        varighetSekunder = 2067.0,
                        strekninger =
                            listOf(
                                StrekningDto(varighetSekunder = 631.0, reisetype = Reisetype.WALK, kollektivDetaljer = null),
                                StrekningDto(
                                    varighetSekunder = 900.0,
                                    reisetype = Reisetype.TRANSIT,
                                    kollektivDetaljer =
                                        KollektivDetaljerDto(
                                            startHoldeplass = "Åfarnes ferjekai",
                                            sluttHoldeplass = "Sølsnes ferjekai",
                                            linjeNavn = "Sølsnes-Åfarnes",
                                            linjeType = LinjeType.FERRY,
                                            operatør =
                                                listOf(
                                                    `OperatørDto`(
                                                        navn = "Møre og Romsdal fylkeskommune",
                                                        url = "https://frammr.no/",
                                                    ),
                                                ),
                                        ),
                                ),
                                StrekningDto(varighetSekunder = 534.0, reisetype = Reisetype.WALK, kollektivDetaljer = null),
                            ),
                        startLokasjon = LokasjonDto(lat = 62.659176200000005, lng = 7.502830500000001),
                        sluttLokasjon = LokasjonDto(lat = 62.685745999999995, lng = 7.454721900000001),
                        startAdresse = "Myravegen 2, 6360 Åfarnes, Norway",
                        sluttAdresse = "Fv64 40, 6456 Skåla, Norway",
                    ),
            )

        assertThat(googlemapsService.hentKjøreruter(Address(address = fraAdresse), Address(tilAdresse))).isEqualTo(
            forventet,
        )
    }
}
