package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtDagStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UtfyltDagAutomatiskVurdering
import no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager.RegistrertKjørtDag
import no.nav.tilleggsstonader.sak.privatbil.registrertekjørtedager.RegistrertKjørtUke
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode as KontrakterDatoperiode

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

    @Test
    fun `mapper uke med registrertKjørtUke til kjørelisteDag fra registrerte dager`() {
        val reiseId = ReiseId.random()
        val behandlingId = BehandlingId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 10 januar 2025,
            )
        val registrertUke =
            RegistrertKjørtUke(
                behandlingId = behandlingId,
                reiseId = reiseId,
                dager =
                    setOf(
                        RegistrertKjørtDag(dato = 6 januar 2025, harKjørt = true, parkeringsutgift = 80),
                        RegistrertKjørtDag(dato = 7 januar 2025, harKjørt = false),
                        RegistrertKjørtDag(dato = 8 januar 2025, harKjørt = true, parkeringsutgift = null),
                        RegistrertKjørtDag(dato = 9 januar 2025, harKjørt = false),
                        RegistrertKjørtDag(dato = 10 januar 2025, harKjørt = true, parkeringsutgift = 50),
                    ),
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = null,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
                registrerteUker = listOf(registrertUke),
            )

        val dager = dto.uker.single().dager
        val dagPerDato = dager.associateBy { it.dato }

        assertThat(dagPerDato[6 januar 2025]?.kjørelisteDag?.harKjørt).isTrue()
        assertThat(dagPerDato[6 januar 2025]?.kjørelisteDag?.parkeringsutgift).isEqualTo(80)
        assertThat(dagPerDato[7 januar 2025]?.kjørelisteDag?.harKjørt).isFalse()
        assertThat(dagPerDato[7 januar 2025]?.kjørelisteDag?.parkeringsutgift).isNull()
        assertThat(dagPerDato[8 januar 2025]?.kjørelisteDag?.harKjørt).isTrue()
        assertThat(dagPerDato[8 januar 2025]?.kjørelisteDag?.parkeringsutgift).isNull()
        assertThat(dagPerDato[10 januar 2025]?.kjørelisteDag?.harKjørt).isTrue()
        assertThat(dagPerDato[10 januar 2025]?.kjørelisteDag?.parkeringsutgift).isEqualTo(50)
    }

    @Test
    fun `bruker registrertKjørtUke når dersom de er sendt inn og kjøreliste mangler`() {
        val reiseId = ReiseId.random()
        val behandlingId = BehandlingId.random()
        val kjørelisteId = KjørelisteId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 6 januar 2025,
            )

        val kjøreliste =
            KjørelisteUtil.kjøreliste(
                id = kjørelisteId,
                reiseId = reiseId,
                periode = KontrakterDatoperiode(6 januar 2025, 6 januar 2025),
                kjørteDager = listOf(KjørtDag(dato = 6 januar 2025, parkeringsutgift = 100)),
            )

        val avklartUke =
            AvklartKjørtUke(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                kjørelisteId = kjørelisteId,
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 6 januar 2025,
                uke = (6 januar 2025).tilUkeIÅr(),
                status = UkeStatus.OK_AUTOMATISK,
                avklartKjørtUkeStatus = AvklartKjørtUkeStatus.NY,
                dager =
                    setOf(
                        AvklartKjørtDag(
                            dato = 6 januar 2025,
                            godkjentGjennomførtKjøring = GodkjentGjennomførtKjøring.JA,
                            automatiskVurdering = UtfyltDagAutomatiskVurdering.OK,
                            avvik = emptyList(),
                            parkeringsutgift = 100,
                            avklartKjørtDagStatus = AvklartKjørtDagStatus.NY,
                        ),
                    ),
            )

        val registrertUke =
            RegistrertKjørtUke(
                behandlingId = behandlingId,
                reiseId = reiseId,
                dager =
                    setOf(
                        RegistrertKjørtDag(
                            dato = 6 januar 2025,
                            harKjørt = false,
                            parkeringsutgift = null,
                        ),
                    ),
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = null,
                avklarteUker = listOf(avklartUke),
                kjørelister = listOf(kjøreliste),
                registrerteUker = listOf(registrertUke),
            )

        val dag =
            dto.uker
                .single()
                .dager
                .single()
        assertThat(dag.kjørelisteDag?.harKjørt).isTrue()
        assertThat(dag.kjørelisteDag?.parkeringsutgift).isEqualTo(100)
    }

    @Test
    fun `uke med registrertKjørtUke men uten avklartUke får status MANUELT_REGISTRERT`() {
        val reiseId = ReiseId.random()
        val behandlingId = BehandlingId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 6 januar 2025,
            )
        val registrertUke =
            RegistrertKjørtUke(
                behandlingId = behandlingId,
                reiseId = reiseId,
                dager = setOf(RegistrertKjørtDag(dato = 6 januar 2025, harKjørt = true)),
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = null,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
                registrerteUker = listOf(registrertUke),
            )

        assertThat(dto.uker.single().status).isEqualTo(UkeStatus.MANUELT_REGISTRERT)
    }

    @Test
    fun `uke uten avklartUke og uten registrertKjørtUke får status IKKE_MOTTATT_KJØRELISTE`() {
        val reiseId = ReiseId.random()
        val gjeldendeReise =
            rammeForReiseMedPrivatBil(
                reiseId = reiseId,
                fom = 6 januar 2025,
                tom = 6 januar 2025,
            )

        val dto =
            ReisevurderingPrivatBilMapper.tilReisevurderingDto(
                gjeldendeRammevedtakForReise = gjeldendeReise,
                forrigeRammevedtakForReise = null,
                avklarteUker = emptyList(),
                kjørelister = emptyList(),
            )

        assertThat(dto.uker.single().status).isEqualTo(UkeStatus.IKKE_MOTTATT_KJØRELISTE)
    }
}
