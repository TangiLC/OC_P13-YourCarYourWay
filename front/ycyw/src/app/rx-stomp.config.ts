import { RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

export const myRxStompConfig: RxStompConfig = {
  brokerURL: undefined,
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: 'Bearer ' + localStorage.getItem('jwtToken') || '',
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 0,
  heartbeatOutgoing: 0,
  debug: (str) => console.log(str),
};
