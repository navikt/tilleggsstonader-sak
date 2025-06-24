# language: no
# encoding: UTF-8

Egenskap: Utleder tidligste endring ved flere endringer

  Regel: Ved flere endringer skal tidligste endring dato være tidligste av alle endringer

    Scenario: Alle perioder blir forlenget, men aktivitet endrer også antall dager
      Gitt følgende vedtaksperioder i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      Og følgende vedtaksperioder i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 01.01.2024 | 19.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Og følgende målgrupper i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | AAP  | OPPFYLT  | NY     |
      Og følgende målgrupper i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Og følgende vilkår i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | PASS_BARN | OPPFYLT  | NY     |
      Og følgende vilkår i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Type      | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

      Gitt følgende aktiviteter i forrige behandling - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | BARNETILSYN | TILTAK | 5               | OPPFYLT  | NY     |

      Gitt følgende aktiviteter i revurdering - utledTidligsteEndring
        | Fom        | Tom        | Stønadstype | Type   | Aktivitetsdager | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | BARNETILSYN | TILTAK | 4               | OPPFYLT  | ENDRET |

      Når utleder tidligste endring

      Så forvent følgende dato for tidligste endring: 01.01.2024