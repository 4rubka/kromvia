import React from "react";
import ReactDOM from "react-dom/client";
import { invoke } from "@tauri-apps/api/core";
import App from "./App";
import "@fontsource/unbounded/600.css";
import "@fontsource/unbounded/800.css";
import "@fontsource-variable/manrope";
import "./styles/tokens.css";
import "./styles/app.css";

const report = (text: string) => invoke("launcher_log", { text }).catch(() => {});
window.addEventListener("error", (e) => report(e.error instanceof Error ? (e.error.stack ?? String(e.error)) : e.message));
window.addEventListener("unhandledrejection", (e) => e.reason instanceof Error && report(e.reason.stack ?? String(e.reason)));

class Recover extends React.Component<{ children: React.ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  componentDidCatch(error: Error) {
    report(error.stack ?? String(error));
  }
  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="recover">
        <img className="recover-bee" src="/bee/crash.webp" alt="" draggable={false} />
        <p>Something went wrong in Kromvia.</p>
        <button className="btn" onClick={() => location.reload()}>
          Reload
        </button>
        <button className="btn btn-ghost" onClick={() => invoke("launcher_log_open")}>
          Open log
        </button>
      </div>
    );
  }
}

function Ready() {
  React.useEffect(() => {
    invoke("ui_ready").catch(() => {});
  }, []);
  return null;
}

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <Recover>
      <App />
    </Recover>
    <Ready />
  </React.StrictMode>,
);
