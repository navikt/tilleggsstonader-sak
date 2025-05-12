package no.nav.tilleggsstonader.sak.vedtak.domain

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaVurderingerJsonFilesUtil.tilTypeFaktaOgVurderingSuffix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class VedtakRepositoryJsonTest : IntegrationTest() {
    @Autowired
    lateinit var repository: VedtakRepository

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
    }

    @ParameterizedTest
    @MethodSource("jsonFilProvider")
    fun `sjekk at deserialisering av json fungerer som forventet`(fil: VedtaksdataFilesUtil.JsonFil) {
        val json = FileUtil.readFile(fil.fullPath())

        opprettVedtak(fil.typeVedtaksdata().typeVedtak, json)
        val vedtak = repository.findByIdOrThrow(behandling.id)
        val jsonFraObj = objectMapper.convertValue<Map<String, Any>>(vedtak.data).toSortedMap()
        val jsonFraFil = objectMapper.readValue<Map<String, Any>>(json).toSortedMap()

        assertThat(vedtak.data).isInstanceOf(forventetType(fil.typeVedtaksdata()))
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFraFil))
            .isEqualTo(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFraObj))
    }

    @Disabled
    @Test
    fun `opprett dummy-filer`() {
        alleEnumTypeVedtaksdata.forEach { (stønadstype, type) ->
            val vedtakDir = stønadstype.tilTypeFaktaOgVurderingSuffix()
            val enumName = type.enumName()

            val dir = "src/test/resources/vedtak/$vedtakDir"
            if (!File(dir).exists()) {
                Files.createDirectory(Path.of(dir))
            }
            val file = File("$dir/$enumName.json")
            if (!file.exists()) {
                println("${file.name}: Oppretter")
                file.createNewFile()
                file.writeText("{\n  \"type\": \"$enumName\",\n  \"\": \n}")
            } else {
                println("${file.name}: Eksisterer, oppretter ikke")
            }
        }
    }

    private fun opprettVedtak(
        type: TypeVedtak,
        json: String,
    ) {
        jdbcTemplate.update(
            insertQuery.trimIndent(),
            mapOf(
                "behandlingId" to behandling.id.id,
                "type" to type.name,
                "data" to json,
                "opprettet_av" to "",
                "opprettet_tid" to SporbarUtils.now(),
                "endret_av" to "",
                "endret_tid" to SporbarUtils.now(),
            ),
        )
    }

    val insertQuery = """
        INSERT INTO vedtak 
        (
            behandling_id,
            type,
            data,
            opprettet_av,
            opprettet_tid,
            endret_av,
            endret_tid
        ) values (
            :behandlingId,
            :type,
            (:data)::jsonb,
            :opprettet_av,
            :opprettet_tid,
            :endret_av,
            :endret_tid
        )
    """

    private fun forventetType(type: TypeVedtaksdata): Class<out Vedtaksdata> =
        when (type) {
            TypeVedtakTilsynBarn.INNVILGELSE_TILSYN_BARN -> InnvilgelseTilsynBarn::class
            TypeVedtakTilsynBarn.AVSLAG_TILSYN_BARN -> AvslagTilsynBarn::class
            TypeVedtakTilsynBarn.OPPHØR_TILSYN_BARN -> OpphørTilsynBarn::class
            TypeVedtakLæremidler.INNVILGELSE_LÆREMIDLER -> InnvilgelseLæremidler::class
            TypeVedtakLæremidler.AVSLAG_LÆREMIDLER -> AvslagLæremidler::class
            TypeVedtakLæremidler.OPPHØR_LÆREMIDLER -> OpphørLæremidler::class
            TypeVedtakBoutgifter.INNVILGELSE_BOUTGIFTER -> InnvilgelseBoutgifter::class
            TypeVedtakBoutgifter.AVSLAG_BOUTGIFTER -> AvslagBoutgifter::class
            TypeVedtakBoutgifter.OPPHØR_BOUTGIFTER -> OpphørBoutgifter::class
        }.java

    companion object {
        @JvmStatic
        fun jsonFilProvider(): List<VedtaksdataFilesUtil.JsonFil> = VedtaksdataFilesUtil.jsonFiler
    }
}
