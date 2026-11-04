import React from "react";
import { invoke } from "@tauri-apps/api/core";
import { useI18n } from "../i18n";

export const report = (text: string) => invoke("launcher_log", { text }).catch(() => {});

export class Recover extends React.Component<{ children: React.ReactNode; page?: boolean }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  componentDidCatch(error: Error) {
    report(error.stack ?? String(error));
  }
  render() {
    if (!this.state.failed) return this.props.children;
    const { t } = useI18n();
    return (
      <div className={this.props.page ? "recover recover-page" : "recover"}>
        <img className="recover-bee" src="/bee/crash.webp" alt="" draggable={false} />
        <p>{t("recover.failed")}</p>
        <button
          className="btn"
          onClick={() => (this.props.page ? this.setState({ failed: false }) : location.reload())}
        >
          {t("recover.reload")}
        </button>
        <button className="btn btn-ghost" onClick={() => invoke("launcher_log_open")}>
          {t("recover.openLog")}
        </button>
      </div>
    );
  }
}
