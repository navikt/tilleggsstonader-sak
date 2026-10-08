package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDomain
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FaktaFlyttingMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlytteSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class FaktaFlyttingMapperTest {
    @Test
    fun `skal bevare ufullstendige fakta gjennom domene og dto`() {
        listOf(
            FaktaFlyttingUbestemtDto(adresse = "Flytteveien 1"),
            FaktaFlyttebyråDto(
                tilbud1 = FlyttebyråTilbudDto(navn = "Flyttebyrå A"),
                adresse = "Flytteveien 1",
                erBetalingDokumentert = true,
            ),
            FaktaFlytteSelvDto(adresse = "Flytteveien 1"),
        ).forEach { fakta ->
            assertThat(fakta.tilDomain().tilDto()).isEqualTo(fakta)
        }
    }

    @Test
    fun `skal bevare kostnader og tilbud gjennom lagringsformatet`() {
        listOf(
            FaktaFlyttebyråDto(
                tilbud1 = FlyttebyråTilbudDto(navn = "Flyttebyrå A", pris = 10000),
                tilbud2 = FlyttebyråTilbudDto(navn = "Flyttebyrå B", pris = 12000),
                adresse = "Flytteveien 1",
                erBetalingDokumentert = false,
            ),
            FaktaFlytteSelvDto(
                avstandEnVei = 250,
                henger = 1500,
                bompenger = 0,
                ferge = null,
                parkering = 100,
                adresse = "Flytteveien 1",
            ),
            FaktaFlyttingUbestemtDto(adresse = "Flytteveien 1"),
        ).forEach { fakta ->
            val domain = fakta.tilDomain()
            val json = jsonMapper.writerFor(VilkårFakta::class.java).writeValueAsString(domain)

            assertThat(jsonMapper.readValue<VilkårFakta>(json)).isEqualTo(domain)
            assertThat(jsonMapper.readValue<FaktaFlyttingDto>(json)).isEqualTo(fakta)
            assertThat(json).contains("\"adresse\":\"Flytteveien 1\"")
            assertThat(json).doesNotContain("reiseId", "aktivitetId")
        }
    }

    @Test
    fun `skal bevare eksisterende jsonformat for reisefakta`() {
        val json =
            """
            {
              "type": "DAGLIG_REISE_UBESTEMT",
              "reiseId": "00000000-0000-0000-0000-000000000001",
              "adresse": "Tiltaksveien 1"
            }
            """.trimIndent()
        val fakta = jsonMapper.readValue<VilkårFakta>(json)

        assertThat(fakta).isEqualTo(
            FaktaDagligReiseUbestemt(
                reiseId = ReiseId.fromString("00000000-0000-0000-0000-000000000001"),
                adresse = "Tiltaksveien 1",
            ),
        )
        val serialisert = jsonMapper.writerFor(VilkårFakta::class.java).writeValueAsString(fakta)
        assertThat(jsonMapper.readTree(serialisert)).isEqualTo(jsonMapper.readTree(json))
    }
}
