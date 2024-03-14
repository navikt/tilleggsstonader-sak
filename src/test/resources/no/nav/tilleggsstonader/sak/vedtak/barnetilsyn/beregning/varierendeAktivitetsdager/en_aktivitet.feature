# language: no
# encoding: UTF-8

Egenskap: Beregning - En aktivitet med delvisaktivitet

  Scenario: Tre fulle uker med delvisaktivitet
    # Mål: Antall dager skal bli aktivitetsdager x antall uker

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2023 | 21.01.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 21.01.2023 | TILTAK    | 3               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 29.53   | 9            | 1000   |

    Scenario: Færre dager i stønadsperiode enn antall aktivitetsdager:
      # Mål: Antall dager skal ikke bli flere enn det er plass til i en uke
      Gitt følgende støndsperioder
        | Fom        | Tom        | Aktivitet | Målgruppe |
        | 02.01.2023 | 04.01.2023 | TILTAK    | AAP       |

      Gitt følgende aktiviteter
        | Fom        | Tom        | Aktivitet | Aktivitetsdager |
        | 02.01.2023 | 04.01.2023 | TILTAK    | 4               |

      Gitt følgende utgifter for barn med id: 1
        | Fom     | Tom     | Utgift |
        | 01.2023 | 01.2023 | 1000   |

      Når beregner

      Så forvent følgende beregningsresultat
        | Måned   | Dagsats | Antall dager | Utgift |
        | 01.2023 | 29.53   | 3            | 1000   |