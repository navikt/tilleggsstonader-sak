# language: no
# encoding: UTF-8

Egenskap: Splitt flere aktivitet til uker
  Scenario: Flere aktiviteter uten overlapp:
    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 07.01.2024 | 3               |
      | 15.01.2024 | 21.01.2024 | 2               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 2 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 3            |

    Så forvent følgende aktiviteter for uke med fom=15.01.2024 og tom=19.01.2024
      | Fom        | Tom        | Antall dager |
      | 15.01.2024 | 19.01.2024 | 2            |

  Scenario: Overlapp av aktiviteter
    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 14.01.2024 | 3               |
      | 08.01.2024 | 21.01.2024 | 2               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 3 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 3            |

    Så forvent følgende aktiviteter for uke med fom=08.01.2024 og tom=12.01.2024
      | Fom        | Tom        | Antall dager |
      | 08.01.2024 | 12.01.2024 | 3            |
      | 08.01.2024 | 12.01.2024 | 2            |

    Så forvent følgende aktiviteter for uke med fom=15.01.2024 og tom=19.01.2024
      | Fom        | Tom        | Antall dager |
      | 15.01.2024 | 19.01.2024 | 2            |

  Scenario: En aktivitetperiode inni annen aktivitet
    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 21.01.2024 | 3               |
      | 08.01.2024 | 14.01.2024 | 2               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 3 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 3            |

    Så forvent følgende aktiviteter for uke med fom=08.01.2024 og tom=12.01.2024
      | Fom        | Tom        | Antall dager |
      | 08.01.2024 | 12.01.2024 | 3            |
      | 08.01.2024 | 12.01.2024 | 2            |

    Så forvent følgende aktiviteter for uke med fom=15.01.2024 og tom=19.01.2024
      | Fom        | Tom        | Antall dager |
      | 15.01.2024 | 19.01.2024 | 3            |