package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.util.UUID

/**
 * Spring data jdbc støtter ikke value classes for primary keys, dvs @Id-markerte felter
 */
object IdConverters {

    @WritingConverter
    abstract class ValueClassWriter<T>(val convert: (T) -> UUID) : Converter<T, UUID> {
        override fun convert(valueClass: T & Any): UUID = this.convert.invoke(valueClass)
    }

    @ReadingConverter
    abstract class ValueClassReader<T : Any>(val convert: (UUID) -> T) : Converter<UUID, T> {
        override fun convert(id: UUID): T = this.convert.invoke(id)
    }

    class FagsakPersonIdWritingConverter : ValueClassWriter<FagsakPersonId>({ it.id })
    class FagsakPersonIdReaderConverter : ValueClassReader<FagsakPersonId>({ FagsakPersonId(it) })

    class FagsakIdWritingConverter : ValueClassWriter<FagsakId>({ it.id })
    class FagsakIdReaderConverter : ValueClassReader<FagsakId>({ FagsakId(it) })

    class BehandlingIdWritingConverter : ValueClassWriter<BehandlingId>({ it.id })
    class BehandlingIdReaderConverter : ValueClassReader<BehandlingId>({ BehandlingId(it) })

    class BarnIdWritingConverter : ValueClassWriter<BarnId>({ it.id })
    class BarnIdReaderConverter : ValueClassReader<BarnId>({ BarnId(it) })

    class VilkårIdWritingConverter : ValueClassWriter<VilkårId>({ it.id })
    class VilkårIdReaderConverter : ValueClassReader<VilkårId>({ VilkårId(it) })

    val alleValueClassConverters = listOf(
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
    )
}
