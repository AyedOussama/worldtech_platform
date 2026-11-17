import { bootstrapApplication } from '@angular/platform-browser';
import { createAppConfig } from './app/app.config';
import { App } from './app/app';
import { loadRuntimeConfig } from './app/core/config/runtime-config';
import { showStartupFailure } from './app/core/config/startup-failure';
import { captureActionFragment } from './app/core/auth/action-fragment';

// Nothing in the router or HTTP layer starts before the public configuration is valid.
captureActionFragment(window);
loadRuntimeConfig()
  .then((config) => bootstrapApplication(App, createAppConfig(config)))
  .catch(() => showStartupFailure(document));
