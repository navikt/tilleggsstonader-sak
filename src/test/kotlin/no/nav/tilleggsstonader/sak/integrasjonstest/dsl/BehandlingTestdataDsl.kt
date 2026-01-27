package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.toYearMonth
import java.time.LocalDate

@BehandlingTestdataDslMarker
class BehandlingTestdataDsl internal constructor() {
    internal val aktivitet: VilkårperiodeTestdataDsl = VilkårperiodeTestdataDsl()
    internal val målgruppe: VilkårperiodeTestdataDsl = VilkårperiodeTestdataDsl()
    internal val vilkår: StønadsvilkårTestdataDsl = StønadsvilkårTestdataDsl()
    internal var vedtak: OpprettVedtakTestdataDsl = OpprettVedtakTestdataDsl()

    fun aktivitet(block: VilkårperiodeTestdataDsl.() -> Unit) {
        aktivitet.apply(block)
    }

    fun målgruppe(block: VilkårperiodeTestdataDsl.() -> Unit) {
        målgruppe.apply(block)
    }

    fun vilkår(block: StønadsvilkårTestdataDsl.() -> Unit) {
        vilkår.apply(block)
    }

    fun vedtak(block: OpprettVedtakTestdataDsl.() -> Unit) {
        vedtak.apply(block)
    }

    companion object {
        fun build(block: BehandlingTestdataDsl.() -> Unit): BehandlingTestdataDsl = BehandlingTestdataDsl().apply(block)
    }

    private val defaultFom = 1 januar 2026
    private val defaultTom = 31 januar 2026

    // Hjelpefunksjoner for å sette opp testdata f.eks. for en gitt stønadstype
    fun defaultDagligReiseTsoTestdata(
        fom: LocalDate = defaultFom,
        tom: LocalDate = defaultTom,
    ) {
        aktivitet {
            opprett {
                aktivitetTiltakTso(fom, tom)
            }
        }
        målgruppe {
            opprett {
                målgruppeAAP(fom, tom)
            }
        }
        vilkår {
            opprett {
                offentligTransport(fom = fom, tom = tom)
            }
        }
    }

    fun defaultDagligReiseTsrTestdata(
        fom: LocalDate = defaultFom,
        tom: LocalDate = defaultTom,
    ) {
        aktivitet {
            opprett {
                aktivitetTiltakTsr(
                    fom = fom,
                    tom = tom,
                    typeAktivitet = TypeAktivitet.GRUPPEAMO,
                )
            }
        }
        målgruppe {
            opprett {
                målgruppeTiltakspenger(fom, tom)
            }
        }
        vilkår {
            opprett {
                offentligTransport(fom = fom, tom = tom)
            }
        }
    }

    fun defaultTilsynBarnTestdata(
        fom: LocalDate = defaultFom,
        tom: LocalDate = defaultTom,
    ) {
        aktivitet {
            opprett {
                aktivitetTiltakTilsynBarn(
                    fom = fom,
                    tom = tom,
                    aktivitetsdager = 4,
                )
            }
        }
        målgruppe {
            opprett {
                målgruppeAAP(fom, tom)
            }
        }
        vilkår {
            opprett {
                passBarn(
                    fom = fom.toYearMonth(),
                    tom = tom.toYearMonth(),
                    utgift = 1000,
                )
            }
        }
    }

    fun defaultLæremidlerTestdata(
        fom: LocalDate = defaultFom,
        tom: LocalDate = defaultTom,
    ) {
        aktivitet {
            opprett {
                aktivitetUtdanningLæremidler(fom, tom)
            }
        }
        målgruppe {
            opprett {
                målgruppeAAP(fom, tom)
            }
        }
    }

    fun defaultBoutgifterTestdata(
        fom: LocalDate = defaultFom,
        tom: LocalDate = defaultTom,
    ) {
        aktivitet {
            opprett {
                aktivitetTiltakBoutgifter(fom, tom)
            }
        }
        målgruppe {
            opprett {
                målgruppeAAP(fom, tom)
            }
        }
        vilkår {
            opprett {
                løpendeutgifterEnBolig(fom, tom)
            }
        }
    }
}
