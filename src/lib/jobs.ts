import { useEffect, useRef, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";

export type JobProgress = { id: number; key: string; stage: string; done: number; total: number | null };
type JobFinished = { id: number; key: string; error: string | null };

export function useJobs(prefix: string, onFinished: (key: string, error: string | null) => void) {
  const [jobs, setJobs] = useState<Record<string, JobProgress>>({});
  const finished = useRef(onFinished);
  finished.current = onFinished;

  useEffect(() => {
    invoke<{ id: number; key: string }[]>("jobs_list").then((list) => {
      const running: Record<string, JobProgress> = {};
      for (const j of list.filter((j) => j.key.startsWith(prefix))) {
        running[j.key] = { ...j, stage: "resolve", done: 0, total: null };
      }
      setJobs((cur) => ({ ...running, ...cur }));
    });
    const unProgress = listen<JobProgress>("job-progress", ({ payload }) => {
      if (payload.key.startsWith(prefix)) setJobs((j) => ({ ...j, [payload.key]: payload }));
    });
    const unFinished = listen<JobFinished>("job-finished", ({ payload }) => {
      if (!payload.key.startsWith(prefix)) return;
      setJobs(({ [payload.key]: _, ...rest }) => rest);
      finished.current(payload.key, payload.error === "cancelled" ? null : payload.error);
    });
    return () => {
      unProgress.then((f) => f());
      unFinished.then((f) => f());
    };
  }, [prefix]);

  const started = (key: string, id: number) =>
    setJobs((j) => ({ ...j, [key]: { id, key, stage: "resolve", done: 0, total: null } }));

  return { jobs, started };
}
