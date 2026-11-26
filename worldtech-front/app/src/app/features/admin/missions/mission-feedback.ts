import type { ApiFailure } from '../../../core/http/api-failure';

function failureOf(error: unknown): Partial<ApiFailure> | null {
  return typeof error === 'object' && error !== null ? error as Partial<ApiFailure> : null;
}

export function isNetworkFailure(error: unknown): boolean {
  return failureOf(error)?.kind === 'network';
}

export function missionFailureMessage(error: unknown): string {
  const failure = failureOf(error);
  switch (failure?.status) {
    case 0: return 'Le service est injoignable. Le résultat de la commande n’est pas confirmé.';
    case 400: return 'Les informations envoyées ne sont pas acceptées. Vérifiez les champs et réessayez.';
    case 401: return 'La session a expiré. Reconnectez-vous avant de reprendre.';
    case 403: return 'Action refusée. La session ADMIN ou sa preuve MFA n’est plus valide.';
    case 404: return 'Compte, site ou mission introuvable pour cette session.';
    case 409: return 'Cette action n’est pas possible dans l’état actuel de la mission. Rechargez-la.';
    case 412: return 'La mission a changé depuis sa lecture. Rechargez avant de recommencer.';
    case 429: return 'Trop de tentatives. Attendez avant de réessayer.';
    default: return 'Le service est temporairement indisponible. Le résultat de la commande n’est pas confirmé.';
  }
}
