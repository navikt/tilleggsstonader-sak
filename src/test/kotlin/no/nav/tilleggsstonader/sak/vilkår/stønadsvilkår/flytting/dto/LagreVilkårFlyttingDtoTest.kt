package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class LagreVilkårFlyttingDtoTest {
    @Test
    fun `skal mappe ufullstendige delvilkår til domene uten å legge til svar`() {
        val domain = dto(svar = emptyMap(), fakta = FaktaFlyttingUbestemtDto()).tilDomain()

        assertThat(domain.fom).isEqualTo(1 januar 2026)
        assertThat(domain.tom).isEqualTo(31 januar 2026)
        assertThat(domain.svar).isEmpty()
        assertThat(domain.fakta).isEqualTo(FaktaFlyttingUbestemt())
    }

    @Test
    fun `skal bevare svar og valgfri begrunnelse ved mapping til domene`() {
        val svar = SvarOgBegrunnelseDto(svar = SvarId.FLYTTEBYRÅ, begrunnelse = "Vurdering")
        val domain =
            dto(
                svar = mapOf(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to svar),
                fakta = FaktaFlyttingUbestemtDto(),
            ).tilDomain()

        assertThat(domain.svar).containsOnlyKeys(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE)
        assertThat(domain.svar[RegelId.HVORDAN_SKAL_BRUKER_FLYTTE]).isEqualTo(svar.tilDomain())
    }

    @Test
    fun `skal deserialisere ubesvarte delvilkår uten aktivitetskobling`() {
        val dto =
            jsonMapper.readValue<LagreVilkårFlyttingDto>(
                """
                {
                  "fom": "2026-01-01",
                  "tom": "2026-01-31",
                  "svar": {},
                  "fakta": {"type": "FLYTTING_UBESTEMT", "adresse": "Flytteveien 1"}
                }
                """.trimIndent(),
            )

        assertThat(dto.fom).isEqualTo(1 januar 2026)
        assertThat(dto.tom).isEqualTo(31 januar 2026)
        assertThat(dto.svar).isEmpty()
        assertThat(dto.fakta).isEqualTo(FaktaFlyttingUbestemtDto(adresse = "Flytteveien 1"))
    }

    @Test
    fun `skal deserialisere flyttebyrå med ufullstendige tilbud`() {
        val fakta =
            jsonMapper.readValue<FaktaFlyttingDto>(
                """
                {
                  "type": "FLYTTING_FLYTTEBYRÅ",
                  "tilbud1": {"navn": "Flyttebyrå A"},
                  "tilbud2": {}
                }
                """.trimIndent(),
            )

        assertThat(fakta).isEqualTo(
            FaktaFlyttebyråDto(
                tilbud1 = FlyttebyråTilbudDto(navn = "Flyttebyrå A"),
            ),
        )
    }

    @Test
    fun `skal deserialisere egen kjøring uten avstand og kostnader`() {
        val fakta = jsonMapper.readValue<FaktaFlyttingDto>("""{"type": "FLYTTING_FLYTTE_SELV"}""")

        assertThat(fakta).isEqualTo(FaktaFlytteSelvDto())
    }

    @Test
    fun `skal støtte eldre fakta uten adresse`() {
        val fakta = jsonMapper.readValue<FaktaFlyttingDto>("""{"type": "FLYTTING_UBESTEMT"}""")

        assertThat(fakta).isEqualTo(FaktaFlyttingUbestemtDto())
    }

    @Test
    fun `skal bevare to tilbud og valgfri begrunnelse gjennom json`() {
        val dto =
            dto(
                svar =
                    mapOf(
                        RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to
                            SvarOgBegrunnelseDto(svar = SvarId.FLYTTEBYRÅ, begrunnelse = "To tilbud er mottatt"),
                    ),
                fakta =
                    FaktaFlyttebyråDto(
                        tilbud1 = FlyttebyråTilbudDto(navn = "Flyttebyrå A", pris = 10000),
                        tilbud2 = FlyttebyråTilbudDto(navn = "Flyttebyrå B", pris = 12000),
                        adresse = "Flytteveien 1",
                    ),
            )

        assertThat(jsonMapper.readValue<LagreVilkårFlyttingDto>(jsonMapper.writeValueAsString(dto))).isEqualTo(dto)
    }

    @Test
    fun `skal bevare avstand en vei og skille null fra null kroner gjennom json`() {
        val dto =
            dto(
                svar =
                    mapOf(
                        RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to
                            SvarOgBegrunnelseDto(svar = SvarId.FLYTTER_SELV),
                    ),
                fakta =
                    FaktaFlytteSelvDto(
                        avstandEnVei = 250,
                        henger = 1500,
                        bompenger = 0,
                        ferge = null,
                        parkering = 100,
                        adresse = "Flytteveien 1",
                    ),
            )

        assertThat(jsonMapper.readValue<LagreVilkårFlyttingDto>(jsonMapper.writeValueAsString(dto))).isEqualTo(dto)
    }

    @Test
    fun `skal bevare manglende svar på flyttemåte`() {
        val dto =
            dto(
                svar = emptyMap(),
                fakta = FaktaFlyttingUbestemtDto(),
            )

        assertThat(jsonMapper.readValue<LagreVilkårFlyttingDto>(jsonMapper.writeValueAsString(dto))).isEqualTo(dto)
    }

    private fun dto(
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
        fakta: FaktaFlyttingDto,
    ) = LagreVilkårFlyttingDto(
        fom = 1 januar 2026,
        tom = 31 januar 2026,
        svar = svar,
        fakta = fakta,
    )
}
