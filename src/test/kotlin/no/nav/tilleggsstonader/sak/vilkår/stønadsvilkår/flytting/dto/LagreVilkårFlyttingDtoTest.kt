package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class LagreVilkårFlyttingDtoTest {
    @Test
    fun `skal mappe ufullstendige delvilkår til domene uten å legge til svar`() {
        val fakta = FaktaFlyttingUbestemtDto("Adresse 1")
        val domain = dto(svar = emptyMap(), fakta = fakta).tilDomene()

        assertThat(domain.flyttedato).isEqualTo(1 januar 2026)
        assertThat(domain.svar).isEmpty()
        assertThat(domain.fakta).isEqualTo(FaktaFlyttingUbestemt("Adresse 1"))
    }

    @Test
    fun `skal deserialisere ubesvarte delvilkår uten aktivitetskobling`() {
        val dto =
            jsonMapper.readValue<LagreVilkårFlyttingDto>(
                """
                {
                  "flyttedato": "2026-01-01",
                  "svar": {},
                  "fakta": {"type": "FLYTTING_UBESTEMT", "adresse": "Flytteveien 1"}
                }
                """.trimIndent(),
            )

        assertThat(dto.flyttedato).isEqualTo(1 januar 2026)
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
                  "tilbud": [{"navn": "Flyttebyrå A"}, {}],
                  "adresse": "Flytteveien 1",
                  "erBetalingDokumentert": false,
                  "nødvendigUtgift": {"erUtgiftenNødvendig": "JA", "begrunnelse": null}
                }
                """.trimIndent(),
            )

        assertThat(fakta).isEqualTo(
            FaktaFlyttebyråDto(
                tilbud =
                    listOf(
                        FlyttebyråTilbudDto(navn = "Flyttebyrå A"),
                        FlyttebyråTilbudDto(),
                    ),
                adresse = "Flytteveien 1",
                erBetalingDokumentert = false,
                nødvendigUtgift = nødvendigUtgiftDto,
            ),
        )
    }

    @Test
    fun `skal deserialisere egen kjøring uten avstand og kostnader`() {
        val fakta =
            jsonMapper.readValue<FaktaFlyttingDto>(
                """{"type": "FLYTTING_FLYTTE_SELV", "adresse": "Flytteveien 1", "erUtgifterDokumentert": true, "nødvendigUtgift": {"erUtgiftenNødvendig": "JA", "begrunnelse": null}}""",
            )

        assertThat(fakta).isEqualTo(
            FaktaFlytteSelvDto(erUtgifterDokumentert = true, adresse = "Flytteveien 1", nødvendigUtgift = nødvendigUtgiftDto),
        )
    }

    private fun dto(
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
        fakta: FaktaFlyttingDto,
    ) = LagreVilkårFlyttingDto(
        flyttedato = 1 januar 2026,
        svar = svar,
        fakta = fakta,
    )

    private val nødvendigUtgiftDto = NødvendigUtgiftDto(SvarJaNei.JA, null)
}
