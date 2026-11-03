import type { Metadata } from "next";
import { getLang } from "@/lib/i18n";

const T = {
  en: {
    meta: { title: "Terms", description: "The rules for using Kromvia." },
    head: "Terms of use",
    lede: "The short version: play fair, be decent to people, and don't try to break the service.",
    mcH: "Minecraft",
    mc: "Kromvia is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft. To play online you need your own Minecraft account, and the Minecraft EULA applies to how you use the game.",
    accountH: "Your account",
    rules: [
      "Pick a nickname that doesn't pretend to be someone else and isn't offensive.",
      "Don't spam, harass or threaten people in chat, and don't share anything illegal through share codes or screenshots.",
      "Don't try to get around rate limits, read other people's data, or make accounts in bulk.",
    ],
    ban: "If you break these rules we can remove your nickname, shares or the whole account.",
    modsH: "Mods and content",
    mods: "Mods, resource packs and modpacks come from Modrinth and CurseForge and belong to their authors under their own licences. Kromvia downloads them for you but doesn't own them. Servers have their own rules: check them before you use any client feature there.",
    warrantyH: "No warranty",
    warranty: "Kromvia is provided as is. We work hard to keep your worlds and files safe, but back up anything you can't lose. The launcher has backups built in.",
    changesH: "Changes",
    changes: "If these terms change, the new version goes on this page before it applies.",
  },
  uk: {
    meta: { title: "Умови", description: "Правила користування Kromvia." },
    head: "Умови використання",
    lede: "Коротко: грай чесно, поважай людей і не намагайся зламати сервіс.",
    mcH: "Minecraft",
    mc: "Kromvia не є офіційним продуктом Minecraft, не схвалений Mojang чи Microsoft і не повʼязаний з ними. Для гри онлайн потрібен власний акаунт Minecraft, а на використання гри поширюється EULA Minecraft.",
    accountH: "Твій акаунт",
    rules: [
      "Вибирай нік, який не видає тебе за когось іншого і не є образливим.",
      "Не спам, не цькуй і не погрожуй людям у чаті, не поширюй нічого незаконного через коди чи скріншоти.",
      "Не обходь ліміти запитів, не намагайся читати чужі дані й не створюй акаунти пачками.",
    ],
    ban: "За порушення ми можемо прибрати нік, поширення або весь акаунт.",
    modsH: "Моди й контент",
    mods: "Моди, ресурспаки й модпаки беруться з Modrinth і CurseForge і належать їхнім авторам за їхніми ліцензіями. Kromvia завантажує їх для тебе, але не володіє ними. У серверів свої правила: перевір їх, перш ніж користуватися функціями клієнта.",
    warrantyH: "Без гарантій",
    warranty: "Kromvia надається «як є». Ми стараємося берегти твої світи й файли, але роби бекапи всього, що боїшся втратити. У лаунчері бекапи вбудовані.",
    changesH: "Зміни",
    changes: "Якщо умови зміняться, нова версія зʼявиться на цій сторінці до того, як почне діяти.",
  },
};

export async function generateMetadata(): Promise<Metadata> {
  return T[await getLang()].meta;
}

export default async function Terms() {
  const t = T[await getLang()];
  return (
    <div className="wrap">
      <header className="page-head">
        <h1>{t.head}</h1>
        <p>{t.lede}</p>
      </header>
      <div className="prose">
        <h2>{t.mcH}</h2>
        <p>{t.mc}</p>

        <h2>{t.accountH}</h2>
        <ul>
          {t.rules.map((x) => (
            <li key={x}>{x}</li>
          ))}
        </ul>
        <p>{t.ban}</p>

        <h2>{t.modsH}</h2>
        <p>{t.mods}</p>

        <h2>{t.warrantyH}</h2>
        <p>{t.warranty}</p>

        <h2>{t.changesH}</h2>
        <p>{t.changes}</p>
      </div>
    </div>
  );
}
