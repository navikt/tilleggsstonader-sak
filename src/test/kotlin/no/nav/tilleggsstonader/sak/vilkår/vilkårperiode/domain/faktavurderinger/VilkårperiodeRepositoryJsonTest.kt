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

        assertThat(jsonFraFil).isEqualTo(jsonFraObj)
    }

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

    private fun opprettVilkårperiode(id: UUID, type: String, json: String) {
        namedParameterJdbcTemplate.update(
            insertQuery.trimIndent(),
            mapOf(
                "id" to id,
                "behandlingId" to behandling.id.id,
                "forrigeVilkårperiodeId" to null,
                "type" to type,
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

    companion object {
        @JvmStatic
        fun jsonFilProvider(): List<FaktaVurderingerJsonFilesUtil.JsonFil> = FaktaVurderingerJsonFilesUtil.jsonFiler
    }
}
