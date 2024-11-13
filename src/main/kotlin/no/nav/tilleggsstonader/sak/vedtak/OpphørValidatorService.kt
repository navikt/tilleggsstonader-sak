package no.nav.tilleggsstonader.sak.vedtak

import org.springframework.stereotype.Service

@Service
class OpphørValidatorService {

    fun validerOpphør(){
        validerIngenVilkårEllerVilkårperiodeMedStatusNy()
        validerVilkår()
        validerVilkårperiode()
        validerOverlappsperiode()
        TODO()
    }


    private fun validerIngenVilkårEllerVilkårperiodeMedStatusNy(){
        TODO()
    }

    private fun validerVilkår(){
        //Resultat kan endre seg fra oppfylt til ikke oppfylt
        //At TOM er lik eller "forkortet"
        //Har de samme fom og beløp som tidligere
        TODO()
    }

    private fun validerVilkårperiode(){
        // Resultat kan endre seg fra oppfylt til ikke oppfylt
        // At TOM er lik eller "forkortet"
        TODO()
    }

    private fun validerOverlappsperiode(){
        //At TOM er lik eller "forkortet"
        TODO()
    }
}