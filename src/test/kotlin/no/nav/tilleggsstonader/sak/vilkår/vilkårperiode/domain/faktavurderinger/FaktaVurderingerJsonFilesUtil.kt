package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.FileUtil
import kotlin.io.path.name

object FaktaVurderingerJsonFilesUtil {

    val jsonFiler = FileUtil.listDir("vilkår/vilkårperiode").flatMap { dir ->
        val dirPath = "vilkår/vilkårperiode/${dir.fileName.fileName}"
        FileUtil.listFiles(dirPath)
            .map { JsonFil(dirPath, it.fileName.name) }
    }

    /**
     * Brukes også som dir-name for json-filer
     */
    fun Stønadstype.tilTypeFaktaOgVurderingSuffix() = when (this) {
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
        fun typeVilkårperiode(): String {
            val stønadstype = dirPath.substringAfterLast("/")
            return fileName.substringBefore("_$stønadstype")
        }

        override fun toString(): String {
            return fileName
        }
    }
}
