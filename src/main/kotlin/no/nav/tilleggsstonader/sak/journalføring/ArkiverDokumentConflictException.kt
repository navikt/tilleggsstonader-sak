package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse

class ArkiverDokumentConflictException(val response: ArkiverDokumentResponse) : RuntimeException()
