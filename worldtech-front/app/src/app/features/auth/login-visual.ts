import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Presentation only: no auth, navigation, fixture data or HTTP requests. */
@Component({
  selector: 'wt-login-visual',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-visual.html',
  styleUrl: './login-visual.scss',
})
export class LoginVisual {}
