package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FlyttingVilkårValideringTest {
    private val nødvendigUtgift = NødvendigUtgift(SvarJaNei.JA, null)

    @Test
    fun `skal tillate ufullstendige tilbud ved validering av oppgitte fakta`() {
        assertDoesNotThrow {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaFlyttebyrå(
                    tilbud =
                        listOf(
                            FlyttebyråTilbud(navn = "Flyttebyrå A", pris = null),
                            FlyttebyråTilbud(navn = null, pris = null),
                        ),
                    adresse = "Flytteveien 1",
                    erBetalingDokumentert = true,
                    nødvendigUtgift = nødvendigUtgift,
                ),
            )
        }
    }

    @Test
    fun `skal avvise like firmanavn selv med forskjellig mellomrom og store bokstaver`() {
        assertThrows<ApiFeil> {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaFlyttebyrå(
                    tilbud =
                        listOf(
                            FlyttebyråTilbud(navn = "Flyttebyrå A", pris = 10000),
                            FlyttebyråTilbud(navn = " flyttebyrå a ", pris = 12000),
                        ),
                    adresse = "Flytteveien 1",
                    erBetalingDokumentert = true,
                    nødvendigUtgift = nødvendigUtgift,
                ),
            )
        }
    }

    @Test
    fun `skal avvise annet antall enn to tilbud`() {
        listOf(
            emptyList(),
            listOf(FlyttebyråTilbud("A", 10000)),
            listOf(FlyttebyråTilbud("A", 10000), FlyttebyråTilbud("B", 11000), FlyttebyråTilbud("C", 12000)),
        ).forEach { tilbud ->
            val feil =
                assertThrows<ApiFeil> {
                    FlyttingVilkårValidering.validerOppgitteFakta(
                        FaktaFlyttebyrå(
                            tilbud = tilbud,
                            erBetalingDokumentert = true,
                            adresse = "Flytteveien 1",
                            nødvendigUtgift = nødvendigUtgift,
                        ),
                    )
                }
            assertThat(feil.message).isEqualTo("Det må være nøyaktig to tilbud fra flyttebyrå")
        }
    }

    @Test
    fun `skal avvise blankt oppgitt byrånavn og priser som ikke er positive`() {
        val ugyldigeFakta =
            listOf(
                listOf(FlyttebyråTilbud(" ", 10000), FlyttebyråTilbud("Byrå B", 12000)),
                listOf(FlyttebyråTilbud("Byrå A", 0), FlyttebyråTilbud("Byrå B", 12000)),
                listOf(FlyttebyråTilbud("Byrå A", -1), FlyttebyråTilbud("Byrå B", 12000)),
            )

        ugyldigeFakta.forEach { tilbud ->
            assertThrows<ApiFeil> {
                FlyttingVilkårValidering.validerOppgitteFakta(
                    FaktaFlyttebyrå(
                        tilbud = tilbud,
                        adresse = "Flytteveien 1",
                        erBetalingDokumentert = true,
                        nødvendigUtgift = nødvendigUtgift,
                    ),
                )
            }
        }
    }

    @Test
    fun `skal avvise negativ kostnad for henger`() {
        assertThrows<ApiFeil> {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaFlytteSelv(
                    avstandEnVei = 250,
                    henger = -1,
                    bompenger = null,
                    ferge = null,
                    parkering = null,
                    adresse = "Flytteveien 1",
                    nødvendigUtgift = nødvendigUtgift,
                ),
            )
        }
    }

    @Test
    fun `skal avvise tom flytteadresse`() {
        listOf("", " ", "\t\n").forEach { adresse ->
            listOf(
                FaktaFlyttingUbestemt(adresse = adresse),
                FaktaFlytteSelv(null, null, null, null, null, nødvendigUtgift, adresse),
                FaktaFlyttebyrå(
                    tilbud = listOf(FlyttebyråTilbud(null, null), FlyttebyråTilbud(null, null)),
                    erBetalingDokumentert = false,
                    adresse = adresse,
                    nødvendigUtgift = nødvendigUtgift,
                ),
            ).forEach { fakta ->
                val feil =
                    assertThrows<ApiFeil> {
                        FlyttingVilkårValidering.validerOppgitteFakta(fakta)
                    }
                assertThat(feil.message).isEqualTo("Flytteadressen kan ikke være tom")
            }
        }
    }

    @Test
    fun `skal være fullstendig hvis man kun har dato og adresse`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    flytteDato = 1 januar 2026,
                    svar = emptyMap(),
                    fakta = FaktaFlyttingUbestemt(adresse = "Flytteveien 1"),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal være fullstendig når bruker benytter flyttebyrå med to komplette tilbud`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    flytteDato = 1 januar 2026,
                    svar =
                        mapOf(
                            *oppfylteSvarFremTilFlyttemåte,
                            RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelse(SvarId.FLYTTEBYRÅ),
                        ),
                    fakta =
                        FaktaFlyttebyrå(
                            tilbud =
                                listOf(
                                    FlyttebyråTilbud(navn = "Flyttebyrå A", pris = 10000),
                                    FlyttebyråTilbud(navn = "Flyttebyrå B", pris = 12000),
                                ),
                            adresse = "Flytteveien 1",
                            erBetalingDokumentert = false,
                            nødvendigUtgift = nødvendigUtgift,
                        ),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal ikke være fullstendig når tilbud mangler navn eller pris eller listen ikke har to tilbud`() {
        val svar =
            mapOf(
                *oppfylteSvarFremTilFlyttemåte,
                RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelse(SvarId.FLYTTEBYRÅ),
            )
        val ugyldigeTilbud =
            listOf(
                emptyList(),
                listOf(FlyttebyråTilbud("Byrå A", 10000)),
                listOf(FlyttebyråTilbud("Byrå A", 10000), FlyttebyråTilbud("Byrå B", null)),
                listOf(FlyttebyråTilbud("Byrå A", 10000), FlyttebyråTilbud(null, 12000)),
            )

        ugyldigeTilbud.forEach { tilbud ->
            assertThat(
                FlyttingVilkårValidering.erFullstendig(
                    lagreVilkår(
                        svar = svar,
                        fakta =
                            FaktaFlyttebyrå(
                                tilbud = tilbud,
                                adresse = "Flytteveien 1",
                                erBetalingDokumentert = false,
                                nødvendigUtgift = nødvendigUtgift,
                            ),
                    ),
                ),
            ).isFalse()
        }
    }

    @Test
    fun `skal være fullstendig når bruker flytter selv med oppgitt avstand`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    flytteDato = 1 januar 2026,
                    svar =
                        mapOf(
                            *oppfylteSvarFremTilFlyttemåte,
                            RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelse(SvarId.FLYTTER_SELV),
                        ),
                    fakta =
                        FaktaFlytteSelv(
                            avstandEnVei = 250,
                            henger = null,
                            bompenger = null,
                            ferge = null,
                            parkering = null,
                            adresse = "Flytteveien 1",
                            nødvendigUtgift = nødvendigUtgift,
                        ),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal være fullstendig med kun adresse når bruker ikke må flytte for å gjennomføre aktiviteten`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                lagreVilkår(
                    svar =
                        mapOf(
                            RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                                SvarOgBegrunnelse(
                                    SvarId.FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING,
                                    "Start",
                                ),
                            RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET to SvarOgBegrunnelse(SvarId.NEI, "Nei"),
                        ),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal være fullstendig med kun adresse når bruker ikke flytter i forbindelse med arbeid`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                lagreVilkår(
                    svar =
                        mapOf(
                            RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                                SvarOgBegrunnelse(SvarId.ARBEID_ETTER_GJENNOMFØRT_AKTIVITET, "Arbeid"),
                            RegelId.FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET to SvarOgBegrunnelse(SvarId.NEI, "Nei"),
                        ),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal ikke endre vilkårsresultatet når flytteutgiften vurderes som unødvendig`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                lagreVilkår(
                    svar =
                        mapOf(
                            *oppfylteSvarFremTilFlyttemåte,
                            RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelse(SvarId.FLYTTEBYRÅ),
                        ),
                    fakta =
                        FaktaFlyttebyrå(
                            tilbud =
                                listOf(
                                    FlyttebyråTilbud("Flyttebyrå A", 10000),
                                    FlyttebyråTilbud("Flyttebyrå B", 12000),
                                ),
                            erBetalingDokumentert = false,
                            nødvendigUtgift = NødvendigUtgift(SvarJaNei.NEI, "Utgiften er for høy"),
                            adresse = "Flytteveien 1",
                        ),
                ),
            ),
        ).isTrue()
    }

    private val oppfylteSvarFremTilFlyttemåte =
        arrayOf(
            RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                SvarOgBegrunnelse(
                    SvarId.FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING,
                    "Start",
                ),
            RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET to
                SvarOgBegrunnelse(SvarId.FLYTTER_TIL_AKTIVITETSSTED, "Må flytte"),
        )

    private fun lagreVilkår(
        svar: Map<RegelId, SvarOgBegrunnelse>,
        fakta: FlyttingVilkårFakta = FaktaFlyttingUbestemt(adresse = "Flytteveien 1"),
    ) = LagreVilkårFlytting(
        flytteDato = 1 januar 2026,
        svar = svar,
        fakta = fakta,
    )
}
