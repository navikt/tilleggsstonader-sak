package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.vedtak.forslag.ForeslåVedtaksperiodeFraVilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StønadsperiodeService(
    private val behandlingService: BehandlingService,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeService: VilkårperiodeService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {
    fun hentStønadsperioder(behandlingId: BehandlingId): List<StønadsperiodeDto> =
        stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()

    @Transactional
    fun lagreStønadsperioder(
        behandlingId: BehandlingId,
        stønadsperioder: List<StønadsperiodeDto>,
    ): List<StønadsperiodeDto> {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        validerBehandling(behandling)
        validerStønadsperioder(behandlingId, stønadsperioder)

        val tidligereStønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId)

        slettStønadsperioder(behandling, tidligereStønadsperioder, stønadsperioder)
        val oppdaterteStønadsperioder =
            oppdaterEksisterendeStønadsperioder(behandling, tidligereStønadsperioder, stønadsperioder)
        val nyeStønadsperioder = leggTilNyeStønadsperioder(behandling, stønadsperioder, tidligereStønadsperioder)
        return (nyeStønadsperioder + oppdaterteStønadsperioder).tilSortertDto()
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        behandling.status.validerKanBehandlingRedigeres()
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke lagre stønadsperioder når behandlingen er på steg=${behandling.steg}"
        }
    }

    private fun slettStønadsperioder(
        behandling: Saksbehandling,
        tidligereStønadsperioder: List<Stønadsperiode>,
        stønadsperioder: List<StønadsperiodeDto>,
    ) {
        val gjenståendePerioder = stønadsperioder.mapNotNull { it.id }.toSet()
        val perioderTilSletting = tidligereStønadsperioder.filterNot { gjenståendePerioder.contains(it.id) }
        perioderTilSletting.forEach { validerSlettPeriodeRevurdering(behandling, it) }
        stønadsperiodeRepository.deleteAllById(perioderTilSletting.map { it.id })
    }

    private fun oppdaterEksisterendeStønadsperioder(
        behandling: Saksbehandling,
        tidligereStønadsperioder: List<Stønadsperiode>,
        stønadsperioder: List<StønadsperiodeDto>,
    ): List<Stønadsperiode> {
        val stønadsperioderPåId =
            stønadsperioder.mapNotNull { periode -> periode.id?.let { id -> id to periode } }.toMap()
        val perioderTilOppdatering =
            tidligereStønadsperioder.mapNotNull { tidligereStønadsperiode ->
                stønadsperioderPåId[tidligereStønadsperiode.id]?.let {
                    tidligereStønadsperiode
                        .copy(
                            fom = it.fom,
                            tom = it.tom,
                            målgruppe = it.målgruppe,
                            aktivitet = it.aktivitet,
                        ).medNyStatus(tidligereStønadsperiode)
                        .apply {
                            validerEndrePeriodeRevurdering(behandling, tidligereStønadsperiode, this)
                        }
                }
            }
        return stønadsperiodeRepository.updateAll(perioderTilOppdatering)
    }

    private fun Stønadsperiode.medNyStatus(tidligereStønadsperiode: Stønadsperiode): Stønadsperiode {
        val nyStatus =
            when (tidligereStønadsperiode.status) {
                StønadsperiodeStatus.UENDRET -> utledStatusBasertPåEndring(this, tidligereStønadsperiode)
                else -> this.status
            }
        return this.copy(status = nyStatus)
    }

    private fun utledStatusBasertPåEndring(
        oppdatertPeriode: Stønadsperiode,
        eksisterendePeriode: Stønadsperiode,
    ): StønadsperiodeStatus {
        if (eksisterendePeriode.fom == oppdatertPeriode.fom &&
            eksisterendePeriode.tom == oppdatertPeriode.tom &&
            eksisterendePeriode.målgruppe == oppdatertPeriode.målgruppe &&
            eksisterendePeriode.aktivitet == oppdatertPeriode.aktivitet
        ) {
            return StønadsperiodeStatus.UENDRET
        }

        return StønadsperiodeStatus.ENDRET
    }

    private fun leggTilNyeStønadsperioder(
        behandling: Saksbehandling,
        stønadsperioder: List<StønadsperiodeDto>,
        tidligereStønadsperiode: List<Stønadsperiode>,
    ): List<Stønadsperiode> {
        val tidligereStønadsperiodeIder = tidligereStønadsperiode.map { it.id }.toSet()
        val nyeStønadsperioder =
            stønadsperioder.filterNot { tidligereStønadsperiodeIder.contains(it.id) }.map {
                feilHvis(it.id != null) {
                    "Kan ikke oppdatere stønadsperiode=${it.id} som ikke finnes fra før"
                }
                Stønadsperiode(
                    id = UUID.randomUUID(),
                    behandlingId = behandling.id,
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                    status = StønadsperiodeStatus.NY,
                )
            }
        nyeStønadsperioder.forEach {
            validerNyPeriodeRevurdering(behandling, it)
        }

        return stønadsperiodeRepository.insertAll(nyeStønadsperioder)
    }

    fun validerStønadsperioder(behandlingId: BehandlingId) {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        validerStønadsperioder(behandlingId, stønadsperioder)
    }

    fun validerStønadsperioder(
        behandlingId: BehandlingId,
        stønadsperioder: List<StønadsperiodeDto>,
    ) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val fødselsdato =
            grunnlagsdataService
                .hentGrunnlagsdata(behandlingId)
                .grunnlag.fødsel
                ?.fødselsdatoEller1JanForFødselsår()

        StønadsperiodeValidering.valider(stønadsperioder, vilkårperioder, fødselsdato)
    }

    fun gjenbrukStønadsperioder(
        forrigeIverksatteBehandlingId: BehandlingId,
        nyBehandlingId: BehandlingId,
    ) {
        val eksisterendeStønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(forrigeIverksatteBehandlingId)
        stønadsperiodeRepository.insertAll(eksisterendeStønadsperioder.map { it.kopierTilBehandling(nyBehandlingId) })
    }

    fun foreslåPerioder(behandlingId: BehandlingId): List<StønadsperiodeDto> {
        feilHvis(detFinnesStønadsperioder(behandlingId)) {
            "Det finnes allerede lagrede perioder med overlapp på denne behandlingen."
        }

        val vilkårsperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)

        return ForeslåVedtaksperiodeFraVilkårperioder
            .foreslåVedtaksperioder(
                vilkårperioder = vilkårsperioder,
            ).map {
                StønadsperiodeDto(
                    id = null,
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                    status = null,
                )
            }
    }

    private fun detFinnesStønadsperioder(behandlingId: BehandlingId) =
        stønadsperiodeRepository.findAllByBehandlingId(behandlingId).isNotEmpty()
}
