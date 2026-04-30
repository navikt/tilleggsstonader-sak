package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.kjøreliste
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RammevedtakPrivatBilDtoMapperTest {
    @Test
    fun `innsendtDato settes kun for uker som har kjørelistedata`() {
        val reiseId = ReiseId.random()

        val rammevedtak =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025, // uke 2
                tom = 19 januar 2025, // uke 3
            )

        val datoMottatt = LocalDateTime.of(2025, 1, 15, 10, 0)
        val kjøreliste =
            kjøreliste(
                reiseId = reiseId,
                datoMottatt = datoMottatt,
                periode = Datoperiode(6 januar 2025, 12 januar 2025), // kun uke 2
                kjørteDager = listOf(KjørtDag(dato = 6 januar 2025)),
            )

        val kjørelister = listOf(kjøreliste).groupBy { it.data.reiseId }
        val result = rammevedtak.tilDto(kjørelister).single()

        val uke2 = result.uker.single { it.ukeNummer == 2 }
        val uke3 = result.uker.single { it.ukeNummer == 3 }

        assertThat(uke2.innsendtDato).isEqualTo(datoMottatt.toLocalDate())
        assertThat(uke3.innsendtDato).isNull()
    }

    @Test
    fun `flere kjørelister for samme reise gir riktig innsendtDato per uke`() {
        val reiseId = ReiseId.random()

        val rammevedtak =
            rammevedtakPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025, // uke 2
                tom = 19 januar 2025, // uke 3
            )

        val datoMottattUke2 = LocalDateTime.of(2025, 1, 13, 9, 0)
        val datoMottattUke3 = LocalDateTime.of(2025, 1, 20, 14, 30)

        val kjørelisteUke2 =
            kjøreliste(
                reiseId = reiseId,
                datoMottatt = datoMottattUke2,
                periode = Datoperiode(6 januar 2025, 12 januar 2025),
                kjørteDager = listOf(KjørtDag(dato = 6 januar 2025)),
            )
        val kjørelisteUke3 =
            kjøreliste(
                reiseId = reiseId,
                datoMottatt = datoMottattUke3,
                periode = Datoperiode(13 januar 2025, 19 januar 2025),
                kjørteDager = listOf(KjørtDag(dato = 13 januar 2025)),
            )

        val kjørelister = listOf(kjørelisteUke2, kjørelisteUke3).groupBy { it.data.reiseId }
        val result = rammevedtak.tilDto(kjørelister).single()

        val uke2 = result.uker.single { it.ukeNummer == 2 }
        val uke3 = result.uker.single { it.ukeNummer == 3 }

        assertThat(uke2.innsendtDato).isEqualTo(datoMottattUke2.toLocalDate())
        assertThat(uke3.innsendtDato).isEqualTo(datoMottattUke3.toLocalDate())
    }

    @Test
    fun `ingen kjøreliste gir null innsendtDato for alle uker`() {
        val rammevedtak =
            rammevedtakPrivatBil(
                fom = 6 januar 2025,
                tom = 19 januar 2025,
            )

        val result = rammevedtak.tilDto(emptyMap()).single()

        assertThat(result.uker).allSatisfy {
            assertThat(it.innsendtDato).isNull()
        }
    }
}
