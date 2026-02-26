package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service

@Service
class MittNavVarselService(
    private val behandlingService: BehandlingService,
    private val kjørelisteService: KjørelisteService,
    private val vedtakRepository: VedtakRepository,
) {
    fun sendVarselOmTilgjengeligKjøreliste(behandlingId: BehandlingId): List<Int> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val rammevedtak =
            vedtakRepository
                .findByIdOrThrow(behandling.id)
                .withTypeOrThrow<`InnvilgelseEllerOpphørDagligReise`>()
                .data.rammevedtakPrivatBil

        val kjørelister = kjørelisteService.hentForFagsakId(behandling.fagsakId)

        val kjørelisteMap = kjørelister.associateBy { it.data.reiseId }

        return rammevedtak
            ?.reiser
            ?.flatMap { reise ->
                val rammevedtakUker = reise.uker.map { it.grunnlag.fom.ukenummer() }
                val kjørelisteUker =
                    kjørelisteMap[reise.reiseId]?.data?.reisedager?.map { it.dato.ukenummer() } ?: emptyList()

                return rammevedtakUker.filter { it !in kjørelisteUker }
            }
            ?: emptyList()
    }
}
