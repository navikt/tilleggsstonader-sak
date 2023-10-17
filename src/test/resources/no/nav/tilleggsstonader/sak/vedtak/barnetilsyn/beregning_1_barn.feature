# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn 1 barn

  Scenario: Stønadsperioder og utgifter for januar

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2023 | 31.01.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 29.53   | 22           | 1000   |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        |
      | 01.01.2023 | 31.01.2023 |

  Scenario: Stønadsperioder i 8 dager i januar

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 02.01.2023 | 11.01.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 29.53   | 8            | 1000   |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        |
      | 02.01.2023 | 11.01.2023 |

  Scenario: Stønadsperioder i 10 dager i januar, fordelt på 2 stønadsperioder

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 02.01.2023 | 06.01.2023 |
      | 16.01.2023 | 20.01.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 29.53   | 10           | 1000   |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        |
      | 02.01.2023 | 06.01.2023 |
      | 16.01.2023 | 20.01.2023 |

  Scenario: Stønadsperioder i 8 dager i januar, tom mars

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 20.01.2023 | 31.03.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 03.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 29.53   | 8            | 1000   |
      | 02.2023 | 29.53   | 20           | 1000   |
      | 03.2023 | 29.53   | 23           | 1000   |

    Så forvent følgende stønadsperioder for: 01.2023
      | Fom        | Tom        |
      | 20.01.2023 | 31.01.2023 |

    Så forvent følgende stønadsperioder for: 02.2023
      | Fom        | Tom        |
      | 01.02.2023 | 28.02.2023 |

    Så forvent følgende stønadsperioder for: 03.2023
      | Fom        | Tom        |
      | 01.03.2023 | 31.03.2023 |

  Scenario: Stønadsperioder med like antall dager og beløp skal ikke slåes sammen

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.10.2023 | 30.11.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 10.2023 | 11.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 10.2023 | 29.53   | 22           | 1000   |
      | 11.2023 | 29.53   | 22           | 1000   |

  Scenario: Stønadsperioder med like antall dager og og ulikt beløp skal ikke slåes sammen

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.10.2023 | 30.11.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 10.2023 | 10.2023 | 1000   |
      | 11.2023 | 11.2023 | 1500   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 10.2023 | 29.53   | 22           | 1000   |
      | 11.2023 | 44.30   | 22           | 1500   |

  Scenario: Utgift for gitt periode går over makssatsen

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2023 | 31.01.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 5000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2023 | 147.67  | 22           | 5000   |
