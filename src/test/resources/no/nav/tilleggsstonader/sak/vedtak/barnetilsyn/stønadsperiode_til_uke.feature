# language: no
# encoding: UTF-8

Egenskap: Splitt stønadsperioder til uker

  Scenario: En stønadsperiode - en uke

    Gitt disse stønadsperiodene
      | Fom        | Tom        |
      | 01.01.2024 | 07.01.2024 |

    Når splitter stønadsperiode per uke
    
    Så forvent følgende stønadsperioder for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 5            |

  Scenario: En stønadsperiode - flere uker

    Gitt disse stønadsperiodene
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Når splitter stønadsperiode per uke

    Så forvent følgende stønadsperioder for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 5            |

    Så forvent følgende stønadsperioder for uke med fom=08.01.2024 og tom=12.01.2024
      | Fom        | Tom        | Antall dager |
      | 08.01.2024 | 12.01.2024 | 5            |

    Så forvent følgende stønadsperioder for uke med fom=15.01.2024 og tom=19.01.2024
      | Fom        | Tom        | Antall dager |
      | 15.01.2024 | 19.01.2024 | 5            |

    Så forvent følgende stønadsperioder for uke med fom=22.01.2024 og tom=26.01.2024
      | Fom        | Tom        | Antall dager |
      | 22.01.2024 | 26.01.2024 | 5            |

    Så forvent følgende stønadsperioder for uke med fom=29.01.2024 og tom=02.02.2024
      | Fom        | Tom        | Antall dager |
      | 29.01.2024 | 31.01.2024 | 3            |


    Scenario: En stønadsperiode - uke krysser år
      Gitt disse stønadsperiodene
        | Fom        | Tom        |
        | 02.01.2025 | 03.01.2025 |

      Når splitter stønadsperiode per uke

      Så forvent følgende stønadsperioder for uke med fom=30.12.2024 og tom=03.01.2025
        | Fom        | Tom        | Antall dager |
        | 02.01.2025 | 03.01.2025 | 2            |