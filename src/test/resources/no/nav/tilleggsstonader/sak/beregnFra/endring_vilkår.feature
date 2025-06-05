# language: no
# encoding: UTF-8

Egenskap: Utled beregn fra endring av vilkår

  Scenario: Vilkår er forlenget
    Gitt følgende vilkår i forrige behandling for beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.01.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.02.2024

  Scenario: Vilkår er forkortet
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 30.06.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.01.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.04.2024

  Scenario: Nytt vilkår lagt inn før eksisterende
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.02.2024 | 29.02.2024 | PASS_BARN | OPPFYLT  | NY      |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | UENDRET |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.02.2024

  Scenario: Nytt vilkår lagt inn etter eksisterende
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status  |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | UENDRET |
      | 01.04.2024 | 30.04.2024 | PASS_BARN | OPPFYLT  | NY      |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.04.2024

  Scenario: Endring i utgift
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status | Utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     | 1000   |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status | Utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | ENDRET | 1500   |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.03.2024

#    Endring i resultat tilsier at det har vært en endring i delvilkår som må tas hensyn til
  Scenario: Endring i resultat
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat     | Status |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | IKKE_OPPFYLT | ENDRET |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.03.2024

  Scenario: Utgift endrer seg fra å være fremtidig til å ikke være fremtidig
    Gitt følgende vilkår i forrige behandling - beregnFra
      | Fom        | Tom        | Type      | Resultat | Status | Er fremtidig utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | OPPFYLT  | NY     | Ja                  |

    Gitt følgende vilkår i revurdering - beregnFra
      | Fom        | Tom        | Type      | Resultat     | Status | Er fremtidig utgift |
      | 01.03.2024 | 31.03.2024 | PASS_BARN | IKKE_OPPFYLT | ENDRET | Nei                 |

    Når utleder beregnFraDato

    Så forvent følgende dato for tidligste endring: 01.03.2024