package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

import no.nav.tilleggsstonader.libs.utils.dato.januar
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

    // Hjelpefunksjoner for å sette opp testdata f.eks. for en gitt stønadstype
    fun defaultDagligReiseTsoTestdata(
        fom: LocalDate = 1 januar 2026,
        tom: LocalDate = 31 januar 2026,
    ) {
        aktivitet {
            opprett {
                aktivitetTiltak(fom, tom)
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
}
