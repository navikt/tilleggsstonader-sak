# language: no
# encoding: UTF-8

Egenskap: Utled beregn fra endring av målgruppe

  Regel: Endring av sluttdato skal gi beregn fra dato som tidligste av ny og gammel fom

    Scenario: Målgruppe avslutter senere
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.01.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 01.02.2024

    Scenario: Målgruppe avslutter før
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 01.01.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 01.04.2024

  Regel: Endring av startdato skal gi beregn fra dato som tidligste av ny og gammel tom
    Scenario: Målgruppe starter før
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 14.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 08.01.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 08.01.2024

    Scenario: Målgruppe starter senere
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 08.01.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | ENDRET |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 08.01.2024

  Regel: Nye målgrupper skal gi beregn fra startdato på ny periode
    Scenario: Ny målgrupper lagt inn før eksisterende
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 12.05.2024 | 28.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY      |
        | 12.05.2024 | 28.06.2024 | AAP  | OPPFYLT  | UENDRET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Ny målgruppe lagt inn etter eksisterende
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | UENDRET |
        | 12.05.2024 | 28.06.2024 | AAP  | OPPFYLT  | NY      |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.05.2024

    Scenario: Ny målgruppe lagt inn med samme startdato som eksisterende
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | UENDRET |
        | 14.03.2024 | 28.06.2024 | AAP  | OPPFYLT  | NY      |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

  Regel: Endring i fakta og vurderinger skal gi beregn fra dato lik fom på periode
    Scenario: Endring i resultat
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 14.03.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat     | Status |
        | 14.03.2024 | 31.03.2024 | AAP  | IKKE_OPPFYLT | ENDRET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

  Regel: Sletting av målgruppe skal gi beregn fra lik fom på slettet periode
    Scenario: Målgruppe blir slettet
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | SLETTET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Målgruppe blir slettet og ny målgruppe blir lagt til
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | SLETTET |
        | 12.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY      |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Målgruppe ble slettet i forrige behandling og tilsvarende blir lagt til i revurdering
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | SLETTET |
        | 12.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY      |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 04.02.2024 | 31.03.2024 | AAP  | OPPFYLT  | NY      |
        | 12.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | UENDRET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Målgruppe slettes erstattes med tilsvarende målgruppe med annen type
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 12.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type            | Resultat | Status  |
        | 12.03.2024 | 30.06.2024 | AAP             | OPPFYLT  | SLETTET |
        | 12.03.2024 | 30.06.2024 | OVERGANGSSTØNAD | OPPFYLT  | NY      |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.03.2024

    Scenario: Målgruppe får ny fom før det blir slettet
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 12.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 15.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | UENDRET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.03.2024

    Scenario: Målgruppe får ny tom før det blir slettet
      Gitt følgende målgrupper i forrige behandling - beregnFra
        | Fom        | Tom        | Type | Resultat | Status |
        | 12.03.2024 | 30.06.2024 | AAP  | OPPFYLT  | NY     |

      Gitt følgende målgrupper i revurdering - beregnFra
        | Fom        | Tom        | Type | Resultat | Status  |
        | 12.03.2024 | 14.05.2024 | AAP  | OPPFYLT  | SLETTET |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.03.2024