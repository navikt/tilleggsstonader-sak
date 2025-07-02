# language: no
# encoding: UTF-8

Egenskap: Forslag av vedtaksperioder med behold id for å kunne bruke i revurdering med tidligstEndring

  Bakgrunn:

    Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
      | Fom        | Tom        | type   |
      | 01.01.2023 | 31.03.2023 | TILTAK |

    Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
      | Fom        | Tom        | type |
      | 01.01.2023 | 31.03.2023 | AAP  |

    Gitt følgende vilkår for vedtaksforslag
      | Fom        | Tom        | Resultat |
      | 01.01.2023 | 31.03.2023 | OPPFYLT  |

    Gitt følgende tidligere vedtaksperioder for vedtaksforslag
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 15.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra før tidligere vedtaksperioder

    Når forslag til vedtaksperioder behold id lages tidligstEndring=28.02.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | -1 | 28.02.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra 1 dag etter TOM i tidligere vedtaksperiode - skal forlenge siste perioden

    Når forslag til vedtaksperioder behold id lages tidligstEndring=16.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra 2 dager etter TOM i tidligere vedtaksperiode - skal legge til ny periode etter tidligere perioder

    Når forslag til vedtaksperioder behold id lages tidligstEndring=17.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 15.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
      | -1 | 17.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra FOM på tidligere vedtaksperiode

    Når forslag til vedtaksperioder behold id lages tidligstEndring=01.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra midt i tidligere vedtaksperiode

    Når forslag til vedtaksperioder behold id lages tidligstEndring=10.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra dagen før TOM i tidligere vedtaksperiode

    Når forslag til vedtaksperioder behold id lages tidligstEndring=14.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

  Scenario: Revurder fra TOM i tidligere vedtaksperiode

    Når forslag til vedtaksperioder behold id lages tidligstEndring=15.03.2023

    Så forvent følgende vedtaksperioder med riktig id
      | Id | Fom        | Tom        | aktivitet | målgruppe           |
      | 1  | 01.01.2023 | 31.01.2023 | UTDANNING | ENSLIG_FORSØRGER    |
      | 2  | 01.03.2023 | 31.03.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |
