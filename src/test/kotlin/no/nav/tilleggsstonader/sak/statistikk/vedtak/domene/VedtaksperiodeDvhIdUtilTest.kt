package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeDvhIdUtilTest {
    val behandlingId = BehandlingId.random()
    val fom = LocalDate.of(2025, 1, 1)
    val tom = LocalDate.of(2025, 1, 31)

    @Test
    fun `like egenskaper skal gi samme id`() {
        val id1 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(
                behandlingId,
                fom,
                tom,
                MålgruppeType.AAP,
                AktivitetType.TILTAK,
            )
        val id2 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(
                behandlingId,
                fom,
                tom,
                MålgruppeType.AAP,
                AktivitetType.TILTAK,
            )

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun `skal gi samme id uavhengig av hvor mange ganger den genereres`() {
        val ider =
            (1..5).map {
                VedtaksperiodeDvhIdUtil.genererDeterministiskId(
                    behandlingId,
                    fom,
                    tom,
                    MålgruppeType.AAP,
                    AktivitetType.TILTAK,
                )
            }

        assertThat(ider.distinct()).hasSize(1)
    }

    @Test
    fun `ulik behandlingId skal gi ulik id sånn at like perioder på ulike behandlinger ikke kolliderer`() {
        val annenBehandlingId = BehandlingId.random()

        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)
        val id2 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(annenBehandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `ulik fom skal gi ulik id`() {
        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)
        val id2 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom.plusDays(1), tom, MålgruppeType.AAP, AktivitetType.TILTAK)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `ulik tom skal gi ulik id`() {
        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)
        val id2 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom.plusDays(1), MålgruppeType.AAP, AktivitetType.TILTAK)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `ulik målgruppe skal gi ulik id`() {
        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)
        val id2 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(
                behandlingId,
                fom,
                tom,
                MålgruppeType.OVERGANGSSTØNAD,
                AktivitetType.TILTAK,
            )

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `ulik aktivitet skal gi ulik id`() {
        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK)
        val id2 =
            VedtaksperiodeDvhIdUtil.genererDeterministiskId(
                behandlingId,
                fom,
                tom,
                MålgruppeType.AAP,
                AktivitetType.UTDANNING,
            )

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `ekstra periodeEgenskaper som f eks antallBarn eller studienivå skal påvirke id-en`() {
        val id1 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK, 1)
        val id2 = VedtaksperiodeDvhIdUtil.genererDeterministiskId(behandlingId, fom, tom, MålgruppeType.AAP, AktivitetType.TILTAK, 2)

        assertThat(id1).isNotEqualTo(id2)
    }
}
