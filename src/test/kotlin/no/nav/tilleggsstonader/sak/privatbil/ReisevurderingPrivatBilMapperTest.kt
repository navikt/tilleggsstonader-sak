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

        assertThat(dto.rammevedtak.reiseId).isEqualTo(gjeldendeReise.reiseId)
        assertThat(dto.forrigeRammevedtak).isNull()
        assertThat(dto.uker).allMatch { it.endringIRammevedtakStatus == UkeEndringIRammevedtakStatus.NY }
    }

    @Test
    fun `kun forrige rammevedtak gir kun uendrede uker`() {
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

        assertThat(dto.rammevedtak.reiseId).isEqualTo(forrigeReise.reiseId)
        assertThat(dto.uker).allMatch { it.endringIRammevedtakStatus == UkeEndringIRammevedtakStatus.UENDRET }
    }

    @Test
    fun `sammenslåtte uker markeres med ny, slettet og uendret`() {
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

        val statusPerUke = dto.uker.associate { it.ukenummer to it.endringIRammevedtakStatus }
        assertThat(statusPerUke).containsEntry(2, UkeEndringIRammevedtakStatus.NY)
        assertThat(statusPerUke).containsEntry(3, UkeEndringIRammevedtakStatus.UENDRET)
        assertThat(statusPerUke).containsEntry(4, UkeEndringIRammevedtakStatus.SLETTET)
        assertThat(dto.forrigeRammevedtak).isNotNull
        assertThat(dto.forrigeRammevedtak?.reiseId).isEqualTo(forrigeReise.reiseId)
    }
}
