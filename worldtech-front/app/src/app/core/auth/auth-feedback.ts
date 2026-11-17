import { HttpErrorResponse } from '@angular/common/http';
import { ApiFailure, toApiFailure } from '../http/api-failure';
import { IdentityProtocolError } from './identity-model';

export interface AuthFeedback { readonly message: string; readonly retryAfter: number; }
export function authFeedback(error: unknown): AuthFeedback {
  if (error instanceof IdentityProtocolError) return { message: 'La réponse du service ne permet pas de vérifier votre accès. Aucun espace privé n’a été ouvert.', retryAfter: 0 };
  const value = error instanceof HttpErrorResponse ? toApiFailure(error) : error as Partial<ApiFailure> | null;
  const status = value && typeof value === 'object' ? value.status : undefined;
  const messages: Record<number, string> = {
    0: 'Le service est injoignable. Le résultat de cette opération n’est pas confirmé. Aucun nouvel essai automatique.',
    400: 'Vérifiez les informations saisies. Le lien ou le code peut être invalide.',
    401: 'Les informations fournies ne permettent pas de confirmer votre accès. Vérifiez-les ou reprenez la connexion.',
    403: 'Cette action est refusée. Votre session, le code ou la protection de la requête ne permet pas cette opération. Aucun nouvel essai automatique.',
    409: 'Cette action n’est plus disponible dans l’état actuel. Votre saisie est conservée.',
    412: 'Les informations ont changé. Vérifiez l’état actuel avant de recommencer.',
    429: 'Trop de tentatives. Attendez avant de réessayer.',
  };
  return { message: messages[status ?? -1] ?? 'Le service est temporairement indisponible. Le résultat de l’opération n’est pas confirmé.',
    retryAfter: value && typeof value === 'object' && typeof value.retryAfterSeconds === 'number' ? value.retryAfterSeconds : 0 };
}
