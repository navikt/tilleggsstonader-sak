# language: no
# encoding: UTF-8

Egenskap: Beregning - Utgifter

  Scenario: Stønadsperiode skal begrenses av perioder med utgifter:
    # Mål: Beregningen skal ikke gi resultat for februar fordi det ikke er utgifter i denne perioden

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |

  Scenario: Beregning skal ikke ta med utgifter utenfor stønadsperiode
    # Mål: Beregningen skal ikke gi resultat for februar fordi det ikke er utgifter i denne perioden

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.02.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 03.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 02.2023 | 29.53   | 20           | 1000   | 591         |

  Scenario: Kun et barn har utgift i deler av perioden
    # Mål: Beregning skal kun inkludere utgifter for barn med utgift den måneden

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Gitt følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 59.07   | 22           | 2000   | 1300        |
      | 02.2023 | 29.53   | 20           | 1000   | 591         |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Så forvent følgende stønadsperioder for: 02.2023
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.02.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

  Scenario: Skal avrunde månedsbeløpet

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe |
      | 02.01.2023 | 03.01.2023 | TILTAK    | AAP       |
      | 06.02.2023 | 07.02.2023 | TILTAK    | AAP       |
      | 06.03.2023 | 07.03.2023 | TILTAK    | AAP       |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Type   | Aktivitetsdager |
      | 01.01.2023 | 07.03.2023 | TILTAK | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 144    |
      | 02.2023 | 02.2023 | 136    |
      | 03.2023 | 03.2023 | 131    |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Månedsbeløp | Antall dager | Utgift |
      # Denne gir en sats som blir 4.2455.., som blir arundet til 4.25
      | 01.2023 | 4.25    | 9           | 2            | 144    |
      # Pga avrunding av utgift * sats, blir denne 4.01 og ikke 4.02
      | 02.2023 | 4.01    | 8           | 2            | 136    |
      | 03.2023 | 3.88    | 8           | 2            | 131    |

  Scenario: 0 i utgifter:

    Gitt følgende støndsperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 0      |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 0.00    | 22           | 0      | 0           |