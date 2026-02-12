package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.infrastruktur.database.IdConverters.alleValueClassConverters
import no.nav.tilleggsstonader.sak.oppfølging.OppfølgingData
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagData
import no.nav.tilleggsstonader.sak.opplysninger.søknad.boutgifter.SkjemaBoutgifter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.SkjemaDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaLæremidler
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.TilbakekrevingJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.DefaultQueryMappingConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.RollbackOn
import tools.jackson.module.kotlin.readValue
import java.util.Optional
import javax.sql.DataSource
import kotlin.reflect.KClass

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.tilleggsstonader.sak", "no.nav.familie.prosessering")
@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
class DatabaseConfiguration : AbstractJdbcConfiguration() {
    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations = NamedParameterJdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> = AuditorAware { Optional.of(Endret()) }

    @Bean
    fun verifyIgnoreIfProd(
        @Value("\${spring.flyway.placeholders.ignoreIfProd}") ignoreIfProd: String,
        environment: Environment,
    ): FlywayConfigurationCustomizer {
        val isProd = environment.activeProfiles.contains("prod")
        val ignore = ignoreIfProd == "--"
        return FlywayConfigurationCustomizer {
            if (isProd && !ignore) {
                throw RuntimeException("Prod profile men har ikke riktig verdi for placeholder ignoreIfProd=$ignoreIfProd")
            }
            if (!isProd && ignore) {
                throw RuntimeException("Profile=${environment.activeProfiles} men har ignoreIfProd=--")
            }
        }
    }

    @Bean
    fun rowMappers(): QueryMappingConfiguration =
        DefaultQueryMappingConfiguration()
            .registerRowMapper<SettPåVent>(SettPåVent::class.java, SettPåVentRowMapper())

    override fun userConverters(): List<*> =
        listOf(
            StringTilPropertiesWrapperConverter(),
            PropertiesWrapperTilStringConverter(),
            PGobjectTilJsonWrapperConverter(),
            JsonWrapperTilPGobjectConverter(),
            PGobjectTilDelvilkårConverter(),
            DelvilkårTilPGobjectConverter(),
            SimuleringResponseWriter(),
            SimuleringResponseReader(),
            ÅrsakerReader(),
            ÅrsakerWriter(),
            SkjemaBarnetilsynReader(),
            SkjemaBarnetilsynWriter(),
            SkjemaLæremidlerReader(),
            SkjemaLæremidlerWriter(),
            SkjemaBoutgifterReader(),
            SkjemaBoutgifterWriter(),
            SkjemaDagligReiseReader(),
            SkjemaDagligReiseWriter(),
            BarnMedBarnepassReader(),
            BarnMedBarnepassWriter(),
            FilTilBytearrayConverter(),
            BytearrayTilFilConverter(),
            VedtaksdataReader(),
            VedtaksdataWriter(),
            TilVilkårperiodeTypeConverter(),
            VilkårperiodeTypeTilStringConverter(),
            VilkårperioderGrunnlagReader(),
            VilkårperioderGrunnlagWriter(),
            FaktaOgVurderingReader(),
            FaktaOgVurderingWriter(),
            OppfølgingDataReader(),
            OppfølgingDataWriter(),
            FaktaGrunnlagDataReader(),
            FaktaGrunnlagDataWriter(),
            VilkårFaktaDataReader(),
            VilkårFaktaDataWriter(),
            TilbakekrevingJsonDataReader(),
            TilbakekrevingJsonDataWriter(),
            InnsendtKjørelisteReader(),
            InnsendtKjørelisteWriter(),
        ) + alleVedtaksstatistikkJsonConverters +
            alleValueClassConverters

