package no.nav.tilleggsstonader.sak.nvdbApi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test

class BomstasjonUtilsTest {
    // Koordinater fra Elstrømbrua-scenariet.
    // Bomstasjonen er ~117m fra nærmeste rutepunkt, men under 1m fra linjestykket mellom p1 og p2.
    private val segmentP1 = GeoPoint(lat = 59.19511, lng = 9.58613)
    private val segmentP2 = GeoPoint(lat = 59.19690, lng = 9.58346)
    private val elstrombru =
        NvdbBomstasjon(id = 732290254, navn = "Elstrømbrua", takstLitenBilRush = 29.0, lat = 59.19610052, lng = 9.58466914)

    @Test
    fun `gir liten avstand når bomstasjonen treffer midt på et linjestykke`() {
        val avstand = beregnKortestAvstandFraPunktTilLinjestykke(elstrombru.lat, elstrombru.lng, segmentP1, segmentP2)

        assertThat(avstand).isLessThan(5.0)
    }

    @Test
    fun `avstand til linjestykke er mye kortere enn avstand til nærmeste endepunkt`() {
        val segmentAvstand = beregnKortestAvstandFraPunktTilLinjestykke(elstrombru.lat, elstrombru.lng, segmentP1, segmentP2)
        val punktAvstandP1 = beregnAvstandMellomPunkterMeter(segmentP1.lat, segmentP1.lng, elstrombru.lat, elstrombru.lng)
        val punktAvstandP2 = beregnAvstandMellomPunkterMeter(segmentP2.lat, segmentP2.lng, elstrombru.lat, elstrombru.lng)

        assertThat(minOf(punktAvstandP1, punktAvstandP2)).isGreaterThan(100.0)
        assertThat(segmentAvstand).isLessThan(5.0)
    }

    @Test
    fun `gir samme avstand som til endepunkt når bomstasjonen er utenfor stykket`() {
        // Punktet er sør-øst for p1, utenfor stykket — nærmeste punkt er p1
        val utenforSegmentet = GeoPoint(lat = 59.19400, lng = 9.58700)
        val segmentAvstand = beregnKortestAvstandFraPunktTilLinjestykke(utenforSegmentet.lat, utenforSegmentet.lng, segmentP1, segmentP2)
        val punktAvstand = beregnAvstandMellomPunkterMeter(segmentP1.lat, segmentP1.lng, utenforSegmentet.lat, utenforSegmentet.lng)

        assertThat(segmentAvstand).isCloseTo(punktAvstand, offset(0.01))
    }

    @Test
    fun `gir riktig avstand når p1 og p2 er samme punkt`() {
        val avstand = beregnKortestAvstandFraPunktTilLinjestykke(elstrombru.lat, elstrombru.lng, segmentP1, segmentP1)
        val forventet = beregnAvstandMellomPunkterMeter(segmentP1.lat, segmentP1.lng, elstrombru.lat, elstrombru.lng)

        assertThat(avstand).isCloseTo(forventet, offset(0.01))
    }

    @Test
    fun `finner riktig minimumsavstand over alle linjestykker i ruten`() {
        val punkter =
            listOf(
                GeoPoint(lat = 59.19400, lng = 9.59000),
                segmentP1,
                segmentP2,
                GeoPoint(lat = 59.19800, lng = 9.58200),
            )

        val avstand = beregnKortestAvstandFraPunktTilRute(elstrombru.lat, elstrombru.lng, punkter)

        assertThat(avstand).isLessThan(5.0)
    }

    @Test
    fun `returnerer MAX_VALUE for tom ruteliste`() {
        val avstand = beregnKortestAvstandFraPunktTilRute(elstrombru.lat, elstrombru.lng, emptyList())

        assertThat(avstand).isEqualTo(Double.MAX_VALUE)
    }
}
