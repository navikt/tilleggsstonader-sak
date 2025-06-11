# language: no
# encoding: UTF-8

Egenskap: Utleder beregn fra dato ved flere endringer

  Regel: Ved flere endringer skal beregn fra dato være tidligste av alle endringer

    Scenario: Alle perioder blir forlenget, men aktivitet endrer også antall dager
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      Og følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 19.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Og følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | AAP  | OPPFYLT  | NY     |
      Og følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Og følgende vilkår i forrige behandling - beregnFra
        | Fom        | Tom        | Type      | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | PASS_BARN | OPPFYLT  | NY     |
      Og følgende vilkår i revurdering - beregnFra
        | Fom        | Tom        | Type      | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

      Gitt følgende aktiviteter i forrige behandling - beregnFra
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - beregnFra
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 4               | OPPFYLT  | ENDRET |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 01.01.2024