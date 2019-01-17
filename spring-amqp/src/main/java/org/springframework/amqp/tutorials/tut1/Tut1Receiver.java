/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.amqp.tutorials.tut1;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * @author Gary Russell
 * @author Scott Deeg
 * @author Wayne Lund
 * Alio 에서는 KKO - AlioSendRequestConsumer 에
  @RabbitListener(queues = "#{requestQueueName}")
  	public void getMessage(String request) {	service.sendMessageAndResponseSave(request);}
  이 처럼 선언했다.

 */

@RabbitListener(queues = "hello")
// Queue 이름을 전달하여, 해당 큐를 리스닝 하도록 한다.
// queue 이름을 지정하지 않으면, 아무 데이터도 못받을까? 못받는다.
// sender queue name  O , receiver queue name  O  성공
// sender queue name  O , receiver queue name  x  실패
// sender queue name  x , receiver queue name  x  실패
public class Tut1Receiver {

	@RabbitHandler
	//We then annotate our receive method with @RabbitHandler passing in the payload that has been pushed to the queue
	public void receive(String in) {
		System.out.println(" [x] Received '" + in + "'");
	}

}
