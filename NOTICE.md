# BandCalendar data and third-party notices

BandCalendar is an independent personal calendar. It is not endorsed by Xiaomi,
the United Nations, any government, or the maintainers of its data sources.
Original application code is provided under PolyForm Noncommercial 1.0.0.
Third-party software and data retain their own rights and terms.

- Worldwide country/territory-level public holidays: **Python holidays 0.87**,
  MIT, https://holidays.readthedocs.io/en/latest/ . Generated offline for
  2025–2028 with `observed=True`, no subdivision, English names where available.
  249 country/territory codes are included, excluding CN, which is handled
  separately. This is not every local, ethnic, school or religious festival.
  Future dates, especially dates based on religious sightings, may be estimates.
  Observed substitute days retain their source names. The MIT notice is in
  THIRD_PARTY_HOLIDAYS_0.txt and in both installable applications.
- Chinese territory labels: **Babel 2.17.0 / Unicode CLDR**, BSD-3-Clause.
  Labels do not imply a political position. See THIRD_PARTY_BABEL_0.txt.
- Lunar conversion and 24 solar-term civil dates: **Hong Kong Observatory**,
  Government of the Hong Kong Special Administrative Region (HKSARG).
  HKSARG is the intellectual-property rights owner of these materials.
  The materials form part of the HKO websites published by HKO at
  https://www.hko.gov.hk and https://www.weather.gov.hk .
  Calendar CSV data are obtained through the open-data endpoint described at
  https://data.gov.hk/en-data/dataset/hk-hko-rss-gregorian-lunar-calendar-conversion-table .
  Solar-term dates are transcribed from HKO's Gregorian-lunar text tables.
  HKO's intellectual-property notice, disclaimer and noncommercial terms are
  reproduced in `data/HKO-NOTICES.txt` and included with both applications.
  https://www.hko.gov.hk/en/readme/readme.htm
  https://www.hko.gov.hk/en/appweb/applink.htm
  https://data.gov.hk/en/terms-and-conditions
- Mainland China rest days and compensatory workdays: factual dates manually
  transcribed from the State Council General Office's notices for 2025 and 2026:
  https://www.gov.cn/zhengce/zhengceku/202411/content_6986383.htm
  https://www.gov.cn/zhengce/content/202511/content_7047090.htm .
  These are holiday-arrangement days, not a classification of statutory pay
  entitlements. Normal weekdays/weekends are not an individual's work schedule.
  Official rest/work arrangements for 2027–2028 are NOT included in this build.
- Selected international observances: factual dates cross-checked against the
  UN list, https://www.un.org/en/observances/list-days-weeks . Folk observances
  (e.g. Christmas Eve, Valentine's Day, Halloween) are separately described as
  international/folk festivals; they are not claimed to be UN observances.
  No endorsement or UN insignia is used.
- Xiaomi XMS Wearable SDK 1.4 is obtained from Xiaomi's official interconnect
  demo; its vendor terms apply. The binary is excluded from the source ZIP.
  `tools/prepare-sdk.ps1` obtains it for a local build.
- Xiaomi aiot-toolkit 2.0.5 and its dependencies retain their respective terms.
  JSON-java is used only in JVM tests; https://github.com/stleary/JSON-java .

All data are provided for reference without a guarantee of completeness or
currency. Verify important plans with the relevant local official notices.
No global/national festival changes the Mainland China rest/work badges.
No signing secrets, captured device logs or device identifiers are included
in the deliverable source archive.
