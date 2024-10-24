# language: no
# encoding: UTF-8

Egenskap: Beregning av stønadsperioder

  Bakgrunn:
    Gitt følgende søknadsdato: 01.01.2023

  Regel: Stønadsperiode skal være snittet av målgruppe og aktivitet

    Scenario: Enkleste case der målgruppe og aktivitet overlapper perfekt
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.03.2023 | TILTAK    | AAP       |

    Scenario: Det finnes ikke noen vilkårsperiode for aktivitet
      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Det finnes ingen kombinasjon av aktiviteter og målgrupper som kan brukes til å lage perioder med overlapp

    Scenario: Aktivitetsperioden er kortere enn målgruppeperioden
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 15.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 15.02.2023 | TILTAK    | AAP       |

    Scenario: Aktivitetsperioden er lengre enn målgruppeperioden
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2024 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 15.02.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 15.02.2023 | TILTAK    | AAP       |

    Scenario: Aktivitet omsluttes av målgruppe
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.02.2023 | 31.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 15.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.02.2023 | 31.02.2023 | TILTAK    | AAP       |

    Scenario: Ingen overlapp mellom målgruppe og aktivitet
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.02.2023 | 21.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.03.2023 | 15.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Fant ingen gyldig overlapp mellom gitte aktiviteter og målgrupper

  Regel: Stønadsperioden er avhengig av kombinasjonen av typen aktivitet og typen målgruppe

    Scenario: Ingen gyldig kombinasjon av aktivitet og målgruppe
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type               |
        | 01.01.2023 | 31.03.2023 | REELL_ARBEIDSSØKER |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Det finnes ingen kombinasjon av aktiviteter og målgrupper som kan brukes til å lage perioder med overlapp

    Scenario: Flere aktiviteter og en målgrupper, men kun én gyldig kombinasjon
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type               |
        | 01.01.2023 | 31.03.2023 | REELL_ARBEIDSSØKER |
        | 01.01.2023 | 31.03.2023 | TILTAK             |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.03.2023 | TILTAK    | AAP       |

    Scenario: Flere gyldige sammenhengende aktiviter skal (foreløpig) gi feilmelding
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 28.02.2023 | TILTAK |
        | 03.03.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 31.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Foreløpig håndterer vi kun én gyldig kombinasjon av aktivitet og målgruppe

    Scenario: Én aktiviteter og flere målgrupper, men kun én gyldig kombinasjon
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 31.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type            |
        | 01.01.2023 | 31.03.2023 | AAP             |
        | 01.01.2023 | 31.03.2023 | OVERGANGSSTØNAD |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 31.03.2023 | TILTAK    | AAP       |

  Regel: Stønadsperioden skal maks kunne gå tre måneder tilbake i tid fra første dagen i søknadsmåneden

    Scenario: Tre måneder fra søknadsdato ligger midt i vilkårsperiodene
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.02.2024 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2024 | AAP  |

      Gitt følgende søknadsdato: 15.01.2024

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.10.2023 | 01.02.2024 | TILTAK    | AAP       |

    Scenario: Aktivitet og målgruppe er overlapper tre måneder fra søknadsdato, men ikke den første dagen i måneden tre måneder fra søknadsdato
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 10.01.2023 | 01.02.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 10.01.2023 | 01.02.2023 | AAP  |

      Gitt følgende søknadsdato: 15.04.2023

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 10.01.2023 | 01.02.2023 | TILTAK    | AAP       |

    Scenario: Tre måneder fra søknadsdato er etter vilkårsperiodene
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.01.2024 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2024 | AAP  |

      Gitt følgende søknadsdato: 01.01.2025

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Aktivitet og målgruppe ligger lengre enn tre månder tilbake i tid fra søknadsdato

  Regel: Stønadsperioden skal foreslås for flere like aktiviteter eller målgrupper som er rett etter hverandre
    Scenario: To like aktiviter som er rett etter hverandre og en målgruppe
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.02.2023 | TILTAK |
        | 01.02.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.03.2023 | AAP  |


      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 01.03.2023 | TILTAK    | AAP       |


    Scenario: En aktivit og to like målgrupper som er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 02.02.2023 | 01.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 01.03.2023 | TILTAK    | AAP       |

    Scenario: To aktiviteter og to like målgrupper som er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.02.2023 | TILTAK |
        | 01.02.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 02.02.2023 | 01.03.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 01.03.2023 | TILTAK    | AAP       |

    Scenario: Tre aktiviteter og to like målgrupper som er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.02.2023 | TILTAK |
        | 01.02.2023 | 01.03.2023 | TILTAK |
        | 02.03.2023 | 01.04.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 02.02.2023 | 01.04.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende stønadsperioder
        | Fom        | Tom        | aktivitet | målgruppe |
        | 01.01.2023 | 01.04.2023 | TILTAK    | AAP       |

    Scenario: En aktivitet og to like målgrupper som ikke er rett etter hverandre
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 03.02.2023 | 01.04.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Foreløpig håndterer vi kun én gyldig kombinasjon av aktivitet og målgruppe

    Scenario: En aktivitet og tre like målgrupper hvor to er like etter hverandre, men den siste er med opphold
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 03.02.2023 | 01.04.2023 | AAP  |
        | 03.04.2023 | 01.05.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Foreløpig håndterer vi kun én gyldig kombinasjon av aktivitet og målgruppe

    Scenario: En aktivitet og to like målgrupper som overlapper
      Gitt følgende vilkårsperioder med aktiviteter
        | Fom        | Tom        | type   |
        | 01.01.2023 | 01.03.2023 | TILTAK |

      Gitt følgende vilkårsperioder med målgrupper
        | Fom        | Tom        | type |
        | 01.01.2023 | 01.02.2023 | AAP  |
        | 15.01.2023 | 01.04.2023 | AAP  |

      Når forslag til stønadsperioder lages

      Så forvent følgende beregningsfeil: Foreløpig håndterer vi kun én gyldig kombinasjon av aktivitet og målgruppe