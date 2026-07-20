# language: no
# encoding: UTF-8

Egenskap: Splitt en aktivitet med full aktivitet til uker

  Scenario: En fullstendig uke
    # Mål med test: en aktivitet over en uke skal resultere i en uke med helg fjernet

    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 07.01.2024 | 5               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 1 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 5            |

  Scenario: Flere fullstendige uker
    # Mål med test: en aktivitet over to uker skal splittes på to uker uten å miste andre dager enn helg

    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 01.01.2024 | 14.01.2024 | 5               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 2 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 5            |

    Så forvent følgende aktiviteter for uke med fom=08.01.2024 og tom=12.01.2024
      | Fom        | Tom        | Antall dager |
      | 08.01.2024 | 12.01.2024 | 5            |

  Scenario: Flere ikke fullstendige uker
    # Mål med test: ikke fullstendige uker skal gi kortere perioder enn 5 dager
    # Kun plass til 3 arbeidsdager i første uke og 4 i andre.

    Gitt disse aktivitetene
      | Fom        | Tom        | Aktivitetsdager |
      | 03.01.2024 | 11.01.2024 | 5               |

    Når splitter aktiviteter per uke

    Så forvent at aktivitetene ble splittet til 2 uker

    Så forvent følgende aktiviteter for uke med fom=01.01.2024 og tom=05.01.2024
      | Fom        | Tom        | Antall dager |
      | 03.01.2024 | 05.01.2024 | 3            |

    Så forvent følgende aktiviteter for uke med fom=08.01.2024 og tom=12.01.2024
      | Fom        | Tom        | Antall dager |
      | 08.01.2024 | 11.01.2024 | 4            |