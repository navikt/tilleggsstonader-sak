package no.nav.tilleggsstonader.sak.cucumber

import io.cucumber.datatable.DataTable

fun <T> DataTable.mapRad(mapper: (Map<String, String>) -> T): List<T> {
    return this.asMaps().mapIndexed { index, row ->
        try {
            mapper(row)
        } catch (e: Exception) {
            throw RuntimeException("Feilet parsing av rad $index", e)
        }
    }
}
