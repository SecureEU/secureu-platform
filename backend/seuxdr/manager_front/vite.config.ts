import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import fs from "fs";
import path from 'path';
 
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // ✅ Load .env variables safely
  const env = loadEnv(mode, process.cwd());
  process.env = { ...process.env, ...env };
 
  const useTLS = process.env.VITE_USE_TLS === "true";
 
  console.log("Loaded VITE_USE_TLS:", process.env.VITE_HTTPS_KEY);
  console.log("Loaded VITE_USE_TLS:", process.env.VITE_HTTPS_CERT);
  console.log("Loaded VITE_USE_TLS:", process.env.VITE_USE_TLS);
  // Only define key and crt if useTLS is true
  let httpsConfig = undefined;
 
  if (useTLS) {
    const keyPath = process.env.VITE_HTTPS_KEY;
    const crtPath = process.env.VITE_HTTPS_CERT;
    console.log("loading certificates");
 
    if (!keyPath || !crtPath) {
      throw new Error(
        "VITE_HTTPS_KEY and VITE_HTTPS_CERT must be defined when VITE_USE_TLS is true"
      );
    }
 
    httpsConfig = {
      key: fs.readFileSync(keyPath),
      cert: fs.readFileSync(crtPath),
    };
  }
  console.log(httpsConfig);
 
  return {
    plugins: [react()],
    server: {
      https: httpsConfig,
      port: 8080,
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
  };
});
 