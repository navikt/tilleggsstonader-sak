package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.validerBehandlingIdErLik
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.AktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeValideringUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilSortertDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerKanLeggeTilMålgruppeManuelt
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerEndrePeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerNyPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeRevurderFraValidering.validerSlettPeriodeRevurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Stønadsperiodestatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.evaulerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDomenetype
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class VilkårperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
    private val aktivitetService: AktivitetService,
    private val ytelseService: YtelseService,
    private val søknadService: SøknadService,
    private val tilgangService: TilgangService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentVilkårperioder(behandlingId: BehandlingId): Vilkårperioder {
        val vilkårsperioder = vilkårperiodeRepository.findByBehandlingId(behandlingId)
            .sortedWith(compareBy({ it.fom }, { it.tom }))

        return Vilkårperioder(
            målgrupper = finnPerioder<MålgruppeType>(vilkårsperioder),
            aktiviteter = finnPerioder<AktivitetType>(vilkårsperioder),
        )
    }

    fun hentVilkårperioderResponse(behandlingId: BehandlingId): VilkårperioderResponse {
        val grunnlagsdataVilkårsperioder = hentEllerOpprettGrunnlag(behandlingId)

        return VilkårperioderResponse(
            vilkårperioder = hentVilkårperioderDto(behandlingId),
            grunnlag = grunnlagsdataVilkårsperioder?.tilDto(),
        )
    }

    @Transactional
    fun oppdaterGrunnlag(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke oppdatere grunnlag når behandlingen er låst"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke oppdatere grunnlag når behandlingen er i annet steg enn vilkår."
        }

        val eksisterendeGrunnlag = vilkårperioderGrunnlagRepository.findByIdOrThrow(behandlingId)

        val eksisterendeHentetFom = eksisterendeGrunnlag.grunnlag.hentetInformasjon.fom
        val tom = YearMonth.now().plusYears(1).atEndOfMonth()

        val nyGrunnlagsdata = hentGrunnlagsdata(behandlingId, eksisterendeHentetFom, tom)
        vilkårperioderGrunnlagRepository.update(eksisterendeGrunnlag.copy(grunnlag = nyGrunnlagsdata))
        val tidSidenForrigeHenting =
            ChronoUnit.HOURS.between(nyGrunnlagsdata.hentetInformasjon.tidspunktHentet, LocalDateTime.now())
        logger.info("Oppdatert grunnlagsdata for behandling=$behandlingId timerSidenForrige=$tidSidenForrigeHenting")
    }

    private fun hentEllerOpprettGrunnlag(behandlingId: BehandlingId): VilkårperioderGrunnlag? {
        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)?.grunnlag

        return if (grunnlag != null) {
            grunnlag
        } else if (behandlingErLåstForVidereRedigering(behandlingId)) {
            null
        } else {
            opprettGrunnlagsdata(behandlingId).grunnlag
        }
    }

    private fun opprettGrunnlagsdata(behandlingId: BehandlingId): VilkårperioderGrunnlagDomain {
        brukerfeilHvisIkke(tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
            "Behandlingen er ikke påbegynt. Kan ikke opprette vilkårperiode hvis man ikke er saksbehandler"
        }

        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)
        val utgangspunktDato = søknad?.mottattTidspunkt?.toLocalDate() ?: LocalDate.now()

        val fom = YearMonth.from(utgangspunktDato).minusMonths(3).atDay(1)
        val tom = YearMonth.from(utgangspunktDato).plusYears(1).atEndOfMonth()

        val grunnlag = hentGrunnlagsdata(behandlingId, fom, tom)
        return vilkårperioderGrunnlagRepository.insert(
            VilkårperioderGrunnlagDomain(
                behandlingId = behandlingId,
                grunnlag = grunnlag,
            ),
        )
    }

    private fun hentGrunnlagsdata(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperioderGrunnlag {
        return VilkårperioderGrunnlag(
            aktivitet = hentGrunnlagAktvititet(behandlingId, fom, tom),
            ytelse = hentGrunnlagYtelse(behandlingId, fom, tom),
            hentetInformasjon = HentetInformasjon(
                fom = fom,
                tom = tom,
                tidspunktHentet = LocalDateTime.now(),
            ),
        )
    }

    private fun hentGrunnlagAktvititet(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ) = GrunnlagAktivitet(
        aktiviteter = aktivitetService.hentAktiviteterForGrunnlagsdata(
            ident = behandlingService.hentSaksbehandling(behandlingId).ident,
            fom = fom,
            tom = tom,
        ),
    )

    private fun hentGrunnlagYtelse(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): GrunnlagYtelse {
        val ytelserFraRegister = ytelseService.hentYtelseForGrunnlag(behandlingId = behandlingId, fom = fom, tom = tom)

        return GrunnlagYtelse(
            perioder = ytelserFraRegister.perioder
                .filter { it.aapErFerdigAvklart != true }
                .map {
                    PeriodeGrunnlagYtelse(
                        type = it.type,
                        fom = it.fom,
                        tom = it.tom,
                        ensligForsørgerStønadstype = it.ensligForsørgerStønadstype?.tilDomenetype(),
                    )
                },
        )
    }

    fun hentVilkårperioderDto(behandlingId: BehandlingId): VilkårperioderDto {
        return hentVilkårperioder(behandlingId).tilDto()
    }

    private inline fun <reified T : VilkårperiodeType> finnPerioder(
        vilkårsperioder: List<Vilkårperiode>,
    ) = vilkårsperioder.filter { it.type is T }

    fun validerOgLagResponse(behandlingId: BehandlingId, periode: Vilkårperiode? = null): LagreVilkårperiodeResponse {
        val valideringsresultat = validerStønadsperioder(behandlingId)

        return LagreVilkårperiodeResponse(
            periode = periode?.tilDto(),
            stønadsperiodeStatus = if (valideringsresultat.isSuccess) Stønadsperiodestatus.OK else Stønadsperiodestatus.FEIL,
            stønadsperiodeFeil = valideringsresultat.exceptionOrNull()?.message,
        )
    }

    @Transactional
    fun opprettVilkårperiode(vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerNyPeriodeRevurdering(behandling, vilkårperiode)

        if (vilkårperiode.type is MålgruppeType) {
            validerKanLeggeTilMålgruppeManuelt(behandling.stønadstype, vilkårperiode.type)
        }
        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)

        val resultatEvaluering = evaulerVilkårperiode(vilkårperiode.type, vilkårperiode.delvilkår)

        return vilkårperiodeRepository.insert(
            Vilkårperiode(
                behandlingId = vilkårperiode.behandlingId,
                fom = vilkårperiode.fom,
                tom = vilkårperiode.tom,
                type = vilkårperiode.type,
                delvilkår = resultatEvaluering.delvilkår,
                begrunnelse = vilkårperiode.begrunnelse,
                resultat = resultatEvaluering.resultat,
                aktivitetsdager = vilkårperiode.aktivitetsdager,
                kilde = KildeVilkårsperiode.MANUELL,
                status = Vilkårstatus.NY,
            ),
        )
    }

    private fun validerBehandling(behandling: Saksbehandling) {
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke opprette eller endre vilkårperiode når behandling er låst for videre redigering"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke opprette eller endre vilkårperiode når behandling ikke er på steg ${StegType.INNGANGSVILKÅR}"
        }
    }

    private fun validerAktivitetsdager(vilkårPeriodeType: VilkårperiodeType, aktivitetsdager: Int?) {
        if (vilkårPeriodeType is AktivitetType) {
            brukerfeilHvis(vilkårPeriodeType != AktivitetType.INGEN_AKTIVITET && aktivitetsdager !in 1..5) {
                "Aktivitetsdager må være et heltall mellom 1 og 5"
            }
        } else if (vilkårPeriodeType is MålgruppeType) {
            brukerfeilHvisIkke(aktivitetsdager == null) { "Kan ikke registrere aktivitetsdager på målgrupper" }
        }
    }

    private fun validerStønadsperioder(behandlingId: BehandlingId): Result<Unit> {
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertDto()
        val vilkårperioder = hentVilkårperioder(behandlingId)

        return kotlin.runCatching {
            StønadsperiodeValideringUtil.validerStønadsperioderVedEndringAvVilkårperiode(
                stønadsperioder,
                vilkårperioder.tilDto(),
            )
        }
    }

    fun oppdaterVilkårperiode(id: UUID, vilkårperiode: LagreVilkårperiode): Vilkårperiode {
        val eksisterendeVilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        val behandling = behandlingService.hentSaksbehandling(eksisterendeVilkårperiode.behandlingId)
        validerBehandlingIdErLik(vilkårperiode.behandlingId, eksisterendeVilkårperiode.behandlingId)
        validerBehandling(behandling)

        validerAktivitetsdager(vilkårPeriodeType = vilkårperiode.type, aktivitetsdager = vilkårperiode.aktivitetsdager)

        val resultatEvaluering = evaulerVilkårperiode(eksisterendeVilkårperiode.type, vilkårperiode.delvilkår)
        val nyStatus = if (eksisterendeVilkårperiode.status == Vilkårstatus.NY) {
            Vilkårstatus.NY
        } else {
            Vilkårstatus.ENDRET
        }
        val oppdatert = when (eksisterendeVilkårperiode.kilde) {
            KildeVilkårsperiode.MANUELL -> {
                eksisterendeVilkårperiode.copy(
                    begrunnelse = vilkårperiode.begrunnelse,
                    fom = vilkårperiode.fom,
                    tom = vilkårperiode.tom,
                    delvilkår = resultatEvaluering.delvilkår,
                    aktivitetsdager = vilkårperiode.aktivitetsdager,
                    resultat = resultatEvaluering.resultat,
                    status = nyStatus,
                )
            }

            KildeVilkårsperiode.SYSTEM -> {
                validerIkkeEndretFomTomForSystem(eksisterendeVilkårperiode, vilkårperiode)
                eksisterendeVilkårperiode.copy(
                    begrunnelse = vilkårperiode.begrunnelse,
                    delvilkår = resultatEvaluering.delvilkår,
                    resultat = resultatEvaluering.resultat,
                    status = nyStatus,
                )
            }
        }
        validerEndrePeriodeRevurdering(behandling, eksisterendeVilkårperiode, oppdatert)
        return vilkårperiodeRepository.update(oppdatert)
    }

    private fun validerIkkeEndretFomTomForSystem(
        vilkårperiode: Vilkårperiode,
        oppdaterVilkårperiode: LagreVilkårperiode,
    ) {
        feilHvis(vilkårperiode.fom != oppdaterVilkårperiode.fom) {
            "Kan ikke oppdatere fom når kilde=${KildeVilkårsperiode.SYSTEM}"
        }
        feilHvis(vilkårperiode.tom != oppdaterVilkårperiode.tom) {
            "Kan ikke oppdatere tom når kilde=${KildeVilkårsperiode.SYSTEM}"
        }
    }

    fun slettVilkårperiode(id: UUID, slettVikårperiode: SlettVikårperiode): Vilkårperiode? {
        val vilkårperiode = vilkårperiodeRepository.findByIdOrThrow(id)

        validerBehandlingIdErLik(slettVikårperiode.behandlingId, vilkårperiode.behandlingId)

        val behandling = behandlingService.hentSaksbehandling(vilkårperiode.behandlingId)
        validerBehandling(behandling)
        validerSlettPeriodeRevurdering(behandling, vilkårperiode)

        if (vilkårperiode.kanSlettesPermanent()) {
            vilkårperiodeRepository.deleteById(vilkårperiode.id)
            return null
        } else {
            return vilkårperiodeRepository.update(
                vilkårperiode.copy(
                    resultat = ResultatVilkårperiode.SLETTET,
                    slettetKommentar = slettVikårperiode.kommentar,
                    status = Vilkårstatus.SLETTET,
                ),
            )
        }
    }

    fun gjenbrukVilkårperioder(forrigeBehandlingId: BehandlingId, nyBehandlingId: BehandlingId) {
        val eksisterendeVilkårperioder =
            vilkårperiodeRepository.findByBehandlingIdAndResultatNot(forrigeBehandlingId, ResultatVilkårperiode.SLETTET)

        val kopiertePerioderMedReferanse = eksisterendeVilkårperioder.map { it.kopierTilBehandling(nyBehandlingId) }
        vilkårperiodeRepository.insertAll(kopiertePerioderMedReferanse)
    }

    private fun behandlingErLåstForVidereRedigering(behandlingId: BehandlingId) =
        behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()
}
