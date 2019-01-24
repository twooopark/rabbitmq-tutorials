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
package org.springframework.amqp.tutorials.tut3;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Gary Russell
 * @author Scott Deeg
 */
@Profile({"direct-T", "pub-sub", "publish-subscribe"})
@Configuration
public class Tut3Config {
	@Bean
	public DirectExchange direct() {
		return new DirectExchange("rmq.direct");
	}
	@Profile("receiver")
	private static class ReceiverConfig {
		@Bean
		public Queue directQueue1() {return new Queue("rmq.direct.queue1"); }
		@Bean
		public Queue directQueue2() {return new Queue("rmq.direct.queue2"); }
		@Bean
		public Queue directQueue3() {return new Queue("rmq.direct.queue3"); }

		@Bean
		public Binding binding1(DirectExchange direct, Queue directQueue1) {return BindingBuilder.bind(directQueue1).to(direct).with("rmq.direct"); }
		@Bean
		public Binding binding2(DirectExchange direct, Queue directQueue2) {return BindingBuilder.bind(directQueue2).to(direct).with("rmq.direct"); }
		@Bean
		public Binding binding3(DirectExchange direct, Queue directQueue3) {return BindingBuilder.bind(directQueue3).to(direct).with("rmq.direct"); }
		@Bean
		public Tut3Receiver receiver() {
			return new Tut3Receiver();
		}
	}
	@Profile("sender")
	@Bean
	public Tut3Sender sender() {
		return new Tut3Sender();
	}
}
