package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.statistikk.vedtak.StønadstypeDvh.BARNETILSYN
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

// TODO: Vurder om dette bør flyttes til kontrakter

class VedtaksstatistikkDvh(
    val fagsakId: UUID,
    val behandlingID: UUID,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: Adressebeskyttelse?,
    val tidspunktVedtak: ZonedDateTime,
    val målgruppe: List<MålgruppeDvh>,
    val aktivitet: List<AktivitetDvh>,
    val vilkårsvureringer: List<VilkårsvurderingDvh>,
    val person: String,
    val barn: List<BarnDvh>,
    val behandlingType: BehandlingTypeDvh,
    val behandlingÅrsak: BehandlingÅrsakDvh,
    val vedtakResultat: VedtakResultatDvh,
    val vedtaksperioder: VedtaksperioderDvh,
    val utbetalinger: List<UtbetalingDvh>,
    val stønadstype: StønadstypeDvh = BARNETILSYN,
    val kravMottatt: LocalDate,
    val årsakRevurdering: ÅrsakRevurderingDvh? = null,
    val avslagÅrsak: AvslagÅrsakDvh? = null,

    )

enum class StønadstypeDvh {
    BARNETILSYN,
}

data class UtbetalingDvh(
    val gyldigFom: LocalDate,
    val gyldigTom: LocalDate,
    val beløp: Int,
    val satstype: SatstypeDvh = SatstypeDvh.DAGLIG,
)


enum class SatstypeDvh {
    DAGLIG
}

data class VedtaksperioderDvh(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val utgifter: Int,
)

enum class VedtakResultatDvh {
    INNVILGET,
    AVSLÅTT,
    OPPHØRT,
}

enum class BehandlingÅrsakDvh {
    KLAGE,
    NYE_OPPLYSNINGER,
    SØKNAD,
    MIGRERING,
    KORRIGERING_UTEN_BREV,
    PAPIRSØKNAD,
    SATSENDRING,
    MANUELT_OPPRETTET,
}

enum class BehandlingTypeDvh {
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
}

data class BarnDvh(
    val fnr: String,
)

data class AktivitetDvh(
    val type: AktivitetTypeDvh,
    val resultat: ResultatDvh,
)

enum class AktivitetTypeDvh {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
}

data class MålgruppeDvh(
    val type: MålgruppeTypeDvh,
    val resultat: ResultatDvh,
) {
}

enum class ResultatDvh {
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    IKKE_TATT_STILLING_TIL,
    SKAL_IKKE_VURDERES,

    // Todo: Mapping
}

enum class MålgruppeTypeDvh {
    AAP,
    DAGPENGER,
    OMSTILLINGSSTØNAD,
    OVERGANGSSTØNAD,
    NEDSATT_ARBEIDSEVNE,
    UFØRETRYGD,

    // TODO: Mapping fra MålgruppeType
}

data class VilkårsvurderingDvh(
    val resultat: Vilkårsresultat,
    val vilkårId: RegelId,

    // TODO: Mapping fra VilkårDto
)


enum class Adressebeskyttelse {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,

    // TODO: Mapping fra AdressebeskyttelseGradering
}


enum class AvslagÅrsakDvh {
    // TODO når vi får avslag i løsningen
}

enum class ÅrsakRevurderingDvh {
    // TODO når vi får revurdering i løsningen
}
