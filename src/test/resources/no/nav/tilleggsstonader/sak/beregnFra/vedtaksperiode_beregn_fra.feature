# language: no
# encoding: UTF-8

Egenskap: Utled beregn fra endring av vedtaksperiode

  Regel: Endring av sluttdato skal gi beregn fra dato som tidligste av ny og gammel fom

    Scenario: Vedtaksperiode avslutter senere
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 12.01.2024 | 31.01.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 12.01.2024 | 19.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 01.02.2024

    Scenario: Vedtaksperiode avslutter før
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 12.01.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 12.01.2024 | 14.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 15.03.2024

  Regel: Endring av startdato skal gi beregn fra dato som tidligste av ny og gammel tom
    Scenario: Vedtaksperiode starter før
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.02.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 04.02.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 04.02.2024

    Scenario: Vedtaksperiode starter senere
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 04.02.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.02.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 04.02.2024

  Regel: Nye målgrupper skal gi beregn fra startdato på ny periode
    Scenario: Ny vedtaksperiode lagt inn før eksisterende
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Ny vedtaksperiode lagt inn etter eksisterende
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |


      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.05.2024

  Regel: Sletting av målgruppe skal gi beregn fra lik fom på slettet periode
    Scenario: En vedtaksperiode blir fjernet
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.05.2024

    Scenario: En vedtaksperiode fjernes og ny legges til
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 10.06.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 12.05.2024

    Scenario: Alle vedtaksperioder blir fjernet
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom | Tom | Aktivitet | Målgruppe |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Alle vedtaksperioder blir fjernet og erstattes med nye
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe           |
        | 14.03.2024 | 31.03.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |
        | 12.05.2024 | 28.06.2024 | TILTAK    | NEDSATT_ARBEIDSEVNE |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe        |
        | 01.08.2024 | 31.12.2024 | UTDANNING | ENSLIG_FORSØRGER |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

  Regel: Hvis aktivitet eller målgruppetype endres skal det beregnes fra starten på endret periode
    Scenario: Aktivitettype endres
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe        |
        | 14.03.2024 | 31.03.2024 | UTDANNING | ENSLIG_FORSØRGER |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet          | Målgruppe        |
        | 14.03.2024 | 31.03.2024 | REELL_ARBEIDSSØKER | ENSLIG_FORSØRGER |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024

    Scenario: Målgruppe endres
      Gitt følgende vedtaksperioder i forrige behandling - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe        |
        | 14.03.2024 | 31.03.2024 | UTDANNING | ENSLIG_FORSØRGER |

      Gitt følgende vedtaksperioder i revurdering - beregnFra
        | Fom        | Tom        | Aktivitet | Målgruppe   |
        | 14.03.2024 | 31.03.2024 | UTDANNING | GJENLEVENDE |

      Når utleder beregnFraDato

      Så forvent følgende dato for tidligste endring: 14.03.2024