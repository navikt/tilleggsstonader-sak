# language: no
# encoding: UTF-8

Egenskap: Beregning av offentlig transport daglig reise

  Scenario: Forventer at tretti-dagersbillett lønner seg

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.01.2025 | 44                 | 366                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 31.01.2025 | 778   |

  Scenario: Forventer at tre tretti-dagersbillett lønner seg

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 15.03.2025 | 44                 | 366                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 31.01.2025 | 778   |
      | 01.02.2025 | 03.03.2025 | 778   |
      | 04.03.2025 | 15.03.2025 | 528   |

  Scenario: Forventer at syv-dagersbillett lønner seg

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 06.01.2025 | 12.01.2025 | 44                 | 366                    | 778                       | 5                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Fom        | Tom        | Beløp |
      | 06.01.2025 | 12.01.2025 | 366   |

  Scenario: Forventer at enkeltbillett lønner seg

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 31.01.2025 | 44                 | 366                    | 778                       | 1                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Fom        | Tom        | Beløp |
      | 01.01.2025 | 31.01.2025 | 440   |

  Scenario: Billigste alternativ over flere 30 dagers perioder

    Gitt følgende beregnings input for offentlig transport
      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 31.01.2025 | 15.03.2025 | 44                 | 366                    | 778                       | 3                         |

    Når beregner for daglig reise offentlig transport

    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
      | Fom        | Tom        | Beløp |
      | 31.01.2025 | 02.03.2025 | 778   |
      | 03.03.2025 | 15.03.2025 | 528   |

#  Scenario: TODO Forventer at ukeskort lønner seg på tvers av uker
#
#    Gitt følgende beregnings input for offentlig transport
#      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
#      | 09.01.2025 | 15.01.2025 | 44                | 366                  | 778                     | 5                         |
#
#    Når beregner for daglig reise offentlig transport
#
#    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
#      | Beløp |
#      | 366   |

#
#  Scenario: TODO Forventer at månedskort og tre enkeltbilletter lønner seg på tvers av måneder
#
#    Gitt følgende beregnings input for offentlig transport
#      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
#      | 01.01.2025 | 05.02.2025 | 44                | 366                  | 778                     | 5                         |
#
#    Når beregner for daglig reise offentlig transport
#
#    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
#      | Fom        | Tom        | Beløp |
#      | 01.01.2025 | 31.01.2025 | 778   |
#      | 01.02.2025 | 05.02.2025 | 264   |
#
#  Scenario: TODO ulike priser/reisedager som input
#
#    Gitt følgende beregnings input for offentlig transport
#      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
#      | 01.01.2025 | 15.01.2025 | 44                | 366                  | 778                     | 5                         |
#      | 16.01.2025 | 31.02.2025 | 93                | 466                  | 1012                    | 2                         |
#
#    Når beregner for daglig reise offentlig transport
#
#    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
#      | Fom | Tom | Beløp |
#
#  Scenario: TODO ulike priser og fremkomstmiddel (buss + tog) som input
#
#    Gitt følgende beregnings input for offentlig transport
#      | Fom        | Tom        | Pris enkeltbillett | Pris syv-dagersbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
#      | 01.01.2025 | 15.01.2025 | 44                | 366                  | 778                     | 5                         |
#      | 01.01.2025 | 15.02.2025 | 364               | 1500                 | 2500                    | 5                         |
#
#    Når beregner for daglig reise offentlig transport
#
#    Så forventer vi følgende beregningsrsultat for daglig resie offentlig transport
#      | Fom | Tom | Beløp |