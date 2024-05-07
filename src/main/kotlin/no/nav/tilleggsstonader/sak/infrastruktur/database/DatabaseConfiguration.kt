package no.nav.tilleggsstonader.sak.infrastruktur.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SkjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.BarnDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.UtbetalingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksperioderDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VilkårsvurderingDvh
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtaksdataBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtaksdataTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Årsaker
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
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

                BeriketSimuleringsresultatWriter(),
                BeriketSimuleringsresultatReader(),

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
                VedtaksdataBeregningsresultatReader(),
                VedtaksdataBeregningsresultatWriter(),

                TilVilkårperiodeTypeConverter(),
                VilkårperiodeTypeTilStringConverter(),

                DetaljerVilkårperiodeReader(),
                DetaljerVilkårperiodeWriter(),

                VedtaksperioderDvhTilPgobjektConverter(),
                UtbetalingerDvhTilPgobjektConverter(),
                BarnDvhTilPgobjektConverter(),
                MålgruppeDvhTilPgobjektConverter(),
                AktivitetDvhTilPgobjektConverter(),
                VilkårsvurderingDvhTilPgobjektConverter(),
            ),
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

    @WritingConverter
    class VedtaksperioderDvhTilPgobjektConverter : Converter<VedtaksperioderDvh.JsonWrapper, PGobject> {
        override fun convert(data: VedtaksperioderDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.vedtaksperioder)
            }
    }

    @WritingConverter
    class UtbetalingerDvhTilPgobjektConverter : Converter<UtbetalingerDvh.JsonWrapper, PGobject> {
        override fun convert(data: UtbetalingerDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.utbetalinger)
            }
    }

    @WritingConverter
    class BarnDvhTilPgobjektConverter : Converter<BarnDvh.JsonWrapper, PGobject> {
        override fun convert(data: BarnDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.barn)
            }
    }

    @WritingConverter
    class MålgruppeDvhTilPgobjektConverter : Converter<MålgruppeDvh.JsonWrapper, PGobject> {
        override fun convert(data: MålgruppeDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.målgruppe)
            }
    }

    @WritingConverter
    class AktivitetDvhTilPgobjektConverter : Converter<AktivitetDvh.JsonWrapper, PGobject> {
        override fun convert(data: AktivitetDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.aktivitet)
            }
    }

    @WritingConverter
    class VilkårsvurderingDvhTilPgobjektConverter : Converter<VilkårsvurderingDvh.JsonWrapper, PGobject> {
        override fun convert(data: VilkårsvurderingDvh.JsonWrapper) =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(data.vilkårsvurderinger)
            }
    }

    class BeriketSimuleringsresultatWriter : JsonWriter<BeriketSimuleringsresultat>()
    class BeriketSimuleringsresultatReader : JsonReader<BeriketSimuleringsresultat>(BeriketSimuleringsresultat::class)

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

    class VedtaksdataBeregningsresultatReader :
        JsonReader<VedtaksdataBeregningsresultat>(VedtaksdataBeregningsresultat::class)

    class VedtaksdataBeregningsresultatWriter : JsonWriter<VedtaksdataBeregningsresultat>()

    class DetaljerVilkårperiodeReader : JsonReader<DelvilkårVilkårperiode>(DelvilkårVilkårperiode::class)

    class DetaljerVilkårperiodeWriter : JsonWriter<DelvilkårVilkårperiode>()
}
