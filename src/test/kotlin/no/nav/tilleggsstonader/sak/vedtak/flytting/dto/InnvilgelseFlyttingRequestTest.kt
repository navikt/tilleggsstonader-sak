package no.nav.tilleggsstonader.sak.vedtak.flytting.dto

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class InnvilgelseFlyttingRequestTest {
    @Test
    fun `beholder requestfelter og mapper flytteperioder sortert uten aktivitet`() {
        val senerePeriode =
            VedtaksperiodeFlyttingDto(
                id = VedtaksperiodeId.random(),
                fom = 2 januar 2026,
                tom = 3 januar 2026,
                målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            )
        val tidligerePeriode =
            VedtaksperiodeFlyttingDto(
                id = VedtaksperiodeId.random(),
                fom = 1 januar 2026,
                tom = 1 januar 2026,
                målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            )
        val request =
            InnvilgelseFlyttingRequest(
                vedtaksperioder = listOf(senerePeriode, tidligerePeriode),
                begrunnelse = "Begrunnelse",
            )

        val json = jsonMapper.readTree(jsonMapper.writeValueAsString(request))

        assertThat(json.has("vedtaksperioder")).isTrue
        assertThat(json.has("begrunnelse")).isTrue
        val jsonPeriode = json["vedtaksperioder"][0]
        assertThat(jsonPeriode.has("id")).isTrue
        assertThat(jsonPeriode.has("fom")).isTrue
        assertThat(jsonPeriode.has("tom")).isTrue
        assertThat(jsonPeriode.has("målgruppeType")).isTrue
        assertThat(jsonPeriode.has("aktivitet")).isFalse

        assertThat(request.vedtaksperioder()).containsExactly(
            Vedtaksperiode(
                id = tidligerePeriode.id,
                fom = tidligerePeriode.fom,
                tom = tidligerePeriode.tom,
                målgruppe = tidligerePeriode.målgruppeType,
                aktivitet = null,
            ),
            Vedtaksperiode(
                id = senerePeriode.id,
                fom = senerePeriode.fom,
                tom = senerePeriode.tom,
                målgruppe = senerePeriode.målgruppeType,
                aktivitet = null,
            ),
        )
    }

    @Test
    fun `målgruppeType er obligatorisk i requesten`() {
        val json =
            """
            {
              "vedtaksperioder": [
                {
                  "id": "${VedtaksperiodeId.random()}",
                  "fom": "2026-01-01",
                  "tom": "2026-01-31"
                }
              ]
            }
            """.trimIndent()

        assertThatThrownBy {
            jsonMapper.readValue(json, InnvilgelseFlyttingRequest::class.java)
        }.hasMessageContaining("målgruppeType")
    }
}
