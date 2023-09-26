package no.nav.tilleggsstonader.sak.infrastruktur.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.PropertiesWrapperTilStringConverter
import no.nav.familie.prosessering.StringTilPropertiesWrapperConverter
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.vedtak.domain.Årsaker
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårsvurderingWrapper
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

                PGobjectTilBeriketSimuleringsresultat(),
                BeriketSimuleringsresultatTilPGobjectConverter(),

                PGobjectTilTotrinnsÅrsaker(),
                TotrinnsÅrsakerTilPGobjectConverter(),
            ),
        )
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

    @ReadingConverter
    class PGobjectTilDelvilkårConverter : Converter<PGobject, DelvilkårsvurderingWrapper> {

        override fun convert(pGobject: PGobject): DelvilkårsvurderingWrapper {
            return DelvilkårsvurderingWrapper(pGobject.value?.let { objectMapper.readValue(it) } ?: emptyList())
        }
    }

    @WritingConverter
    class DelvilkårTilPGobjectConverter : Converter<DelvilkårsvurderingWrapper, PGobject> {

        override fun convert(delvilkårsvurdering: DelvilkårsvurderingWrapper): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(delvilkårsvurdering.delvilkårsvurderinger)
            }
    }

    @ReadingConverter
    class PGobjectTilBeriketSimuleringsresultat : Converter<PGobject, BeriketSimuleringsresultat?> {

        override fun convert(pGobject: PGobject): BeriketSimuleringsresultat? {
            return pGobject.value?.let { objectMapper.readValue(it) }
        }
    }

    @WritingConverter
    class BeriketSimuleringsresultatTilPGobjectConverter : Converter<BeriketSimuleringsresultat, PGobject> {

        override fun convert(simuleringsresultat: BeriketSimuleringsresultat): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(simuleringsresultat)
            }
    }

    @ReadingConverter
    class PGobjectTilTotrinnsÅrsaker : Converter<PGobject, Årsaker?> {

        override fun convert(pGobject: PGobject): Årsaker? {
            return pGobject.value?.let { objectMapper.readValue(it) }
        }
    }

    @WritingConverter
    class TotrinnsÅrsakerTilPGobjectConverter : Converter<Årsaker, PGobject> {

        override fun convert(årsaker: Årsaker): PGobject =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(årsaker)
            }
    }
}
