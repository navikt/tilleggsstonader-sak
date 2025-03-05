# language: no
# encoding: UTF-8

Egenskap: Splitt vedtaksperioder til uker

  Scenario: En vedtakssperiode - en uke

    Gitt disse vedtaksperiodene
      | Fom        | Tom        |
      | 01.01.2024 | 07.01.2024 |

    Når splitter vedtaksperiodeBeregning per uke

    Så forvent følgende vedtaksperioder per uke
      | Fom uke    | Tom uke    | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 01.01.2024 | 05.01.2024 | 5            |

  Scenario: En vedtakssperiode - flere uker

    Gitt disse vedtaksperiodene
      | Fom        | Tom        |
      | 01.01.2024 | 31.01.2024 |

    Når splitter vedtaksperiodeBeregning per uke

    Så forvent følgende vedtaksperioder per uke
      | Fom uke    | Tom uke    | Fom        | Tom        | Antall dager |
      | 01.01.2024 | 05.01.2024 | 01.01.2024 | 05.01.2024 | 5            |
      | 08.01.2024 | 12.01.2024 | 08.01.2024 | 12.01.2024 | 5            |
      | 15.01.2024 | 19.01.2024 | 15.01.2024 | 19.01.2024 | 5            |
      | 22.01.2024 | 26.01.2024 | 22.01.2024 | 26.01.2024 | 5            |
      | 29.01.2024 | 02.02.2024 | 29.01.2024 | 31.01.2024 | 3            |


  Scenario: En vedtaksperiode - uke krysser år
    Gitt disse vedtaksperiodene
      | Fom        | Tom        |
      | 02.01.2025 | 03.01.2025 |

    Når splitter vedtaksperiodeBeregning per uke

    Så forvent følgende vedtaksperioder per uke
      | Fom uke    | Tom uke    | Fom        | Tom        | Antall dager |
      | 30.12.2024 | 03.01.2025 | 02.01.2025 | 03.01.2025 | 2            |


  Scenario: En vedtaksperiode - kun èn helg ikke returnerer noen uker ved splitting
    Gitt disse vedtaksperiodene
      | Fom        | Tom        |
      | 06.01.2024 | 07.01.2024 |

    Når splitter vedtaksperiodeBeregning per uke

    Så forvent følgende vedtaksperioder per uke
      | Fom uke | Tom uke | Fom | Tom | Antall dager |