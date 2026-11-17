export function showStartupFailure(document: Document): void {
  const root = document.querySelector('wt-root');
  if (!root) return;

  document.title = 'Configuration indisponible — WorldTech';
  const main = document.createElement('main');
  main.className = 'startup-failure';
  main.setAttribute('role', 'alert');
  const title = document.createElement('h1');
  title.textContent = 'Configuration indisponible';
  const message = document.createElement('p');
  message.textContent = 'WorldTech ne peut pas démarrer. Réessayez ou contactez votre administrateur.';
  const retry = document.createElement('button');
  retry.type = 'button';
  retry.textContent = 'Réessayer';
  retry.addEventListener('click', () => document.defaultView?.location.reload());
  main.append(title, message, retry);
  root.replaceChildren(main);
}
