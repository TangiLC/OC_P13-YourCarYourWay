import { RxStomp } from '@stomp/rx-stomp';
import { myRxStompConfig } from './my-rx-stomp.config';
import { Provider } from '@angular/core';

export function rxStompServiceFactory(): RxStomp {
  const rxStomp = new RxStomp();
  rxStomp.configure(myRxStompConfig);
  rxStomp.activate();
  return rxStomp;
}

export const provideRxStomp: Provider = {
  provide: RxStomp,
  useFactory: rxStompServiceFactory,
};
