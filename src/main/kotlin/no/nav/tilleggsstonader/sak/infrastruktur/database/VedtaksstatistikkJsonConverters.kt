package no.nav.tilleggsstonader.sak.infrastruktur.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktivitetDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.BarnDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgruppeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.UtbetalingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksperioderDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VilkårsvurderingDvh
import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

val alleVedtaksstatistikkJsonConverters = listOf(
    MålgruppeDvhReader(),
    AktivitetDvhReader(),
    VilkårsvurderingDvhReader(),
    BarnDvhReader(),
    UtbetalingerDvhReader(),
    VedtaksperioderDvhReader(),

    VedtaksperioderDvhWriter(),
    UtbetalingerDvhWriter(),
    BarnDvhWriter(),
    MålgruppeDvhWriter(),
    AktivitetDvhWriter(),
    VilkårsvurderingDvhWriter(),
)

@WritingConverter
private class VedtaksperioderDvhWriter : Converter<VedtaksperioderDvh.JsonWrapper, PGobject> {
    override fun convert(data: VedtaksperioderDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.vedtaksperioder)
        }
}

@WritingConverter
class UtbetalingerDvhWriter : Converter<UtbetalingerDvh.JsonWrapper, PGobject> {
    override fun convert(data: UtbetalingerDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.utbetalinger)
        }
}

@WritingConverter
class BarnDvhWriter : Converter<BarnDvh.JsonWrapper, PGobject> {
    override fun convert(data: BarnDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.barn)
        }
}

@WritingConverter
class MålgruppeDvhWriter : Converter<MålgruppeDvh.JsonWrapper, PGobject> {
    override fun convert(data: MålgruppeDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.målgruppe)
        }
}

@WritingConverter
class AktivitetDvhWriter : Converter<AktivitetDvh.JsonWrapper, PGobject> {
    override fun convert(data: AktivitetDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.aktivitet)
        }
}

@WritingConverter
class VilkårsvurderingDvhWriter : Converter<VilkårsvurderingDvh.JsonWrapper, PGobject> {
    override fun convert(data: VilkårsvurderingDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.vilkårsvurderinger)
        }
}

@ReadingConverter
class MålgruppeDvhReader : Converter<PGobject, MålgruppeDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        MålgruppeDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
class AktivitetDvhReader : Converter<PGobject, AktivitetDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        AktivitetDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
class VilkårsvurderingDvhReader : Converter<PGobject, VilkårsvurderingDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        VilkårsvurderingDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
class BarnDvhReader : Converter<PGobject, BarnDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        BarnDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
class UtbetalingerDvhReader : Converter<PGobject, UtbetalingerDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        UtbetalingerDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
class VedtaksperioderDvhReader : Converter<PGobject, VedtaksperioderDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        VedtaksperioderDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}
