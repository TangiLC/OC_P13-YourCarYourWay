import { RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

import { environment } from '../environments/environment';

export const myRxStompConfig: RxStompConfig = {
  brokerURL: undefined,
  webSocketFactory: () => new SockJS(`${environment.apiUrl}/ws`),
  connectHeaders: {
    Authorization: 'Bearer ' + localStorage.getItem('jwtToken') || '',
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 0,
  heartbeatOutgoing: 0,
  debug: (str) => console.log(str),
};
