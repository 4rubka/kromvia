# Tauri + React + Typescript

## Building

The launcher bundles the built-in client mod, so build it first (Loom needs Java 25 to run):

```sh
cd client && ./gradlew bundle   # -> src-tauri/resources/kromvia-client.jar
cd .. && bun run tauri dev
```

Microsoft sign-in needs an Azure app id at build time: `KROMVIA_MS_CLIENT_ID=<id> bun run tauri build`.

This template should help get you started developing with Tauri, React and Typescript in Vite.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Tauri](https://marketplace.visualstudio.com/items?itemName=tauri-apps.tauri-vscode) + [rust-analyzer](https://marketplace.visualstudio.com/items?itemName=rust-lang.rust-analyzer)
