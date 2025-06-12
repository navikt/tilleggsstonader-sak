package no.nav.tilleggsstonader.sak.infrastruktur.database

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.VedtaksperioderDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakAvslagDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.ÅrsakOpphørDvh
import org.postgresql.util.PGobject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

val alleVedtaksstatistikkJsonConverters =
    listOf(
        ÅrsakerAvslagDvhReader(),
        ÅrsakerAvslagDvhWriter(),
        ÅrsakerOpphørDvhWriter(),
        VedtaksperioderDvhWriter(),
        UtbetalingerDvhWriter(),
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

private class ÅrsakerAvslagDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakAvslagDvh.JsonWrapper>()

private class ÅrsakerOpphørDvhWriter : DatabaseConfiguration.JsonWriter<ÅrsakOpphørDvh.JsonWrapper>()

private class ÅrsakerAvslagDvhReader : DatabaseConfiguration.JsonReader<ÅrsakAvslagDvh.JsonWrapper>(ÅrsakAvslagDvh.JsonWrapper::class)
