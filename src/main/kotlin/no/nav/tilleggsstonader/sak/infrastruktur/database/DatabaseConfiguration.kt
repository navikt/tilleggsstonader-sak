package no.nav.tilleggsstonader.sak.infrastruktur.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.infrastruktur.database.IdConverters.alleValueClassConverters
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaLæremidler
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtaksdataTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import java.util.Optional
import javax.sql.DataSource
import kotlin.reflect.KClass

@Configuration
@EnableJdbcAuditing
@EnableJdbcRepositories("no.nav.tilleggsstonader.sak", "no.nav.familie.prosessering")
class DatabaseConfiguration : AbstractJdbcConfiguration() {

    @Bean
    fun operations(dataSource: DataSource): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun auditSporbarEndret(): AuditorAware<Endret> {
        return AuditorAware { Optional.of(Endret()) }
    }

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
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(
                StringTilPropertiesWrapperConverter(),
                PropertiesWrapperTilStringConverter(),

                PGobjectTilJsonWrapperConverter(),
                JsonWrapperTilPGobjectConverter(),

                PGobjectTilDelvilkårConverter(),
                DelvilkårTilPGobjectConverter(),

                SimuleringResponseWriter(),
                SimuleringResponseReader(),

                GrunnlagReader(),
                GrunnlagWriter(),

                ÅrsakerReader(),
                ÅrsakerWriter(),

                SkjemaBarnetilsynReader(),
                SkjemaBarnetilsynWriter(),
                BarnMedBarnepassReader(),
                BarnMedBarnepassWriter(),

                FilTilBytearrayConverter(),
                BytearrayTilFilConverter(),

                VedtaksdataTilsynBarnReader(),
                VedtaksdataTilsynBarnWriter(),
                BeregningsresultatTilsynBarnReader(),
                BeregningsresultatTilsynBarnWriter(),
                ÅrsakAvslagReader(),
                ÅrsakAvslagWriter(),
                ÅrsakOpphørReader(),
                ÅrsakOpphørWriter(),

                TilVilkårperiodeTypeConverter(),
                VilkårperiodeTypeTilStringConverter(),

                DetaljerVilkårperiodeReader(),
                DetaljerVilkårperiodeWriter(),

                VilkårperioderGrunnlagReader(),
                VilkårperioderGrunnlagWriter(),

                SkjemaLæremidlerReader(),
                SkjemaLæremidlerWriter(),

            ) + alleVedtaksstatistikkJsonConverters +
                alleValueClassConverters,
        )
    }

    @WritingConverter
    abstract class JsonWriter<T> : Converter<T, PGobject> {
        override fun convert(source: T & Any): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(source)
            }
    }

    @ReadingConverter
    abstract class JsonReader<T : Any>(val clazz: KClass<T>) : Converter<PGobject, T> {
        override fun convert(pGobject: PGobject): T? {
            return pGobject.value?.let { objectMapper.readValue(it, clazz.java) }
        }
    }

    @ReadingConverter
    class PGobjectTilJsonWrapperConverter : Converter<PGobject, JsonWrapper?> {

        override fun convert(pGobject: PGobject): JsonWrapper? {
            return pGobject.value?.let { JsonWrapper(it) }
        }
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

        override fun convert(fil: Fil): ByteArray {
            return fil.bytes
        }
    }

    @ReadingConverter
    class BytearrayTilFilConverter : Converter<ByteArray, Fil> {

        override fun convert(bytes: ByteArray): Fil {
            return Fil(bytes)
        }
    }

    @ReadingConverter
    class PGobjectTilDelvilkårConverter : Converter<PGobject, DelvilkårWrapper> {

        override fun convert(pGobject: PGobject): DelvilkårWrapper {
            return DelvilkårWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
        }
    }

    @WritingConverter
    class DelvilkårTilPGobjectConverter : Converter<DelvilkårWrapper, PGobject> {

        override fun convert(data: DelvilkårWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.delvilkårsett)
            }
    }

    @ReadingConverter
    class TilVilkårperiodeTypeConverter : Converter<String, VilkårperiodeType> {

        override fun convert(type: String): VilkårperiodeType {
            return vilkårperiodetyper[type] ?: error("Finner ikke mapping for $type")
        }
    }

    @WritingConverter
    class VilkårperiodeTypeTilStringConverter : Converter<VilkårperiodeType, String> {

        override fun convert(data: VilkårperiodeType): String = data.tilDbType()
    }

    class SimuleringResponseWriter : JsonWriter<SimuleringJson>()
    class SimuleringResponseReader : JsonReader<SimuleringJson>(SimuleringJson::class)

    class GrunnlagWriter : JsonWriter<Grunnlag>()
    class GrunnlagReader : JsonReader<Grunnlag>(Grunnlag::class)

    class ÅrsakerReader : JsonReader<Årsaker>(Årsaker::class)
    class ÅrsakerWriter : JsonWriter<Årsaker>()

    // Søknad
    class SkjemaBarnetilsynReader : JsonReader<SkjemaBarnetilsyn>(SkjemaBarnetilsyn::class)
    class SkjemaBarnetilsynWriter : JsonWriter<SkjemaBarnetilsyn>()

    class BarnMedBarnepassReader : JsonReader<BarnMedBarnepass>(BarnMedBarnepass::class)
    class BarnMedBarnepassWriter : JsonWriter<BarnMedBarnepass>()

    class VedtaksdataTilsynBarnReader : JsonReader<VedtaksdataTilsynBarn>(VedtaksdataTilsynBarn::class)
    class VedtaksdataTilsynBarnWriter : JsonWriter<VedtaksdataTilsynBarn>()

    class BeregningsresultatTilsynBarnReader :
        JsonReader<BeregningsresultatTilsynBarn>(BeregningsresultatTilsynBarn::class)

    class BeregningsresultatTilsynBarnWriter : JsonWriter<BeregningsresultatTilsynBarn>()

    class ÅrsakAvslagReader : JsonReader<ÅrsakAvslag.Wrapper>(ÅrsakAvslag.Wrapper::class)

    class ÅrsakAvslagWriter : JsonWriter<ÅrsakAvslag.Wrapper>()

    class ÅrsakOpphørReader : JsonReader<ÅrsakOpphør.Wrapper>(ÅrsakOpphør.Wrapper::class)

    class ÅrsakOpphørWriter : JsonWriter<ÅrsakOpphør.Wrapper>()

    class DetaljerVilkårperiodeReader : JsonReader<DelvilkårVilkårperiode>(DelvilkårVilkårperiode::class)

    class DetaljerVilkårperiodeWriter : JsonWriter<DelvilkårVilkårperiode>()

    class VilkårperioderGrunnlagReader : JsonReader<VilkårperioderGrunnlag>(VilkårperioderGrunnlag::class)

    class VilkårperioderGrunnlagWriter : JsonWriter<VilkårperioderGrunnlag>()

    class SkjemaLæremidlerReader : JsonReader<SkjemaLæremidler>(SkjemaLæremidler::class)

    class SkjemaLæremidlerWriter : JsonWriter<SkjemaLæremidler>()
}
