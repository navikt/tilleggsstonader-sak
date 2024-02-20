# language: no
# encoding: UTF-8

Egenskap: Ny beregning lenger periode

  Scenario: Helt år, aktivitetsdager = 5

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.08.2023 | 31.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 08.2023 | 06.2024 | 3000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.08.2023 | 31.06.2024 | 5               |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift |
      | 08.2024 | 88.60   | 23           | 3000   |
      | 09.2024 | 88.60   | 21           | 3000   |
      | 10.2024 | 88.60   | 22           | 3000   |
      | 11.2024 | 88.60   | 22           | 3000   |
      | 12.2024 | 88.60   | 21           | 3000   |
      | 01.2024 | 88.60   | 23           | 3000   |
      | 02.2024 | 88.60   | 21           | 3000   |
      | 03.2024 | 88.60   | 21           | 3000   |
      | 04.2024 | 88.60   | 22           | 3000   |
      | 05.2024 | 88.60   | 23           | 3000   |
      # Sjekk over mai, april
      | 06.2024 | 88.60   | 20           | 3000   |
# feil i april, mai,
    Når beregner for hele perioden
    Så forvent følgende summert antall dager: 239
    Så forvent følgende totalt antall dager: 239

  Scenario: Helt år, aktivitetsdager = 4

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.08.2023 | 31.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 08.2023 | 06.2024 | 3000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.08.2023 | 31.06.2024 | 4               |

    Når beregner

#    Så forvent følgende beregningsresultat
#      | Måned   | Dagsats | Antall dager | Utgift |
#      | 08.2024 | 88.60   | 23           | 3000   |
#      | 09.2024 | 88.60   | 21           | 3000   |
#      | 10.2024 | 88.60   | 22           | 3000   |
#      | 11.2024 | 88.60   | 22           | 3000   |
#      | 12.2024 | 88.60   | 21x          | 3000   |
#      | 01.2024 | 88.60   | 23x          | 3000   |
#      | 02.2024 | 88.60   | 21           | 3000   |
#      | 03.2024 | 88.60   | 21 x         | 3000   |
#      | 04.2024 | 88.60   | 22  x        | 3000   |
#      | 05.2024 | 88.60   | 23   x       | 3000   |
#      # Sjekk over mai, april
#      | 06.2024 | 88.60   | 20           | 3000   |

  Scenario: Helt år, aktivitetsdager = 3

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.08.2023 | 31.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 08.2023 | 06.2024 | 3000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.08.2023 | 31.06.2024 | 3               |

    Når beregner

  Scenario: Helt år, aktivitetsdager = 2

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.08.2023 | 31.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 08.2023 | 06.2024 | 3000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.08.2023 | 31.06.2024 | 2               |

    Når beregner

  Scenario: Helt år, aktivitetsdager = 1

    Gitt følgende støndsperioder
      | Fom        | Tom        |
      | 01.08.2023 | 31.06.2024 |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 08.2023 | 06.2024 | 3000   |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitetsdager |
      | 01.08.2023 | 31.06.2024 | 1               |

    Når beregner