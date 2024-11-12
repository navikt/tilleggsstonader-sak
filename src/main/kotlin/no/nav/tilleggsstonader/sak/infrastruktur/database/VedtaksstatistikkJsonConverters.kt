package no.nav.tilleggsstonader.sak.infrastruktur.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.statistikk.vedtak.AktiviteterDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.BarnDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.MålgrupperDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.UtbetalingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksperioderDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VilkårsvurderingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.ÅrsakOpphørDvh
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
    ÅrsakerAvslagDvhReader(),

    VedtaksperioderDvhWriter(),
    UtbetalingerDvhWriter(),
    BarnDvhWriter(),
    MålgruppeDvhWriter(),
    AktivitetDvhWriter(),
    VilkårsvurderingDvhWriter(),
    ÅrsakerAvslagDvhWriter(),
    ÅrsakerOpphørDvhWriter()
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
private class UtbetalingerDvhWriter : Converter<UtbetalingerDvh.JsonWrapper, PGobject> {
    override fun convert(data: UtbetalingerDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.utbetalinger)
        }
}

@WritingConverter
private class BarnDvhWriter : Converter<BarnDvh.JsonWrapper, PGobject> {
    override fun convert(data: BarnDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.barn)
        }
}

@WritingConverter
private class MålgruppeDvhWriter : Converter<MålgrupperDvh.JsonWrapper, PGobject> {
    override fun convert(data: MålgrupperDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.målgrupper)
        }
}

@WritingConverter
private class AktivitetDvhWriter : Converter<AktiviteterDvh.JsonWrapper, PGobject> {
    override fun convert(data: AktiviteterDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.aktivitet)
        }
}

@WritingConverter
private class VilkårsvurderingDvhWriter : Converter<VilkårsvurderingerDvh.JsonWrapper, PGobject> {
    override fun convert(data: VilkårsvurderingerDvh.JsonWrapper) =
        PGobject().apply {
            type = "json"
            value = objectMapper.writeValueAsString(data.vilkårsvurderinger)
        }
}

private class ÅrsakerAvslagDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakAvslagDvh.JsonWrapper>()
private class ÅrsakerOpphørDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakOpphørDvh.JsonWrapper>()

@ReadingConverter
private class MålgruppeDvhReader : Converter<PGobject, MålgrupperDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        MålgrupperDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
private class AktivitetDvhReader : Converter<PGobject, AktiviteterDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        AktiviteterDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
private class VilkårsvurderingDvhReader : Converter<PGobject, VilkårsvurderingerDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        VilkårsvurderingerDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
private class BarnDvhReader : Converter<PGobject, BarnDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        BarnDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
private class UtbetalingerDvhReader : Converter<PGobject, UtbetalingerDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        UtbetalingerDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

@ReadingConverter
private class VedtaksperioderDvhReader : Converter<PGobject, VedtaksperioderDvh.JsonWrapper> {
    override fun convert(pgObject: PGobject) =
        VedtaksperioderDvh.JsonWrapper(pgObject.value?.let { objectMapper.readValue(it) } ?: emptyList())
}

private class ÅrsakerAvslagDvhReader :
    DatabaseConfiguration.JsonReader<ÅrsakAvslagDvh.JsonWrapper>(ÅrsakAvslagDvh.JsonWrapper::class)
