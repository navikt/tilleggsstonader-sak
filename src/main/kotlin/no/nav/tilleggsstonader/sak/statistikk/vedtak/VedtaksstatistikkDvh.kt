package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.statistikk.vedtak.StønadstypeDvh.BARNETILSYN
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

// TODO: Vurder om dette bør flyttes til kontrakter

class VedtaksstatistikkDvh(
    val fagsakId: UUID,
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val relatertBehandlingId: Long?, // Ekstern behandlingsid på relatert behandling
    val adressebeskyttelse: AdressebeskyttelseDvh,
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
    DAGLIG,
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
    PAPIRSØKNAD,
    MANUELT_OPPRETTET,
    KORRIGERING_UTEN_BREV,
    SATSENDRING,
    ;

    companion object {
        fun fraDomene(årsak: BehandlingÅrsak) = when (årsak) {
            BehandlingÅrsak.KLAGE -> KLAGE
            BehandlingÅrsak.NYE_OPPLYSNINGER -> NYE_OPPLYSNINGER
            BehandlingÅrsak.SØKNAD -> SØKNAD
            BehandlingÅrsak.PAPIRSØKNAD -> PAPIRSØKNAD
            BehandlingÅrsak.MANUELT_OPPRETTET -> MANUELT_OPPRETTET
            BehandlingÅrsak.KORRIGERING_UTEN_BREV -> KORRIGERING_UTEN_BREV
            BehandlingÅrsak.SATSENDRING -> SATSENDRING
        }
    }
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

data class MålgruppeDvh(
    val type: MålgruppeTypeDvh,
    val resultat: ResultatDvh,
) {
    companion object {
        fun fraDomene(vilkårsperioder: List<Vilkårperiode>): List<MålgruppeDvh> {
            return vilkårsperioder
                .filterNot { ResultatVilkårperiode.SLETTET == it.resultat }
                .map {
                    MålgruppeDvh(
                        type = MålgruppeTypeDvh.fraDomene(it.type),
                        resultat = ResultatDvh.fraDomene(it.resultat),
                    )
                }
        }
    }
}

enum class ResultatDvh {
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    IKKE_TATT_STILLING_TIL,
    SKAL_IKKE_VURDERES,
    ;

    companion object {
        fun fraDomene(resultat: ResultatVilkårperiode) = when (resultat) {
            ResultatVilkårperiode.OPPFYLT -> OPPFYLT
            ResultatVilkårperiode.IKKE_OPPFYLT -> IKKE_OPPFYLT
            ResultatVilkårperiode.IKKE_VURDERT -> IKKE_TATT_STILLING_TIL
            ResultatVilkårperiode.SLETTET ->
                throw IllegalArgumentException("Slettede vedtak er ikke relevant, og skal ha blitt filtrert bort.")
        }
    }
}

enum class AktivitetTypeDvh {
    TILTAK,
    UTDANNING,
    REELL_ARBEIDSSØKER,
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            AktivitetType.TILTAK -> TILTAK
            AktivitetType.UTDANNING -> UTDANNING
            AktivitetType.REELL_ARBEIDSSØKER -> REELL_ARBEIDSSØKER
            else -> {
                throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type aktivitet.")
            }
        }
    }
}

enum class MålgruppeTypeDvh {
    AAP,
    DAGPENGER,
    OMSTILLINGSSTØNAD,
    OVERGANGSSTØNAD,
    NEDSATT_ARBEIDSEVNE,
    UFØRETRYGD,
    ;

    companion object {
        fun fraDomene(vilkårsperiodeType: VilkårperiodeType) = when (vilkårsperiodeType) {
            MålgruppeType.AAP -> AAP
            MålgruppeType.DAGPENGER -> DAGPENGER
            MålgruppeType.OMSTILLINGSSTØNAD -> OMSTILLINGSSTØNAD
            MålgruppeType.OVERGANGSSTØNAD -> OVERGANGSSTØNAD
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> NEDSATT_ARBEIDSEVNE
            MålgruppeType.UFØRETRYGD -> UFØRETRYGD
            else -> {
                throw IllegalArgumentException("$vilkårsperiodeType er ikke en gyldig type målgruppe.")
            }
        }
    }
}

data class VilkårsvurderingDvh(
    val resultat: Vilkårsresultat,
    val vilkårId: RegelId,

    // TODO: Mapping fra VilkårDto
)

enum class AdressebeskyttelseDvh {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    companion object {
        fun fraDomene(adressebeskyttelse: AdressebeskyttelseGradering) =
            when (adressebeskyttelse) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> STRENGT_FORTROLIG
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.FORTROLIG -> FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> UGRADERT
            }
    }
}

enum class AvslagÅrsakDvh {
    // TODO når vi får avslag i løsningen
}

enum class ÅrsakRevurderingDvh {
    // TODO når vi får revurdering i løsningen
}