    @WritingConverter
    abstract class JsonWriter<T : Any> : Converter<T, PGobject> {
        override fun convert(source: T): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(source)
            }
    }

    @ReadingConverter
    abstract class JsonReader<T : Any>(
        val clazz: KClass<T>,
    ) : Converter<PGobject, T?> {
        override fun convert(pGobject: PGobject): T? = pGobject.value?.let { jsonMapper.readValue(it, clazz.java) }
    }

    @ReadingConverter
    class PGobjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {
        override fun convert(pGobject: PGobject): JsonWrapper? = pGobject.value?.let { JsonWrapper(it) }
    }

    @WritingConverter
    class JsonWrapperTilPGobjectConverter : Converter<JsonWrapper, PGobject> {
        override fun convert(jsonWrapper: JsonWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonWrapper.json
            }
    }

    @WritingConverter
    class FilTilBytearrayConverter : Converter<Fil, ByteArray> {
        override fun convert(fil: Fil): ByteArray = fil.bytes
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {
        override fun convert(bytes: ByteArray): Fil = Fil(bytes)
    }

    @ReadingConverter
    class PGobjectTilDelvilkårConverter : Converter<PGobject, DelvilkårWrapper> {
        override fun convert(pGobject: PGobject): DelvilkårWrapper =
            DelvilkårWrapper(pGobject.value?.let { jsonMapper.readValue(it) } ?: emptyList())
    }

    @WritingConverter
    class DelvilkårTilPGobjectConverter : Converter<DelvilkårWrapper, PGobject> {
        override fun convert(data: DelvilkårWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = jsonMapper.writeValueAsString(data.delvilkårsett)
            }
    }

    @ReadingConverter
    class TilVilkårperiodeTypeConverter : Converter<String, VilkårperiodeType> {
        override fun convert(type: String): VilkårperiodeType = vilkårperiodetyper[type] ?: error("Finner ikke mapping for $type")
    }

    @WritingConverter
    class VilkårperiodeTypeTilStringConverter : Converter<VilkårperiodeType, String> {
        override fun convert(data: VilkårperiodeType): String = data.tilDbType()
    }

    class SimuleringResponseWriter : JsonWriter<SimuleringJson>()

    class SimuleringResponseReader : JsonReader<SimuleringJson>(SimuleringJson::class)

    class ÅrsakerReader : JsonReader<Årsaker>(Årsaker::class)

    class ÅrsakerWriter : JsonWriter<Årsaker>()

    // Søknad
    class SkjemaBarnetilsynReader : JsonReader<SkjemaBarnetilsyn>(SkjemaBarnetilsyn::class)

    class SkjemaBarnetilsynWriter : JsonWriter<SkjemaBarnetilsyn>()

    class SkjemaLæremidlerReader : JsonReader<SkjemaLæremidler>(SkjemaLæremidler::class)

    class SkjemaLæremidlerWriter : JsonWriter<SkjemaLæremidler>()

    class SkjemaBoutgifterReader : JsonReader<SkjemaBoutgifter>(SkjemaBoutgifter::class)

    class SkjemaBoutgifterWriter : JsonWriter<SkjemaBoutgifter>()

    class SkjemaDagligReiseReader : JsonReader<SkjemaDagligReise>(SkjemaDagligReise::class)

    class SkjemaDagligReiseWriter : JsonWriter<SkjemaDagligReise>()

    class BarnMedBarnepassReader : JsonReader<BarnMedBarnepass>(BarnMedBarnepass::class)

    class BarnMedBarnepassWriter : JsonWriter<BarnMedBarnepass>()

    class VedtaksdataReader : JsonReader<Vedtaksdata>(Vedtaksdata::class)

    class VedtaksdataWriter : JsonWriter<Vedtaksdata>()

    class VilkårperioderGrunnlagReader : JsonReader<VilkårperioderGrunnlag>(VilkårperioderGrunnlag::class)

    class VilkårperioderGrunnlagWriter : JsonWriter<VilkårperioderGrunnlag>()

    class FaktaOgVurderingReader : JsonReader<FaktaOgVurdering>(FaktaOgVurdering::class)

    class FaktaOgVurderingWriter : JsonWriter<FaktaOgVurdering>()

    class OppfølgingDataReader : JsonReader<OppfølgingData>(OppfølgingData::class)

    class OppfølgingDataWriter : JsonWriter<OppfølgingData>()

    class FaktaGrunnlagDataReader : JsonReader<FaktaGrunnlagData>(FaktaGrunnlagData::class)

    class FaktaGrunnlagDataWriter : JsonWriter<FaktaGrunnlagData>()

    class VilkårFaktaDataReader : JsonReader<VilkårFakta>(VilkårFakta::class)

    class VilkårFaktaDataWriter : JsonWriter<VilkårFakta>()

    class TilbakekrevingJsonDataReader : JsonReader<TilbakekrevingJson>(TilbakekrevingJson::class)

    class TilbakekrevingJsonDataWriter : JsonWriter<TilbakekrevingJson>()

    class InnsendtKjørelisteReader : JsonReader<InnsendtKjøreliste>(InnsendtKjøreliste::class)

    class InnsendtKjørelisteWriter : JsonWriter<InnsendtKjøreliste>()
}
