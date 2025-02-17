package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.vilkårperiodetyper
import kotlin.io.path.name

object FaktaVurderingerJsonFilesUtil {
    val jsonFiler =
        FileUtil.listDir("vilkår/vilkårperiode").flatMap { dir ->
            val dirPath = "vilkår/vilkårperiode/${dir.fileName.fileName}"
            FileUtil
                .listFiles(dirPath)
                .map { JsonFil(dirPath, it.fileName.name) }
        }

    /**
     * Brukes også som dir-name for json-filer
     */
    fun Stønadstype.tilTypeFaktaOgVurderingSuffix() =
        when (this) {
            Stønadstype.BARNETILSYN -> "TILSYN_BARN"
            else -> name
        }

    /**
     * @param dirPath vilkår/vilkårperiode/TILSYN_BARN
     * @param fileName UTDANNING_TILSYN_BARN.json
     */
    data class JsonFil(
        val dirPath: String,
        val fileName: String,
    ) {
        /**
         * Ex vilkår/vilkårperiode/TILSYN_BARN/UTDANNING_TILSYN_BARN.json
         */
        fun fullPath() = "$dirPath/$fileName"

        /**
         * Forventer at alle mapper har strukturen
         * /<Stønadstype>/<Type>_<Stønadstype>.json, der vi henter ut <Type>
         */
        fun typeVilkårperiode(): VilkårperiodeType {
            val stønadstype = dirPath.substringAfterLast("/")
            val type = fileName.substringBefore("_$stønadstype")
            return vilkårperiodetyper[type]
                ?: error("Finner ikke VilkårperiodeType=$type")
        }

        fun typeFaktaOgVurdering(): TypeFaktaOgVurdering {
            val type = fileName.substringBefore(".")
            return alleEnumTyperFaktaOgVurdering
                .single { it.second.enumName() == type }
                .second
        }

        override fun toString(): String = fileName
    }
}
