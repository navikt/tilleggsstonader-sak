# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn 2 barn

  Scenario: Stønadsperioder i 10 dager i januar, og hele februar, kun barn 2 har utgifter i begge måneder

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 02.01.2023 | 06.01.2023 |
      | 01.02.2023 | 28.02.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Gitt følgende utgifter for barn: 2
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 59.07   | 5            | 2000   |
      | 02.2023 | 29.53   | 20           | 1000   |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        |
      | 02.01.2023 | 06.01.2023 |

    Så forvent følgende stønadsperioder for: 02.2023
      | Fom        | Tom        |
      | 01.02.2023 | 28.02.2023 |

