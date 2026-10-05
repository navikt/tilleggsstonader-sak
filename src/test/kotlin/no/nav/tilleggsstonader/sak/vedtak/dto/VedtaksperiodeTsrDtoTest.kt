package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VedtaksperiodeTsrDtoTest {
    @Test
    fun `bruker valgt faktisk målgruppe når den er sendt`() {
        val vedtaksperiode =
            VedtaksperiodeTsrDto(
                fom = 1 januar 2025,
                tom = 28 februar 2025,
                målgruppeType = FaktiskMålgruppe.UNGDOMSPROGRAMMET,
            ).tilDomene()

        assertThat(vedtaksperiode.målgruppe).isEqualTo(FaktiskMålgruppe.UNGDOMSPROGRAMMET)
    }

    @Test
    fun `bruker arbeidssøker når eldre klient ikke sender målgruppe`() {
        val vedtaksperiode =
            VedtaksperiodeTsrDto(
                fom = 1 januar 2025,
                tom = 28 februar 2025,
            ).tilDomene()

        assertThat(vedtaksperiode.målgruppe).isEqualTo(FaktiskMålgruppe.ARBEIDSSØKER)
    }
}
