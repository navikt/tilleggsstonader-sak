package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.felles.domain.FaktaGrunnlagId
import no.nav.tilleggsstonader.sak.felles.domain.RevurderFra
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.time.LocalDate
import java.util.UUID

/**
 * Spring data jdbc støtter ikke value classes for primary keys, dvs @Id-markerte felter
 */
object IdConverters {
    @WritingConverter
    abstract class ValueClassWriter<T, R>(
        val convert: (T) -> R,
    ) : Converter<T, R> {
        override fun convert(valueClass: T & Any): R = this.convert.invoke(valueClass)
    }

    @ReadingConverter
    abstract class ValueClassReader<T : Any, R : Any>(
        val convert: (R) -> T,
    ) : Converter<R, T> {
        override fun convert(id: R): T = this.convert.invoke(id)
    }

    @WritingConverter
    abstract class ValueClassUUIDWriter<T>(
        convert: (T) -> UUID,
    ) : ValueClassWriter<T, UUID>(convert)

    @ReadingConverter
    abstract class ValueClassUUIDReader<T : Any>(
        convert: (UUID) -> T,
    ) : ValueClassReader<T, UUID>(convert)

    class FagsakPersonIdWritingConverter : ValueClassUUIDWriter<FagsakPersonId>({ it.id })

    class FagsakPersonIdReaderConverter : ValueClassUUIDReader<FagsakPersonId>({ FagsakPersonId(it) })

    class FagsakIdWritingConverter : ValueClassUUIDWriter<FagsakId>({ it.id })

    class FagsakIdReaderConverter : ValueClassUUIDReader<FagsakId>({ FagsakId(it) })

    class BehandlingIdWritingConverter : ValueClassUUIDWriter<BehandlingId>({ it.id })

    class BehandlingIdReaderConverter : ValueClassUUIDReader<BehandlingId>({ BehandlingId(it) })

    class BarnIdWritingConverter : ValueClassUUIDWriter<BarnId>({ it.id })

    class BarnIdReaderConverter : ValueClassUUIDReader<BarnId>({ BarnId(it) })

    class VilkårIdWritingConverter : ValueClassUUIDWriter<VilkårId>({ it.id })

    class VilkårIdReaderConverter : ValueClassUUIDReader<VilkårId>({ VilkårId(it) })

    class FaktaGrunnlagIdWritingConverter : ValueClassUUIDWriter<FaktaGrunnlagId>({ it.id })

    class FaktaGrunnlagIdReaderConverter : ValueClassUUIDReader<FaktaGrunnlagId>({ FaktaGrunnlagId(it) })

    class RevurderFraWritingConverter : ValueClassWriter<RevurderFra, LocalDate>({ it.dato })

    class RevurderFraReaderConverter : ValueClassReader<RevurderFra, LocalDate>({ RevurderFra(it) })

    val alleValueClassConverters =
        listOf(
            FagsakPersonIdWritingConverter(),
            FagsakPersonIdReaderConverter(),
            FagsakIdWritingConverter(),
            FagsakIdReaderConverter(),
            BehandlingIdWritingConverter(),
            BehandlingIdReaderConverter(),
            BarnIdWritingConverter(),
            BarnIdReaderConverter(),
            VilkårIdWritingConverter(),
            VilkårIdReaderConverter(),
            FaktaGrunnlagIdWritingConverter(),
            FaktaGrunnlagIdReaderConverter(),
            RevurderFraWritingConverter(),
            RevurderFraReaderConverter(),
        )
}
