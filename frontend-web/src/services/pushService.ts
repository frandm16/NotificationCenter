import { api } from './api';

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export const pushService = {
  isSupported(): boolean {
    return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  },

  async registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
    if (!('serviceWorker' in navigator)) return null;
    try {
      return await navigator.serviceWorker.register('/sw.js');
    } catch {
      return null;
    }
  },

  async getVapidKey(): Promise<{ publicKey: string; enabled: boolean }> {
    const { data } = await api.get<{ publicKey: string; enabled: boolean }>('/push/vapid-public-key');
    return data;
  },

  async isSubscribed(): Promise<boolean> {
    if (!this.isSupported()) return false;
    try {
      const reg = await navigator.serviceWorker.getRegistration();
      if (!reg) return false;
      const sub = await reg.pushManager.getSubscription();
      return !!sub;
    } catch {
      return false;
    }
  },

  async enablePush(): Promise<boolean> {
    if (!this.isSupported()) return false;

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') return false;

    const { publicKey, enabled } = await this.getVapidKey();
    if (!enabled || !publicKey) return false;

    const registration = await this.registerServiceWorker();
    if (!registration) return false;

    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey),
    });

    const subJson = subscription.toJSON();
    await api.post('/push/subscriptions', {
      endpoint: subscription.endpoint,
      keys: {
        p256dh: subJson.keys?.p256dh,
        auth: subJson.keys?.auth,
      },
    });

    return true;
  },
};
