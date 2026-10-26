import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { RuntimeConfig } from './app/core/runtime-config';

// config.json is read at startup, not baked in at build time, so one image runs in every environment
fetch('/config.json')
  .then((response) => {
    if (!response.ok) {
      throw new Error(`Could not load /config.json (HTTP ${response.status})`);
    }
    return response.json() as Promise<RuntimeConfig>;
  })
  .then((config) => bootstrapApplication(AppComponent, appConfig(config)))
  .catch((error) => console.error(error));
