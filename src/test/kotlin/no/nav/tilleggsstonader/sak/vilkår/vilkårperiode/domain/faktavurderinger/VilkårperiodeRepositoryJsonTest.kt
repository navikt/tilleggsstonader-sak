package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaVurderingerJsonFilesUtil.tilTypeFaktaOgVurderingSuffix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.io.File
import java.time.LocalDate
import java.util.UUID

class VilkårperiodeRepositoryJsonTest : IntegrationTest() {

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
    }

    @ParameterizedTest
    @MethodSource("jsonFilProvider")
    fun `sjekk at deserialisering av json fungerer som forventet`(fil: FaktaVurderingerJsonFilesUtil.JsonFil) {
        val json = FileUtil.readFile(fil.fullPath())
        val id = UUID.randomUUID()

        opprettVilkårperiode(id, fil.typeVilkårperiode(), json)
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)
        val jsonFraObj = objectMapper.convertValue<Map<String, Any>>(vilkårperiode.faktaOgVurdering).toSortedMap()
        val jsonFraFil = objectMapper.readValue<Map<String, Any>>(json).toSortedMap()

        assertThat(vilkårperiode.faktaOgVurdering).isInstanceOf(forventetType(fil.typeFaktaOgVurdering()))
        assertThat(jsonFraFil).isEqualTo(jsonFraObj)
    }

    /**
     * Når man legger til nye typer kan denne generere dummy-filer som man kan legge inn riktig data i
     */
    @Disabled
    @Test
    fun `opprett dummy-filer`() {
        alleEnumTyperFaktaOgVurdering.forEach { (stønadstype, type) ->
            val dir = stønadstype.tilTypeFaktaOgVurderingSuffix()
            val enumName = type.enumName()

            val file = File("src/test/resources/vilkår/vilkårperiode/$dir/$enumName.json")
            if (!file.exists()) {
                println("${file.name}: Oppretter")
                file.createNewFile()
                file.writeText("{\n  \"type\": \"$enumName\",\n  \"fakta\": {},\n  \"vurderinger\": {}\n}")
            } else {
                println("${file.name}: Eksisterer, oppretter ikke")
            }
        }
    }

    private fun opprettVilkårperiode(id: UUID, type: VilkårperiodeType, json: String) {
        namedParameterJdbcTemplate.update(
            insertQuery.trimIndent(),
            mapOf(
                "id" to id,
                "behandlingId" to behandling.id.id,
                "forrigeVilkårperiodeId" to null,
                "type" to type.tilDbType(),
                "fom" to LocalDate.now(),
                "tom" to LocalDate.now(),
                "faktaOgVurdering" to json,
                "begrunnelse" to "begrunnelse",
                "resultat" to "OPPFYLT",
                "slettetKommentar" to null,
                "status" to "NY",
                "opprettet_av" to "",
                "opprettet_tid" to SporbarUtils.now(),
                "endret_av" to "",
                "endret_tid" to SporbarUtils.now(),
                "kildeId" to null,
                "kilde" to "MANUELL",
            ),
        )
    }

    val insertQuery = """
        INSERT INTO vilkar_periode 
        (
            id,
            behandling_id,
            forrige_vilkarperiode_id,
            type,
            fom,
            tom,
            fakta_og_vurdering,
            begrunnelse,
            resultat,
            slettet_kommentar,
            status,
            opprettet_av,
            opprettet_tid,
            endret_av,
            endret_tid,
            kilde_id,
            kilde
        ) values (
            :id,
            :behandlingId,
            :forrigeVilkårperiodeId,
            :type,
            :fom,
            :tom,
            (:faktaOgVurdering)::jsonb,
            :begrunnelse,
            :resultat,
            :slettetKommentar,
            :status,
            :opprettet_av,
            :opprettet_tid,
            :endret_av,
            :endret_tid,
            :kildeId,
            :kilde
        )
    """

    private fun forventetType(type: TypeFaktaOgVurdering): Class<out FaktaOgVurdering> {
        if (type is TypeFaktaOgVurderingTilsynBarn) {
            return forventetTypeTilsynBarn(type)
        } else if (type is TypeFaktaOgVurderingLæremidler) {
            return forventetTypeLæremidler(type)
        }
        error("Ukjent type")
    }

    private fun forventetTypeTilsynBarn(type: TypeFaktaOgVurderingTilsynBarn): Class<out FaktaOgVurderingTilsynBarn> =
        when (type) {
            is MålgruppeTilsynBarnType -> {
                when (type) {
                    MålgruppeTilsynBarnType.SYKEPENGER_100_PROSENT_TILSYN_BARN -> SykepengerTilsynBarn::class
                    MålgruppeTilsynBarnType.INGEN_MÅLGRUPPE_TILSYN_BARN -> IngenMålgruppeTilsynBarn::class
                    MålgruppeTilsynBarnType.OMSTILLINGSSTØNAD_TILSYN_BARN -> OmstillingsstønadTilsynBarn::class
                    MålgruppeTilsynBarnType.OVERGANGSSTØNAD_TILSYN_BARN -> OvergangssstønadTilsynBarn::class
                    MålgruppeTilsynBarnType.AAP_TILSYN_BARN -> AAPTilsynBarn::class
                    MålgruppeTilsynBarnType.UFØRETRYGD_TILSYN_BARN -> UføretrygdTilsynBarn::class
                    MålgruppeTilsynBarnType.NEDSATT_ARBEIDSEVNE_TILSYN_BARN -> NedsattArbeidsevneTilsynBarn::class
                }
            }

            is AktivitetTilsynBarnType -> {
                when (type) {
                    AktivitetTilsynBarnType.UTDANNING_TILSYN_BARN -> UtdanningTilsynBarn::class
                    AktivitetTilsynBarnType.TILTAK_TILSYN_BARN -> TiltakTilsynBarn::class
                    AktivitetTilsynBarnType.REELL_ARBEIDSSØKER_TILSYN_BARN -> ReellArbeidsøkerTilsynBarn::class
                    AktivitetTilsynBarnType.INGEN_AKTIVITET_TILSYN_BARN -> IngenAktivitetTilsynBarn::class
                }
            }
        }.java

    private fun forventetTypeLæremidler(type: TypeFaktaOgVurderingLæremidler): Class<out FaktaOgVurderingLæremidler> =
        when (type) {
            is MålgruppeLæremidlerType -> {
                when (type) {
                    MålgruppeLæremidlerType.AAP_LÆREMIDLER -> AAPLæremidler::class
                    MålgruppeLæremidlerType.UFØRETRYGD_LÆREMIDLER -> UføretrygdLæremidler::class
                    MålgruppeLæremidlerType.NEDSATT_ARBEIDSEVNE_LÆREMIDLER -> NedsattArbeidsevneLæremidler::class
                    MålgruppeLæremidlerType.OMSTILLINGSSTØNAD_LÆREMIDLER -> OmstillingsstønadLæremidler::class
                    MålgruppeLæremidlerType.OVERGANGSSTØNAD_LÆREMIDLER -> OvergangssstønadLæremidler::class
                    MålgruppeLæremidlerType.INGEN_MÅLGRUPPE_LÆREMIDLER -> IngenMålgruppeLæremidler::class
                    MålgruppeLæremidlerType.SYKEPENGER_100_PROSENT_LÆREMIDLER -> SykepengerLæremidler::class
                }
            }

            is AktivitetLæremidlerType -> {
                when (type) {
                    AktivitetLæremidlerType.UTDANNING_LÆREMIDLER -> UtdanningLæremidler::class
                    AktivitetLæremidlerType.TILTAK_LÆREMIDLER -> TiltakLæremidler::class
                    AktivitetLæremidlerType.INGEN_UTDANNING_LÆREMIDLER -> IngenUtdanningLæremidler::class
                }
            }
        }.java

    companion object {
        @JvmStatic
        fun jsonFilProvider(): List<FaktaVurderingerJsonFilesUtil.JsonFil> = FaktaVurderingerJsonFilesUtil.jsonFiler
    }
}
