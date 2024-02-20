# language: no
# encoding: UTF-8

Egenskap: Ny beregning

  Scenario: En full måned med 5 aktivitetsdager

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 31.01.2024 | 5               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 23           | 1000   |

  Scenario: En full måned med 2 aktivitetsdager

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 31.01.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 10           | 1000   |

  Scenario: En full måned med antall aktivitetsdager mer enn hva det er plass til

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 31.01.2024 | 4               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 19           | 1000   |

  Scenario: En full måned med aktivitet inni aktivitet

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 31.01.2024 | 4               |
      | 16.01.2024 | 31.01.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 21           | 1000   |

  Scenario: En full måned med overlappende aktiviteter

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 19.01.2024 | 4               |
      | 16.01.2024 | 31.01.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 17           | 1000   |

  Scenario: En full måned med 3 overlappende aktiviteter

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 01.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 10.01.2024 | 3               |
      | 09.01.2024 | 23.01.2024 | 3               |
      | 16.01.2024 | 31.01.2024 | 2               |
#    uke 1 = 3
#    uke 2 = 3+3=6 => 5
#    uke 3 = 3+2=5
#    uke 4 = 2 (periode 2) + 2 (periode 3) = 4
#    uke 5 = 2
#    totalt = 3+5+5+4+2=19

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 19           | 1000   |

  Scenario: Stønadsperiode på tvers av to måneder og en aktivitet

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 29.02.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 02.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 29.02.2024 | 5               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 23           | 1000   |
      | 02.2024 | 29.53   | 21           | 1000   |

  Scenario: Stønadsperiode på tvers av to måneder og to aktiviteter

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 29.02.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 02.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 31.01.2024 | 4               |
      | 24.01.2024 | 29.02.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 20           | 1000   |
      | 02.2024 | 29.53   | 10           | 1000   |

  Scenario: Stønadsperiode på tvers av to måneder og tre aktiviteter

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 15.02.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 02.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 24.01.2024 | 2               |
      | 17.01.2024 | 12.02.2024 | 2               |
      | 06.02.2024 | 15.02.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 14           | 1000   |
      | 02.2024 | 29.53   | 9            | 1000   |

    Når beregner for hele perioden

    Så forvent følgende totalt antall dager: 21


  Scenario: Stønadsperiode på tvers av flere måneder full aktivitet

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 14.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 06.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 14.06.2024 | 5               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 23           | 1000   |
      | 02.2024 | 29.53   | 21           | 1000   |
      | 03.2024 | 29.53   | 21           | 1000   |
      | 04.2024 | 29.53   | 22           | 1000   |
      | 05.2024 | 29.53   | 23           | 1000   |
      | 06.2024 | 29.53   | 10           | 1000   |

    Når beregner for hele perioden
    Så forvent følgende summert antall dager: 120
    Så forvent følgende totalt antall dager: 120

  Scenario: Stønadsperiode på tvers av flere måneder færre aktivitetsdager

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.01.2024 | 14.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2024 | 06.2024 | 1000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 14.06.2024 | 2               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 01.2024 | 29.53   | 10           | 1000   |
      | 02.2024 | 29.53   | 10           | 1000   |
      | 03.2024 | 29.53   | 9            | 1000   |
      | 04.2024 | 29.53   | 10           | 1000   |
      | 05.2024 | 29.53   | 10           | 1000   |
      | 06.2024 | 29.53   | 4            | 1000   |

    Når beregner for hele perioden
    Så forvent følgende summert antall dager: 53
    Så forvent følgende totalt antall dager: 48

