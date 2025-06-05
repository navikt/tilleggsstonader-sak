# language: no
# encoding: UTF-8

Egenskap: Utled beregn fra endring av aktivitet

  Scenario: Aktivitet er forlenget
    Gitt følgende aktiviteter i forrige behandling - beregnFra
      | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
      | 01.01.2024 | 31.01.2024 | BARNETILSYN | TILTAK | 5                | OPPFYLT  | NY     |

    Gitt følgende aktiviteter i revurdering - beregnFra
      | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
      | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 5                | OPPFYLT  | NY     |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.02.2024