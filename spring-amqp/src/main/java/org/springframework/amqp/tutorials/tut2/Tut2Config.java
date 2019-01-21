/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.amqp.tutorials.tut2;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Gary Russell
 * @author Scott Deeg
 */
@Profile({"tut2", "work-queues"})
@Configuration
public class Tut2Config {

	@Bean
	public Queue hello() {
		return new Queue("tut.hello");
	}

	@Profile("receiver")
	private static class ReceiverConfig {
		/*
Work Queues(aka : Task Queues) 이전에 메인 아이디어는 직접적으로 자원 집약적(resource-intensive) 업무을 즉시 하거나,
그 작업을 완료하는데 까지 대기하는 것들을 피하기 위함이다.

하나의 메시지로 업무를 캡슐화 하고, 그걸 큐에 보냅니다.
백그라운드에서 동작하는 Work process는 그 업무를 끄집어 내고 최종적으로 작업을 실행합니다.
많은 작업자들을 운영할때,위 업무들은 작업자들사이에 공유 될 것입니다.

다수의 Consumer를 만들어 대기를 시키면 RabbitMQ에서는 Round-Robin 분배를 통해, 비어있는 Consumer에게 메시지를 보냅니다.
(QoS(Quality of Service)는 다른 응용 프로그램, 사용자, 데이터 흐름 등에 우선 순위를 정하여,
데이터 전송에 특정 수준의 성능을 보장하기 위한 능력을 말한다.)
출처: http://kimseunghyun76.tistory.com/424?category=687132 [하루에 하나씩.....]

특징
1.Round-robin dispatching
- 병렬 처리
Task queue를 사용하는 이점 중에 하나는 쉽게 병렬 작업을 할수 있는 능력이다.
작업의 잔무를 처리하고 있다면, 작업자를 좀 더 추가하여 쉽게 규모를 키울수 있다.
동시에 두개의 Worker 클래스를 실행하자. 이들은 모두 queue로부터 메시지를 얻으려고 하겠지만, 정확시 어떻게?
- 균등 분배
두개는 Work 프로그램 돌리고, 하나는 newTasks 를 게시하면, 순서대로 차근차근 나눠서 분배가 된다.

RabbitMQ는 기본적으로 순차적으로  consumer에게 메세지를 각각 전달한다. (이게 Round-Robin 방식이다.)

참고) Fair dispatch(공평한 분배)
여러 consumer에게 round robin할 때, 번갈아가면서 메시지를 전달하지만 완전히 공평하진 않음
(ex. 매 홀수는 데이터크기가 크고, 매 짝수는 데이터크기가 작은 경우 등)
때문에 busy한 서버에게 메시지를 계속 전달하지 않도록 prefetchCount라는 개념사용.
prefetchCount가 1일때는 아직 ack를 받지 못한 메시지가 1개라도 있으면 다시 그 consumer에게 메시지 할당하지 않음.
즉 prefetchCount는 동시에 보내는 메시지 양임
cf) prefetchCount는 2가지가 있음.
a. 각 소비자들이 처리할 작업의 제한 (위에 설명한)
b. 채널의 모든 소비자 사이에서 공유되는 수 (global-prefatchCount)


2.Message acknowledgment (default로 동작)
참고) Acknowledgment(Consumer전달확인)와 Confirm(Publish전달확인)

Consumer가 죽더라도, 작업을 잃지 않는 방법 : message acknowledgement를 통해,
ack처리가 되지 않았다면(channel closed, disconnection, TCP disconnection), rabbitmq에 그 메시지를 다시 큐잉한다.

그럼 언제까지 그 메시지를 유지하고 있을까?
- 메시지 타임아웃이 없다. (계속해서 메시지를 유지하고 있다.)
- worker(consumer)의 연결이 끊겼을 때만, message를 재전달 한다. (automatic ack)
- 결론 : 일정 시간 후에 자동으로 NACK을 보내는 시간 초과가 필요하면 직접해야한다.

https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf
describes Acknowledgements
a. automatic : 클라이언트는 아무것도 할 게 없고, 메시지는 배달된 즉시 ack 된다. (전달 보장을 줄이고, 처리량을 높임)
b. explicit : 명시된 절차에 따라 메시지의 그룹 또는 각 메시지를 ack 해야한다.

3.Message persistence
RabbitMQ가 재기동 됐을때 대응책!
메시지는 기본적으로 Spring AMQP와 함께 영속적이다. 메시지가 끝날 대기열은 내구성이 있어야합니다.
그렇지 않으면 비 지속성 대기열 자체가 다시 시작될 때까지 지속되지 않으므로 브로커가 다시 시작될 때 메시지가 지속되지 않습니다.
메시지 지속성을보다 강력하게 제어하려면 MessagePostProcessor를 사용하면 된다.
MessagePostProcessor는 실제 전달하기 전에 적용되도록 하는 절차로서, 메시지 페이로드 또는 헤더를 수정하기에 좋습니다.

	 * Basic RPC pattern with conversion. Send a Java object converted to a message to a default exchange with a
	 * specific routing key and attempt to receive a response, converting that to a Java object. Implementations will
	 * normally set the reply-to header to an exclusive queue and wait up for some time limited by a timeout.

메시지를 Queue에 보관할 때 file에도 같이 쓰도록 만드는 방법이다.
아래와 같은 방법으로 설정해야 동작한다.
1queue생성시 durable속성을 true로 주고 만든다.
2message publish할때 MessageProperties.PERSISTENT_TEXT_PLAIN을 설정함
1,2번 모두 만족해야 메시지가 Queue에 남아있을 때 restart해도 날라가지 않는다.
근데, 완벽한 보장 X, 모든 메시지들을 위해 fsync를 하지 않음.(cache에 저장, disck에 정말write하지 않음)
더 강력한 보장을 위해서는 https://www.rabbitmq.com/confirms.html를 확인...



		*/
		@Bean
		public Tut2Receiver receiver1() {
			return new Tut2Receiver(1);
		}

