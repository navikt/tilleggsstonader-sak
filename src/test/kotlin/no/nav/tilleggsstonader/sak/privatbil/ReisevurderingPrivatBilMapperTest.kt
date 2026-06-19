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
        assertThat(dto.uker.flatMap { it.dager }).allMatch { !it.erDagSlettet }
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
        assertThat(dto.uker.flatMap { it.dager }).allMatch { it.erDagSlettet }
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
        assertThat(dto.uker.flatMap { it.dager }).allMatch { it.erDagSlettet }
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

        // Dager i uke 2 og 3 er ikke slettet, dager i uke 4 er slettet (uke kun i forrige)
        val dagerPerUke = dto.uker.associate { it.ukenummer to it.dager }
        assertThat(dagerPerUke[2]?.map { it.erDagSlettet }).allMatch { !it }
        assertThat(dagerPerUke[3]?.map { it.erDagSlettet }).allMatch { !it }
        assertThat(dagerPerUke[4]?.map { it.erDagSlettet }).allMatch { it }
    }

    @Test
    fun `dager slettet innen uke - kun dager fjernet fra gjeldende rammevedtak er slettet`() {
        val reiseId = ReiseId.random()
        // gjeldende starter onsdag (8 jan) — mandag og tirsdag i uke 2 er fjernet
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 8 januar 2025, // onsdag uke 2
                tom = 10 januar 2025, // fredag uke 2
            )
        val forrigeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025, // mandag uke 2
                tom = 10 januar 2025, // fredag uke 2
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = forrigeReise,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        assertThat(dto.uker).hasSize(1)
        val uke2 = dto.uker.single()
        assertThat(uke2.erUkeSlettet).isFalse()

        val dagSlettetPerDato = uke2.dager.associate { it.dato to it.erDagSlettet }
        assertThat(dagSlettetPerDato[6 januar 2025]).isTrue() // mandag — slettet
        assertThat(dagSlettetPerDato[7 januar 2025]).isTrue() // tirsdag — slettet
        assertThat(dagSlettetPerDato[8 januar 2025]).isFalse() // onsdag — i gjeldende
        assertThat(dagSlettetPerDato[9 januar 2025]).isFalse() // torsdag — i gjeldende
        assertThat(dagSlettetPerDato[10 januar 2025]).isFalse() // fredag — i gjeldende
    }
}
