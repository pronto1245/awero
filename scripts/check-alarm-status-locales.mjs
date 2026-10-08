import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const androidLocales = {
  en: "apps/android/app/src/main/res/values/strings.xml",
  ru: "apps/android/app/src/main/res/values-ru/strings.xml",
  "pt-BR": "apps/android/app/src/main/res/values-pt-rBR/strings.xml",
  fr: "apps/android/app/src/main/res/values-fr/strings.xml",
  de: "apps/android/app/src/main/res/values-de/strings.xml",
  es: "apps/android/app/src/main/res/values-es/strings.xml",
};
const iosLocales = {
  en: "apps/ios/AWERO/en.lproj/Localizable.strings",
  ru: "apps/ios/AWERO/ru.lproj/Localizable.strings",
  "pt-BR": "apps/ios/AWERO/pt-BR.lproj/Localizable.strings",
  fr: "apps/ios/AWERO/fr.lproj/Localizable.strings",
  de: "apps/ios/AWERO/de.lproj/Localizable.strings",
  es: "apps/ios/AWERO/es.lproj/Localizable.strings",
};
const expectedAndroid = new Set([
  "app_name",
  "alarm_state_scheduled",
  "alarm_state_permission",
  "alarm_state_missing",
  "alarm_state_invalid",
  "alarm_state_checking",
  "alarm_retry",
  "alarm_open_settings",
]);
const expectedIOS = new Set([
  "alarm.status.scheduled",
  "alarm.status.notificationFallback",
  "alarm.status.actionRequired",
  "alarm.status.notScheduled",
  "alarm.status.disabled",
  "alarm.retry",
  "alarm.openSettings",
  "alarm.status.errorTitle",
  "alarm.status.errorBody",
]);

function assertKeys(platform, locale, path, expected, pattern) {
  const source = readFileSync(resolve(path), "utf8");
  const actual = new Set([...source.matchAll(pattern)].map((match) => match[1]));
  const missing = [...expected].filter((key) => !actual.has(key));
  const extra = [...actual].filter((key) => !expected.has(key));
  if (missing.length || extra.length) {
    throw new Error(
      `${platform}/${locale}: missing [${missing.join(", ")}], extra [${extra.join(", ")}]`
    );
  }
}

for (const [locale, path] of Object.entries(androidLocales)) {
  assertKeys("Android", locale, path, expectedAndroid, /<string\\s+name="([^"]+)"/g);
}
for (const [locale, path] of Object.entries(iosLocales)) {
  assertKeys("iOS", locale, path, expectedIOS, /^\\s*"([^"]+)"\\s*=/gm);
}

console.log("Alarm status locale parity: PASS (en, ru, pt-BR, fr, de, es)");