		@Bean
		public Tut2Receiver receiver2() {
			return new Tut2Receiver(2);
		}

	}

	@Profile("sender")
	@Bean
	public Tut2Sender sender() {
		return new Tut2Sender();
	}

}

/*

 [x] Sent 'Hello.1'
 [x] Sent 'Hello..2'
 [x] Sent 'Hello...3'
 [x] Sent 'Hello.4'
 [x] Sent 'Hello..5'
 [x] Sent 'Hello...6'
 [x] Sent 'Hello.7'
 [x] Sent 'Hello..8'
 [x] Sent 'Hello...9'
 [x] Sent 'Hello.10'

테스트 0 ( rcv : 1개 프로세스 )
1회
instance 1 [x] Received 'Hello.1'
instance 1 [x] Done in 1.001s
instance 2 [x] Received 'Hello..2'
instance 1 [x] Received 'Hello...3'
instance 2 [x] Done in 2.011s
instance 2 [x] Received 'Hello.4'
instance 2 [x] Done in 1.015s
instance 1 [x] Done in 3.026s
instance 1 [x] Received 'Hello..5'
instance 2 [x] Received 'Hello...6'
instance 1 [x] Done in 2.001s
instance 1 [x] Received 'Hello.7'
instance 1 [x] Done in 1.016s
instance 2 [x] Done in 3.017s
instance 2 [x] Received 'Hello..8'
instance 1 [x] Received 'Hello...9'
instance 2 [x] Done in 2.005s
instance 2 [x] Received 'Hello.10'
instance 2 [x] Done in 1.01s
instance 1 [x] Done in 3.015s

2회
instance 1 [x] Received 'Hello.1'
instance 1 [x] Done in 1.0s
instance 2 [x] Received 'Hello..2'
instance 1 [x] Received 'Hello...3'
instance 2 [x] Done in 2.029s
instance 2 [x] Received 'Hello.4'
instance 2 [x] Done in 1.001s
instance 1 [x] Done in 3.029s
instance 1 [x] Received 'Hello..5'
instance 2 [x] Received 'Hello...6'
instance 1 [x] Done in 2.023s
instance 1 [x] Received 'Hello.7'
instance 1 [x] Done in 1.0s
instance 2 [x] Done in 3.038s
instance 2 [x] Received 'Hello..8'
instance 1 [x] Received 'Hello...9'
instance 2 [x] Done in 2.015s
instance 2 [x] Received 'Hello.10'
instance 2 [x] Done in 1.016s
instance 1 [x] Done in 3.031s

*/
