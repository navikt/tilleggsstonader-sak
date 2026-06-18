package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReisevurderingPrivatBilMapperTest {
    @Test
    fun `kun gjeldende rammevedtak gir kun nye uker`() {
        val reiseId = ReiseId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 19 januar 2025,
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = null,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        assertThat(dto.rammevedtak?.reiseId).isEqualTo(gjeldendeReise.reiseId)
        assertThat(dto.forrigeRammevedtak).isNull()
        assertThat(dto.uker).allMatch { !it.erUkeSlettet }
    }

    @Test
    fun `kun forrige rammevedtak uten gjeldende vedtak gir kun slettede uker`() {
        val reiseId = ReiseId.random()
        val forrigeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 19 januar 2025,
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = null,
                forrigeRammevedtakForReise = forrigeReise,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        assertThat(dto.reiseId).isEqualTo(forrigeReise.reiseId)
        assertThat(dto.uker).allMatch { it.erUkeSlettet }
    }

    @Test
    fun `hel reise slettet fra gjeldende vedtak gir kun slettede uker`() {
        val reiseId = ReiseId.random()
        val forrigeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 19 januar 2025,
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = null,
                forrigeRammevedtakForReise = forrigeReise,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        assertThat(dto.reiseId).isEqualTo(forrigeReise.reiseId)
        assertThat(dto.uker).allMatch { it.erUkeSlettet }
    }

    @Test
    fun `sammenslåtte uker - kun slettet hvis ikke i gjeldende rammevedtak`() {
        val reiseId = ReiseId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025, // uke 2
                tom = 19 januar 2025, // uke 3
            )
        val forrigeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 13 januar 2025, // uke 3
                tom = 26 januar 2025, // uke 4
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = forrigeReise,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        val slettetPerUke = dto.uker.associate { it.ukenummer to it.erUkeSlettet }
        assertThat(slettetPerUke).containsEntry(2, false) // kun i gjeldende
        assertThat(slettetPerUke).containsEntry(3, false) // i begge
        assertThat(slettetPerUke).containsEntry(4, true) // kun i forrige
        assertThat(dto.forrigeRammevedtak).isNotNull
        assertThat(dto.forrigeRammevedtak?.reiseId).isEqualTo(forrigeReise.reiseId)
    }
}
