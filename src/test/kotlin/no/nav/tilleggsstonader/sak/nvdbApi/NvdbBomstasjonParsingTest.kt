package no.nav.tilleggsstonader.sak.nvdbApi

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class NvdbBomstasjonParsingTest {
    @Test
    fun `tilDomene parser POINT Z korrekt - lat er første verdi, lng er andre verdi`() {
        val objekt =
            NvdbObjekt(
                id = 1L,
                lokasjon =
                    NvdbLokasjon(
                        geometri = NvdbGeometri(wkt = "POINT Z (59.9231 10.7555 12.5)", srid = 4326),
                    ),
                egenskaper =
                    listOf(
                        NvdbEgenskaper(
                            id = 1,
                            navn = "Navn bomstasjon",
                            verdi = "Test Stasjon",
                            egenskapstype = "string",
                        ),
                    ),
            )

        val result = objekt.tilDomene()

        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo(1L)
        assertThat(result.lat).isEqualTo(59.9231)
        assertThat(result.lng).isEqualTo(10.7555)
    }

    @Test
    fun `tilDomene parser POINT uten Z korrekt`() {
        val objekt =
            NvdbObjekt(
                id = 2L,
                lokasjon =
                    NvdbLokasjon(
                        geometri = NvdbGeometri(wkt = "POINT (60.41800606 5.3129743)", srid = 4326),
                    ),
                egenskaper =
                    listOf(
                        NvdbEgenskaper(
                            id = 1,
                            navn = "Navn bomstasjon",
                            verdi = "Bergen Stasjon",
                            egenskapstype = "string",
                        ),
                    ),
            )

        val result = objekt.tilDomene()

        assertThat(result).isNotNull
        assertThat(result!!.lat).isEqualTo(60.41800606)
        assertThat(result.lng).isEqualTo(5.3129743)
    }

    @Test
    fun `tilDomene returnerer null ved manglende lokasjon`() {
        val objekt = NvdbObjekt(id = 3L, lokasjon = null, egenskaper = emptyList())

        val result = objekt.tilDomene()
        assertThat(result).isNull()
    }

    @Test
    fun `tilDomene returnerer null når egenskaper er null`() {
        val objekt =
            NvdbObjekt(
                id = 4L,
                lokasjon = NvdbLokasjon(geometri = NvdbGeometri(wkt = "POINT (59.9231 10.7555)", srid = 4326)),
                egenskaper = null,
            )

        assertThat(objekt.tilDomene()).isNull()
    }

    @Test
    fun `tilDomene returnerer null for ugyldig WKT-format`() {
        val objekt =
            NvdbObjekt(
                id = 7L,
                lokasjon = NvdbLokasjon(geometri = NvdbGeometri(wkt = "UGYLDIG_WKT", srid = 4326)),
                egenskaper =
                    listOf(
                        NvdbEgenskaper(id = 1, navn = "Navn bomstasjon", verdi = "Test", egenskapstype = "string"),
                    ),
            )

        assertThat(objekt.tilDomene()).isNull()
    }

    @Test
    fun `deserialiserer NVDB v4 JSON-respons korrekt`() {
        val json = FileUtil.readFile("no/nav/tilleggsstonader/sak/nvdb/nvdb_bomstasjoner.json")
        val response = jsonMapper.readValue<NvdbBomstasjonResponse>(json)

        assertThat(response.objekter.size).isEqualTo(3)
        assertThat(response.metadata.neste?.start).isEqualTo("1027073034:1")

        val domeneObjekter = response.objekter.mapNotNull { it.tilDomene() }
        assertThat(domeneObjekter).hasSize(3)

        // Verifiser kjent stasjon fra fixture (første Bergen-stasjon)
        val bergenStasjon = domeneObjekter.first { it.id == 82443541L }
        assertThat(bergenStasjon.lat).isEqualTo(60.41800606)
        assertThat(bergenStasjon.lng).isEqualTo(5.3129743)
    }
}
